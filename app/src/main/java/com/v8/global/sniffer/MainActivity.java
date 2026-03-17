package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.v8.global.sniffer.game.MainGameActivity;
import com.v8.global.sniffer.utils.PermissionManager;

public class MainActivity extends Activity implements PermissionManager.PermissionCallback {

    private PermissionManager permissionManager;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تهيئة مدير الصلاحيات
        permissionManager = new PermissionManager(this);
        permissionManager.setCallback(this);
        
        // التحقق من الصلاحيات
        if (permissionManager.hasAllPermissions()) {
            // كل الصلاحيات موجودة - افتح اللعبة
            openGame();
        } else {
            // اطلب الصلاحيات
            permissionManager.requestAllPermissions();
        }
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onAllPermissionsGranted() {
        // تم منح جميع الصلاحيات
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "✅ تم تفعيل جميع الصلاحيات", Toast.LENGTH_SHORT).show();
                openGame();
            }
        });
    }

    @Override
    public void onPermissionDenied(String permission) {
        // تم رفض صلاحية معينة - نسجلها ونتابع
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "⚠️ بعض الصلاحيات معلقة", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionManager.handleSpecialPermissionResult(requestCode);
    }
}
