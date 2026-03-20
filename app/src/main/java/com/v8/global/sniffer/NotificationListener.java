package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationListener extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // هنا تقدر ترسل الإشعارات للتليجرام
    }
    
    @Override
    public void onListenerConnected() {
        // لما يتصل الخدمة
    }
}
