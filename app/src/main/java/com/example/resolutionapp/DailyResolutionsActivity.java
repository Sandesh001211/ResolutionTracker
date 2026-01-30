package com.example.resolutionapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.View;
import java.util.List;
import java.util.ArrayList;
import com.example.resolutionapp.data.FirestoreHelper;
import com.example.resolutionapp.model.Habit;

public class DailyResolutionsActivity extends AppCompatActivity {

    private FirestoreHelper firestoreHelper;
    private String currentDate;
    private boolean isPast;
    private LinearLayout llResolutionsContainer;
    private List<Habit> allHabits = new ArrayList<>();
    private List<String> completedHabitIds = new ArrayList<>();
    private BokehView bokehView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_resolutions);

        firestoreHelper = new FirestoreHelper();
        currentDate = getIntent().getStringExtra("DATE");
        isPast = getIntent().getBooleanExtra("IS_PAST", false);
        llResolutionsContainer = findViewById(R.id.llResolutionsContainer);
        bokehView = findViewById(R.id.bokehView);

        TextView tvDate = findViewById(R.id.tvDateTitle);
        tvDate.setText("Resolutions for " + currentDate);

        loadHabits();
    }

    private void loadHabits() {
        firestoreHelper.getHabits(habits -> {
            allHabits = new ArrayList<>(habits);
            injectDefaultHabits();
            generateCheckBoxes();
            loadResolutions(); // Load status after creating boxes
        });
    }

    private void injectDefaultHabits() {
        boolean exists = false;
        for (Habit h : allHabits) {
            if ("default_weekly_cleaning".equals(h.getId())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            Habit cleaning = new Habit();
            cleaning.setId("default_weekly_cleaning");
            cleaning.setTitle("Weekly Cleaning");
            cleaning.setDescription("Time to clean your space!");
            List<String> freq = new ArrayList<>();
            freq.add("SUNDAY");
            cleaning.setFrequency(freq);
            allHabits.add(cleaning);
        }
    }

    private void generateCheckBoxes() {
        llResolutionsContainer.removeAllViews();
        if (allHabits.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No habits found. Please add using Manage Habits.");
            llResolutionsContainer.addView(tv);
            return;
        }

        for (Habit habit : allHabits) {
            // Filter logic
            if (habit.getFrequency() != null && !habit.getFrequency().isEmpty()) {
                if (!isHabitScheduledForDate(habit, currentDate)) {
                    continue; // Skip this habit today
                }
            }

            // Inflate Card
            View cardView = getLayoutInflater().inflate(R.layout.item_resolution_card, llResolutionsContainer, false);
            CheckBox cb = cardView.findViewById(R.id.cbResolution);
            TextView tv = cardView.findViewById(R.id.tvResolutionTitle);

            tv.setText(habit.getTitle());
            cb.setTag(habit.getId());
            cb.setEnabled(!isPast);

            // Allow clicking the entire card to toggle checkbox
            if (!isPast) {
                cardView.setOnClickListener(v -> cb.performClick());

                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked)
                        playSuccessSound();
                    updateCardStyle(cardView, isChecked);
                    saveResolutions();
                    checkAllResolutionsCompleted();
                });
            } else {
                cb.setAlpha(0.5f);
                tv.setAlpha(0.5f);
            }

            llResolutionsContainer.addView(cardView);
        }
    }

    private void updateCardStyle(View cardView, boolean isChecked) {
        TextView tv = cardView.findViewById(R.id.tvResolutionTitle);
        androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) cardView;

        if (isChecked) {
            card.setCardBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // Green
            tv.setTextColor(android.graphics.Color.WHITE);
        } else {
            card.setCardBackgroundColor(android.graphics.Color.parseColor("#2A2A35")); // Default Dark
            tv.setTextColor(android.graphics.Color.WHITE);
        }
    }

    private boolean isHabitScheduledForDate(Habit habit, String dateString) {
        if (habit.getFrequency() == null || habit.getFrequency().isEmpty())
            return true; // Daily

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd",
                    java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(dateString);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);

            String dayName = getDayName(dayOfWeek);
            return dayName != null && habit.getFrequency().contains(dayName);

        } catch (Exception e) {
            e.printStackTrace();
            return true; // Default to show if error
        }
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case java.util.Calendar.SUNDAY:
                return "SUNDAY";
            case java.util.Calendar.MONDAY:
                return "MONDAY";
            case java.util.Calendar.TUESDAY:
                return "TUESDAY";
            case java.util.Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case java.util.Calendar.THURSDAY:
                return "THURSDAY";
            case java.util.Calendar.FRIDAY:
                return "FRIDAY";
            case java.util.Calendar.SATURDAY:
                return "SATURDAY";
            default:
                return null;
        }
    }

    private void loadResolutions() {
        firestoreHelper.getResolutionsForDate(currentDate, ids -> {
            completedHabitIds = ids;
            // Iterate through child views (Cards)
            for (int i = 0; i < llResolutionsContainer.getChildCount(); i++) {
                View v = llResolutionsContainer.getChildAt(i);
                // It is now a CardView potentially
                CheckBox cb = v.findViewById(R.id.cbResolution);
                if (cb != null) {
                    String habitId = (String) cb.getTag();
                    cb.setOnCheckedChangeListener(null); // Prevent trigger
                    boolean isDone = completedHabitIds.contains(habitId);
                    cb.setChecked(isDone);
                    updateCardStyle(v, isDone);

                    if (!isPast) {
                        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked)
                                playSuccessSound();
                            updateCardStyle(v, isChecked);
                            saveResolutions();
                            checkAllResolutionsCompleted();
                        });
                    }
                }
            }
            checkAllResolutionsCompleted();
        });
    }

    private void saveResolutions() {
        if (isPast)
            return;

        List<String> currentCompletedIds = new ArrayList<>();

        for (int i = 0; i < llResolutionsContainer.getChildCount(); i++) {
            View v = llResolutionsContainer.getChildAt(i);
            CheckBox cb = v.findViewById(R.id.cbResolution);
            if (cb != null && cb.isChecked()) {
                currentCompletedIds.add((String) cb.getTag());
            }
        }

        firestoreHelper.saveResolutions(currentDate, currentCompletedIds);
    }

    private void playSuccessSound() {
        try {
            android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, R.raw.success);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAllResolutionsCompleted() {
        if (isPast)
            return;

        int total = 0;
        int completed = 0;

        for (int i = 0; i < llResolutionsContainer.getChildCount(); i++) {
            View v = llResolutionsContainer.getChildAt(i);
            CheckBox cb = v.findViewById(R.id.cbResolution);
            if (cb != null) {
                total++;
                if (cb.isChecked()) {
                    completed++;
                }
            }
        }

        if (total > 0 && total == completed) {
            bokehView.startAnimation();
            bokehView.postDelayed(() -> bokehView.stopAnimation(), 5000);
        } else {
            bokehView.stopAnimation();
        }
    }
}
