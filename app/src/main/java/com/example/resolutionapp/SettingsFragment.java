package com.example.resolutionapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private TextInputEditText etRecipientPhone;
    private Button btnSave;

    public static final String PREFS_NAME = "ResolutionAppPrefs";
    public static final String KEY_RECIPIENT_PHONE = "pref_recipient_phone";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etRecipientPhone = view.findViewById(R.id.et_recipient_phone);
        btnSave = view.findViewById(R.id.btn_save_settings);

        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());

        view.findViewById(R.id.btn_send_demo).setOnClickListener(v -> sendRealReportDemo());

        view.findViewById(R.id.btn_show_tour).setOnClickListener(v -> {
            SharedPreferences introPrefs = requireActivity().getSharedPreferences("PREFS", Context.MODE_PRIVATE);
            introPrefs.edit().putBoolean("INTRO_SEEN", false).apply();
            android.content.Intent intent = new android.content.Intent(requireActivity(), IntroActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String phone = prefs.getString(KEY_RECIPIENT_PHONE, "+91");
        if (phone.isEmpty())
            phone = "+91";
        etRecipientPhone.setText(phone);
    }

    private void saveSettings() {
        String recipientPhone = etRecipientPhone.getText().toString().trim();
        if (!recipientPhone.startsWith("+") && !recipientPhone.isEmpty()) {
            recipientPhone = "+91" + recipientPhone;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_RECIPIENT_PHONE, recipientPhone);
        editor.apply();
        etRecipientPhone.setText(recipientPhone); // Update UI to show prefix

        Toast.makeText(getContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    private void sendRealReportDemo() {
        String phone = etRecipientPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.resolutionapp.data.FirestoreHelper helper = new com.example.resolutionapp.data.FirestoreHelper();
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());

        Toast.makeText(getContext(), "Generating report...", Toast.LENGTH_SHORT).show();

        helper.getHabitsTask().addOnSuccessListener(habitsSnapshot -> {
            java.util.List<com.example.resolutionapp.model.Habit> habits = habitsSnapshot
                    .toObjects(com.example.resolutionapp.model.Habit.class);

            helper.getResolutionsForDateTask(today).addOnSuccessListener(resSnapshot -> {
                java.util.List<String> completedIds = new java.util.ArrayList<>();
                if (resSnapshot.exists()) {
                    com.example.resolutionapp.model.ResolutionDay day = resSnapshot
                            .toObject(com.example.resolutionapp.model.ResolutionDay.class);
                    if (day != null && day.getCompletedHabitIds() != null) {
                        completedIds = day.getCompletedHabitIds();
                    }
                }

                StringBuilder body = new StringBuilder();
                body.append("Demo Report (").append(today).append("):\n");

                for (com.example.resolutionapp.model.Habit habit : habits) {
                    // Inclusion logic same as Worker
                    boolean isScheduled = habit.getFrequency() == null || habit.getFrequency().isEmpty();
                    if (!isScheduled) {
                        String[] days = { "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY",
                                "SATURDAY" };
                        int dayIndex = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1;
                        isScheduled = habit.getFrequency().contains(days[dayIndex]);
                    }

                    if (isScheduled) {
                        boolean isComplete = completedIds.contains(habit.getId());
                        body.append(isComplete ? "✓ " : "✗ ");
                        body.append(habit.getTitle()).append("\n");
                    }
                }

                com.example.resolutionapp.util.SmsSender.sendSms(getContext(), phone, body.toString());
                Toast.makeText(getContext(), "Demo Report Sent!", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to fetch habits", Toast.LENGTH_SHORT).show());
    }
}
