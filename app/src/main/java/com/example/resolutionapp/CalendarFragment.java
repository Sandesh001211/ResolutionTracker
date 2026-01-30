package com.example.resolutionapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CalendarFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        android.widget.CalendarView calendarView = view.findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // Month is 0-indexed
            String selectedDate = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, (month + 1),
                    dayOfMonth);

            java.util.Calendar today = java.util.Calendar.getInstance();
            java.util.Calendar selected = java.util.Calendar.getInstance();
            selected.set(year, month, dayOfMonth);

            // normalize to start of day
            today.set(java.util.Calendar.HOUR_OF_DAY, 0);
            today.set(java.util.Calendar.MINUTE, 0);
            today.set(java.util.Calendar.SECOND, 0);
            today.set(java.util.Calendar.MILLISECOND, 0); // Important for comparison

            selected.set(java.util.Calendar.HOUR_OF_DAY, 0);
            selected.set(java.util.Calendar.MINUTE, 0);
            selected.set(java.util.Calendar.SECOND, 0);
            selected.set(java.util.Calendar.MILLISECOND, 0);

            if (selected.after(today)) {
                android.widget.Toast
                        .makeText(getContext(), "Future selection not allowed!", android.widget.Toast.LENGTH_SHORT)
                        .show();
            } else {
                android.content.Intent intent = new android.content.Intent(getContext(),
                        DailyResolutionsActivity.class);
                intent.putExtra("DATE", selectedDate);
                intent.putExtra("IS_PAST", selected.before(today));
                startActivity(intent);
            }
        });

        return view;
    }
}
