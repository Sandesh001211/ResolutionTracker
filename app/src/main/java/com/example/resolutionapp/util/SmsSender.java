package com.example.resolutionapp.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

public class SmsSender {
    private static final String TAG = "SmsSender";
    private static final String SENT = "SMS_SENT";

    public static void sendSms(Context context, String phoneNumber, String message) {
        // 1. Sanitize Phone Number: Keep only digits and '+'
        String sanitizedNumber = phoneNumber.replaceAll("[^\\d+]", "");

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted");
            return;
        }

        if (sanitizedNumber.isEmpty()) {
            Log.e(TAG, "Phone number is empty after sanitization");
            return;
        }

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, this is the preferred way
                smsManager = context.getSystemService(SmsManager.class);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                // For older but still semi-modern versions, handles Dual SIM better
                int subId = SmsManager.getDefaultSmsSubscriptionId();
                smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
            } else {
                smsManager = SmsManager.getDefault();
            }

            if (smsManager == null) {
                smsManager = SmsManager.getDefault(); // Absolute fallback
            }

            if (smsManager != null) {
                // If message is long, it MUST be split
                java.util.ArrayList<String> parts = smsManager.divideMessage(message);

                if (parts.size() > 1) {
                    smsManager.sendMultipartTextMessage(sanitizedNumber, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage(sanitizedNumber, null, message, null, null);
                }
                Log.d(TAG, "SMS request sent to system.");
            } else {
                Log.e(TAG, "FAILED: SmsManager is still null.");
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR: " + e.getMessage());
        }
    }
}
