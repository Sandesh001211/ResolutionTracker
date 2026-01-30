package com.example.resolutionapp.worker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.resolutionapp.data.FirestoreHelper;
import com.example.resolutionapp.model.Habit;
import com.example.resolutionapp.model.ResolutionDay;
import com.example.resolutionapp.util.NotificationHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";
    private final FirestoreHelper firestoreHelper;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        firestoreHelper = new FirestoreHelper();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background check for resolutions...");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        try {
            // 1. Get Habits (Synchronously)
            Task<QuerySnapshot> habitsTask = firestoreHelper.getHabitsTask();
            if (habitsTask == null) {
                // User not logged in?
                Log.d(TAG, "No user logged in, skipping.");
                return Result.success();
            }
            QuerySnapshot habitsSnapshot = Tasks.await(habitsTask);
            List<Habit> habits = habitsSnapshot.toObjects(Habit.class);

            if (habits.isEmpty()) {
                Log.d(TAG, "No habits found to check.");
                return Result.success();
            }

            // 2. Get Today's Resolutions (Synchronously)
            Task<DocumentSnapshot> resolutionsTask = firestoreHelper.getResolutionsForDateTask(today);
            DocumentSnapshot resolutionSnapshot = Tasks.await(resolutionsTask);

            int completedCount = 0;
            if (resolutionSnapshot.exists()) {
                ResolutionDay day = resolutionSnapshot.toObject(ResolutionDay.class);
                if (day != null && day.getCompletedHabitIds() != null) {
                    completedCount = day.getCompletedHabitIds().size();
                }
            }

            // 3. Compare (Filter habits first)
            int totalHabits = 0;
            for (Habit h : habits) {
                if (isHabitScheduledForToday(h))
                    totalHabits++;
            }

            int remaining = totalHabits - completedCount;
            // Prevent negative if data is inconsistent
            if (remaining < 0)
                remaining = 0;

            Log.d(TAG, "Total: " + totalHabits + ", Completed: " + completedCount + ", Remaining: " + remaining);

            if (remaining > 0) {
                // Send Notification
                String message = "You have " + remaining + " resolutions remaining for today. Finish them now!";
                NotificationHelper.showNotification(getApplicationContext(), "Keep going!", message);
            }

            // --- Accountability SMS Report ---
            sendAccountabilityReport(habits, resolutionSnapshot, today);

            return Result.success();

        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error fetching data in worker", e);
            return Result.retry();
        }
    }

    private void sendAccountabilityReport(List<Habit> habits, DocumentSnapshot resolutionSnapshot, String date) {
        android.content.SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                com.example.resolutionapp.SettingsFragment.PREFS_NAME, android.content.Context.MODE_PRIVATE);

        String recipientPhone = prefs.getString(com.example.resolutionapp.SettingsFragment.KEY_RECIPIENT_PHONE, "");

        if (recipientPhone.isEmpty()) {
            Log.d(TAG, "Recipient phone number not set in Settings. Skipping SMS.");
            return;
        }

        List<String> completedIds = new java.util.ArrayList<>();
        if (resolutionSnapshot.exists()) {
            ResolutionDay day = resolutionSnapshot.toObject(ResolutionDay.class);
            if (day != null && day.getCompletedHabitIds() != null) {
                completedIds = day.getCompletedHabitIds();
            }
        }

        StringBuilder body = new StringBuilder();
        body.append("Daily Report (").append(date).append("):\n");

        for (Habit habit : habits) {
            if (!isHabitScheduledForToday(habit))
                continue;

            boolean isComplete = completedIds.contains(habit.getId());
            body.append(isComplete ? "✓ " : "✗ ");
            body.append(habit.getTitle()).append("\n");
        }

        try {
            com.example.resolutionapp.util.SmsSender.sendSms(getApplicationContext(), recipientPhone, body.toString());
            Log.d(TAG, "Accountability SMS sent successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS", e);
        }
    }

    private boolean isHabitScheduledForToday(Habit habit) {
        if (habit.getFrequency() == null || habit.getFrequency().isEmpty())
            return true; // Daily habit

        String[] days = { "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY" };
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayIndex = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1; // 0 for Sunday
        String todayString = days[dayIndex];

        return habit.getFrequency().contains(todayString);
    }
}
