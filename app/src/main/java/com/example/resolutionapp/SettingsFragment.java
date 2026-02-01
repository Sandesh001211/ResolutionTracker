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
        String phone = prefs.getString(KEY_RECIPIENT_PHONE, "");
        etRecipientPhone.setText(phone);
    }

    private void saveSettings() {
        String recipientPhone = etRecipientPhone.getText().toString().trim();

        // Remove spaces/hyphens
        recipientPhone = recipientPhone.replace(" ", "").replace("-", "");

        if (recipientPhone.isEmpty()) {
            etRecipientPhone.setError("Phone number cannot be empty");
            return;
        }

        // Check for minimum 10 digits
        String digitsOnly = recipientPhone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10) {
            etRecipientPhone.setError("Phone number cannot be less than 10 digits");
            return;
        }

        // If user entered 10 digits, assume India and add +91
        if (recipientPhone.matches("\\d{10}")) {
            recipientPhone = "+91" + recipientPhone;
        }

        // Basic validation: Must start with + and have at least 10 digits
        if (!recipientPhone.startsWith("+")) {
            etRecipientPhone.setError("Invalid format. Enter 10 digit number.");
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_RECIPIENT_PHONE, recipientPhone);
        editor.apply();

        etRecipientPhone.setError(null);
        etRecipientPhone.setText(recipientPhone); // Update UI to show prefix

        Toast.makeText(getContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }
}
