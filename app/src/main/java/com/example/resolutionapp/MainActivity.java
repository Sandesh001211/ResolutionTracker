package com.example.resolutionapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import com.example.resolutionapp.util.NotificationHelper;
import com.example.resolutionapp.worker.NotificationWorker;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Fragment settingsFragment;
    private Fragment calendarFragment;
    private Fragment statsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Create Channel
        NotificationHelper.createNotificationChannel(this);

        // 2. Schedule Daily Work And Reminders
        scheduleDailyNotification();
        schedulePeriodicReminder();

        // 3. Request Permissions
        android.util.Pair<String[], Integer> perms = getRequiredPermissions();
        if (perms.first.length > 0) {
            ActivityCompat.requestPermissions(this, perms.first, perms.second);
        }

        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fab_manage);
        fab.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(MainActivity.this,
                    ManageResolutionsActivity.class);
            startActivity(intent);
        });

        // Show by default if Calendar is the first menu item
        fab.show();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_settings) {
                fab.hide();
                showFragment(settingsFragment);
                return true;
            } else if (itemId == R.id.nav_calendar) {
                fab.show();
                showFragment(calendarFragment);
                return true;
            } else if (itemId == R.id.nav_stats) {
                fab.hide();
                showFragment(statsFragment);
                return true;
            }
            return false;
        });

        // Initialize fragments
        if (savedInstanceState == null) {
            settingsFragment = new SettingsFragment();
            calendarFragment = new CalendarFragment();
            statsFragment = new StatsFragment();

            // Add all, hide others, show default (Calendar)
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, "SETTINGS").hide(settingsFragment)
                    .add(R.id.fragment_container, calendarFragment, "CALENDAR").hide(calendarFragment)
                    .add(R.id.fragment_container, statsFragment, "STATS").hide(statsFragment)
                    .show(calendarFragment)
                    .commit();

            activeFragment = calendarFragment;
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        } else {
            // Restore references properly
            settingsFragment = getSupportFragmentManager().findFragmentByTag("SETTINGS");
            calendarFragment = getSupportFragmentManager().findFragmentByTag("CALENDAR");
            statsFragment = getSupportFragmentManager().findFragmentByTag("STATS");

            // Handle potential nulls to prevent crashes
            if (settingsFragment == null)
                settingsFragment = new SettingsFragment();
            if (calendarFragment == null)
                calendarFragment = new CalendarFragment();
            if (statsFragment == null)
                statsFragment = new StatsFragment();

            // Restore activeFragment based on hidden state
            if (!settingsFragment.isHidden())
                activeFragment = settingsFragment;
            else if (!calendarFragment.isHidden())
                activeFragment = calendarFragment;
            else if (!statsFragment.isHidden())
                activeFragment = statsFragment;
            else
                activeFragment = calendarFragment; // Fallback
        }

        // Handle Back Press
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });
    }

    private void showFragment(Fragment fragment) {
        if (fragment == activeFragment)
            return;

        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
    }

    private void showExitDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void scheduleDailyNotification() {
        // Schedule for 23:59 (11:59 PM)
        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        dueDate.set(Calendar.HOUR_OF_DAY, 23);
        dueDate.set(Calendar.MINUTE, 59);
        dueDate.set(Calendar.SECOND, 0);

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24);
        }

        long timeDiff = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

        PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(NotificationWorker.class, 24,
                TimeUnit.HOURS)
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .addTag("daily_resolution_check")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_resolution_check",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest);
    }

    private void schedulePeriodicReminder() {
        // Schedule every 3 hours
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                com.example.resolutionapp.worker.ReminderWorker.class, 3, TimeUnit.HOURS)
                .addTag("periodic_reminder_check")
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "periodic_reminder_check",
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest);
    }

    private android.util.Pair<String[], Integer> getRequiredPermissions() {
        java.util.List<String> permissions = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        return new android.util.Pair<>(permissions.toArray(new String[0]), 101);
    }
}
