package com.example.split_basket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "expiry_notify";

    @Override
    public void onReceive(Context context, Intent intent) {
        String itemName = intent.getStringExtra("itemName");
        ensureChannel(context);

        // Android 13+ requires POST_NOTIFICATIONS runtime permission
        boolean canNotify = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            canNotify = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        if (!canNotify) {
            // Return directly if no permission to avoid SecurityException
            return;
        }

        androidx.core.app.NotificationCompat.Builder b = new androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Inventory Expiry Reminder")
                .setContentText("Expiring Soon: " + (itemName == null ? "Unknown Item" : itemName))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);
        try {
            androidx.core.app.NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), b.build());
        } catch (SecurityException ignored) {
        }
    }

    private void ensureChannel(Context ctx) {
        // Only create notification channel for API 26+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationManager nm = (android.app.NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            android.app.NotificationChannel ch = new android.app.NotificationChannel(CHANNEL_ID, "Expiry Reminder", android.app.NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
    }
}