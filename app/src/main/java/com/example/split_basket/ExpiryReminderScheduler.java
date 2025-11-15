package com.example.split_basket;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ExpiryReminderScheduler {

    public static void scheduleReminder(Context ctx, String itemId, String itemName, long triggerAtMillis) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, ReminderReceiver.class)
                .putExtra("itemId", itemId)
                .putExtra("itemName", itemName);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    public static void cancelReminder(Context ctx, String itemId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                itemId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (am != null) {
            am.cancel(pi);
        }
    }
}