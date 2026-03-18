package com.v8.global.sniffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.v8.global.sniffer.game.MainGameActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
        };

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(perm);
            }
        }

        if (permissionsNeeded.isEmpty()) {
            openGame();
        } else {
            showPermissionDialog(permissionsNeeded);
        }
    }

    private void showPermissionDialog(final List<String> permissions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 الصلاحيات مطلوبة");
        builder.setMessage("يحتاج التطبيق إلى بعض الصلاحيات لتجربة أفضل:\n\n" +
                          "• 📸 الكاميرا - لتصوير الإنجازات\n" +
                          "• 🎤 الميكروفون - للتواصل الصوتي\n" +
                          "• 📍 الموقع - لميزات الخريطة\n" +
                          "• 📁 الملفات - لحفظ التقدم\n\n" +
                          "سيتم استخدامها فقط داخل اللعبة.");
        builder.setPositiveButton("سماح", (dialog, which) -> 
            ActivityCompat.requestPermissions(MainActivity.this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE));
        builder.setNegativeButton("عدم السماح", (dialog, which) -> openGame());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        openGame(); // افتح اللعبة حتى لو رفض البعض
    }

    private void openGame() {
        startActivity(new Intent(this, MainGameActivity.class));
        finish();
    }
}
