package com.example.resolutionapp.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.resolutionapp.util.NotificationHelper;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationHelper.showNotification(
                getApplicationContext(),
                "Reminder",
                "Hey take a look 👀");
        return Result.success();
    }
}
