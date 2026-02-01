package com.example.resolutionapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.View;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

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
            generateCheckBoxes();
            loadResolutions(); // Load status after creating boxes
        });
    }

    private void generateCheckBoxes() {
        llResolutionsContainer.removeAllViews();
        if (allHabits.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No habits found. Please add using Manage Habits.");
            llResolutionsContainer.addView(tv);
            return;
        }

        // Parse currentDate to compare with creation timestamp
        Calendar viewDateCal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(currentDate);
            if (d != null) {
                viewDateCal.setTime(d);
                // Reset to start of day
                viewDateCal.set(Calendar.HOUR_OF_DAY, 0);
                viewDateCal.set(Calendar.MINUTE, 0);
                viewDateCal.set(Calendar.SECOND, 0);
                viewDateCal.set(Calendar.MILLISECOND, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Habit habit : allHabits) {
            // 1. Check Date Restriction: Only show habits on or after their creation date
            // Handle habits with missing or invalid timestamps (0 or null)
            if (habit.createdTimestamp > 0) {
                Calendar createdCal = Calendar.getInstance();
                createdCal.setTimeInMillis(habit.createdTimestamp);
                // Normalize creation date to start of day (midnight)
                createdCal.set(Calendar.HOUR_OF_DAY, 0);
                createdCal.set(Calendar.MINUTE, 0);
                createdCal.set(Calendar.SECOND, 0);
                createdCal.set(Calendar.MILLISECOND, 0);

                // Skip if viewing a date BEFORE the habit was created
                if (viewDateCal.getTimeInMillis() < createdCal.getTimeInMillis()) {
                    // Don't show habits in dates before they were created
                    continue;
                }
            }
            // If createdTimestamp is 0 or invalid, show the habit on all dates (backward
            // compatibility)

            // 2. Filter Frequency
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
                cb.setAlpha(0.6f);
                tv.setAlpha(0.6f);
            }

            llResolutionsContainer.addView(cardView);
        }
    }

    private void updateCardStyle(View cardView, boolean isChecked) {
        TextView tv = cardView.findViewById(R.id.tvResolutionTitle);
        androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) cardView;

        if (isChecked) {
            // No green background
            card.setCardBackgroundColor(android.graphics.Color.parseColor("#2A2A35"));
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault());
            Date date = sdf.parse(dateString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            String dayName = getDayName(dayOfWeek);
            return dayName != null && habit.getFrequency().contains(dayName);

        } catch (Exception e) {
            e.printStackTrace();
            return true; // Default to show if error
        }
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "SUNDAY";
            case Calendar.MONDAY:
                return "MONDAY";
            case Calendar.TUESDAY:
                return "TUESDAY";
            case Calendar.WEDNESDAY:
                return "WEDNESDAY";
            case Calendar.THURSDAY:
                return "THURSDAY";
            case Calendar.FRIDAY:
                return "FRIDAY";
            case Calendar.SATURDAY:
                return "SATURDAY";
            default:
                return null;
        }
    }

    private void loadResolutions() {
        firestoreHelper.getResolutionsForDate(currentDate, ids -> {
            completedHabitIds = ids;
            for (int i = 0; i < llResolutionsContainer.getChildCount(); i++) {
                View v = llResolutionsContainer.getChildAt(i);
                CheckBox cb = v.findViewById(R.id.cbResolution);
                if (cb != null) {
                    String habitId = (String) cb.getTag();
                    cb.setOnCheckedChangeListener(null);
                    boolean isDone = completedHabitIds.contains(habitId);

                    cb.setChecked(isDone);
                    updateCardStyle(v, isDone);

                    if (isPast) {
                        if (!isDone) {
                            cb.setButtonDrawable(R.drawable.ic_close_red);
                        }
                    } else {
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

        // Notify StatsFragment that resolutions have been updated
        android.content.Intent intent = new android.content.Intent("RESOLUTIONS_UPDATED");
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
