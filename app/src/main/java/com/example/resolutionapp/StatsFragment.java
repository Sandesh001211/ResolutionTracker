package com.example.resolutionapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.resolutionapp.data.FirestoreHelper;
import com.example.resolutionapp.model.ResolutionDay;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class StatsFragment extends Fragment {

    private TextView tvStreak;
    private TextView tvHeatmapTitle;
    private GridLayout glHeatmap;
    private FirestoreHelper firestoreHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        tvStreak = view.findViewById(R.id.tvStreak);
        tvHeatmapTitle = view.findViewById(R.id.tvHeatmapTitle);
        glHeatmap = view.findViewById(R.id.glHeatmap);
        firestoreHelper = new FirestoreHelper();

        // Register broadcast receiver for resolution updates
        android.content.IntentFilter filter = new android.content.IntentFilter("RESOLUTIONS_UPDATED");
        if (getActivity() != null) {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getActivity())
                    .registerReceiver(resolutionUpdateReceiver, filter);
        }

        loadStats();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats whenever the fragment becomes visible
        // This ensures the heatmap updates when you complete resolutions and switch
        // tabs
        loadStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister broadcast receiver to prevent memory leaks
        if (getActivity() != null) {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getActivity())
                    .unregisterReceiver(resolutionUpdateReceiver);
        }
    }

    // BroadcastReceiver to listen for resolution updates
    private final android.content.BroadcastReceiver resolutionUpdateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            // Refresh stats when resolutions are updated
            loadStats();
        }
    };

    private void loadStats() {
        // Calculate start date (1 year ago)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(cal.getTime());

        // Use Tasks API or simple barrier for parallel execution
        // Since helper uses callbacks, we'll use a simple counter approach or nested
        // optimized for speed
        // To keep it clean without changing Helper signature to Task<T>, we'll launch
        // both.

        final java.util.concurrent.atomic.AtomicReference<List<com.example.resolutionapp.model.Habit>> habitsRef = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<List<ResolutionDay>> resolutionsRef = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

        Runnable checkDone = () -> {
            if (counter.incrementAndGet() == 2) {
                // Both done
                if (getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    int totalHabits = habitsRef.get() != null ? habitsRef.get().size() : 0;
                    List<ResolutionDay> days = resolutionsRef.get() != null ? resolutionsRef.get()
                            : new java.util.ArrayList<>();

                    Map<String, ResolutionDay> map = new HashMap<>();
                    for (ResolutionDay d : days)
                        map.put(d.getDate(), d);

                    calculateStreak(map);
                    populateHeatmap(map, habitsRef.get());
                });
            }
        };

        // 1. Fetch Habits
        firestoreHelper.getHabits(habits -> {
            habitsRef.set(habits);
            checkDone.run();
        });

        // 2. Fetch Resolutions (Filtered)
        firestoreHelper.getResolutionsSince(startDate, days -> {
            resolutionsRef.set(days);
            checkDone.run();
        });
    }

    private void calculateStreak(Map<String, ResolutionDay> map) {
        int streak = 0;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 365; i++) {
            String dateKey = sdf.format(cal.getTime());
            ResolutionDay day = map.get(dateKey);
            if (day != null && isDaySuccessful(day)) {
                streak++;
            } else if (i == 0) {
                if (map.containsKey(dateKey)) {
                    break;
                }
            } else {
                break;
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        tvStreak.setText(streak + " Days");
    }

    private void populateHeatmap(Map<String, ResolutionDay> map,
            List<com.example.resolutionapp.model.Habit> allHabits) {
        glHeatmap.removeAllViews();

        Calendar cal = Calendar.getInstance();
        // Set to 1st day of current month
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Update Title
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (tvHeatmapTitle != null) {
            tvHeatmapTitle.setText(monthFormat.format(cal.getTime()));
        }

        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Sun=1 ... Sat=7

        int size = (int) (getResources().getDisplayMetrics().density * 40);
        int margin = (int) (getResources().getDisplayMetrics().density * 2);

        // 1. Header Row
        String[] days = { "S", "M", "T", "W", "T", "F", "S" };
        for (String d : days) {
            TextView header = new TextView(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size / 2;
            params.setMargins(margin, margin, margin, margin);
            header.setLayoutParams(params);
            header.setText(d);
            header.setGravity(android.view.Gravity.CENTER);
            header.setTextColor(Color.WHITE);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            glHeatmap.addView(header);
        }

        // 2. Spacers
        for (int i = 1; i < startDayOfWeek; i++) {
            android.widget.Space space = new android.widget.Space(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            space.setLayoutParams(params);
            glHeatmap.addView(space);
        }

        // 3. Days
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 1; i <= maxDays; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            String dateKey = sdf.format(cal.getTime());
            ResolutionDay day = map.get(dateKey);

            TextView box = new TextView(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            box.setLayoutParams(params);

            box.setText(String.valueOf(i));
            box.setGravity(android.view.Gravity.CENTER);
            box.setTextSize(12);
            box.setTextColor(Color.BLACK);

            int count = (day != null) ? getCompletedCount(day.getCompletedHabitIds()) : 0;
            int totalExpected = getScheduledCountForDay(allHabits, (Calendar) cal.clone());

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar cellCal = (Calendar) cal.clone();
            cellCal.set(Calendar.HOUR_OF_DAY, 0);
            cellCal.set(Calendar.MINUTE, 0);
            cellCal.set(Calendar.SECOND, 0);
            cellCal.set(Calendar.MILLISECOND, 0);

            if (cellCal.before(today)) {
                // Past dates
                if (totalExpected > 0) {
                    double completionRate = (double) count / totalExpected;

                    if (count >= totalExpected) {
                        // 100% - All completed
                        box.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                        box.setTextColor(Color.WHITE);
                    } else if (completionRate >= 0.5) {
                        // 50% or more - Half or more completed
                        box.setBackgroundColor(Color.parseColor("#FFC107")); // Yellow
                        box.setTextColor(Color.BLACK);
                    } else if (count > 0) {
                        // > 0% but < 50% - Need improvement
                        box.setBackgroundColor(Color.parseColor("#9E9E9E")); // Grey
                        box.setTextColor(Color.BLACK);
                    } else {
                        // 0% - None completed
                        box.setBackgroundColor(Color.parseColor("#F44336")); // Red
                        box.setTextColor(Color.WHITE);
                    }
                } else {
                    box.setBackgroundColor(Color.parseColor("#424242")); // Grey (Nothing scheduled)
                    box.setTextColor(Color.WHITE);
                }
            } else if (cellCal.equals(today)) {
                // Today - show completion status with a border to indicate it's today
                int bgColor;
                int textColor;

                if (totalExpected > 0) {
                    double completionRate = (double) count / totalExpected;

                    if (count >= totalExpected) {
                        // 100% - All completed
                        bgColor = Color.parseColor("#4CAF50"); // Green - All done!
                        textColor = Color.WHITE;
                    } else if (completionRate >= 0.5) {
                        // 50% or more - Half or more completed
                        bgColor = Color.parseColor("#FFC107"); // Yellow - Partial (≥50%)
                        textColor = Color.BLACK;
                    } else if (count > 0) {
                        // > 0% but < 50% - Need improvement
                        bgColor = Color.parseColor("#9E9E9E"); // Grey
                        textColor = Color.BLACK;
                    } else {
                        // 0% - None completed
                        bgColor = Color.parseColor("#F44336"); // Red
                        textColor = Color.WHITE;
                    }
                } else {
                    bgColor = Color.parseColor("#424242"); // Grey (Nothing scheduled)
                    textColor = Color.WHITE;
                }

                // Create border drawable for today
                box.setPadding(4, 4, 4, 4);
                android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
                border.setColor(bgColor);
                border.setStroke(4, Color.parseColor("#FFA500")); // Orange border for today
                border.setCornerRadius(8);
                box.setBackground(border);
                box.setTextColor(textColor);
            } else {
                // Future dates
                box.setBackgroundColor(Color.parseColor("#2C2C2C")); // Dark Grey (Future)
                box.setTextColor(Color.GRAY);
            }

            glHeatmap.addView(box);
            // Move loop calc to end to avoid double-increment if we used cal.add inside
            // logic?
            // original code had cal.add(DAY_OF_YEAR, 1) inside loop.
        }

        tvStreak.setTextColor(Color.parseColor("#FFA500"));
    }

    private int getScheduledCountForDay(List<com.example.resolutionapp.model.Habit> habits, Calendar cal) {
        if (habits == null)
            return 0;
        int count = 0;
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayName(dayOfWeek);

        // Normalize cal to check (start of day)
        Calendar checkCal = (Calendar) cal.clone();
        checkCal.set(Calendar.HOUR_OF_DAY, 0);
        checkCal.set(Calendar.MINUTE, 0);
        checkCal.set(Calendar.SECOND, 0);
        checkCal.set(Calendar.MILLISECOND, 0);

        for (com.example.resolutionapp.model.Habit h : habits) {
            // Check Creation Date
            Calendar createdCal = Calendar.getInstance();
            createdCal.setTimeInMillis(h.getCreatedTimestamp());
            createdCal.set(Calendar.HOUR_OF_DAY, 0);
            createdCal.set(Calendar.MINUTE, 0);
            createdCal.set(Calendar.SECOND, 0);
            createdCal.set(Calendar.MILLISECOND, 0);

            // If the day we are checking is BEFORE the creation day, skip this habit.
            if (checkCal.before(createdCal)) {
                continue;
            }

            if (h.getFrequency() == null || h.getFrequency().isEmpty()) {
                count++;
            } else if (dayName != null && h.getFrequency().contains(dayName)) {
                count++;
            }
        }
        return count;
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

    private boolean isDaySuccessful(ResolutionDay day) {
        return day.getCompletedHabitIds() != null && !day.getCompletedHabitIds().isEmpty();
    }

    private int getCompletedCount(List<String> identifiers) {
        if (identifiers == null)
            return 0;
        return identifiers.size();
    }
}
