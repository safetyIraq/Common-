package com.v8.global.sniffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // التحقق من الصلاحيات
        checkPermissions();
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // قائمة الصلاحيات المطلوبة
        String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS
        };

        // جمع الصلاحيات التي لم تُمنح بعد
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (permissionsNeeded.isEmpty()) {
            // كل الصلاحيات ممنوحة - افتح اللعبة
            openGame();
        } else {
            // عرض رسالة توضح سبب حاجة الصلاحيات
            showPermissionDialog(permissionsNeeded);
        }
    }

    private void showPermissionDialog(final List<String> permissions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 الصلاحيات مطلوبة");
        builder.setMessage("يحتاج التطبيق إلى بعض الصلاحيات لتجربة لعب أفضل:\n\n" +
                          "• 📸 الكاميرا - لتصوير ومشاركة الإنجازات\n" +
                          "• 🎤 الميكروفون - للتواصل الصوتي\n" +
                          "• 📍 الموقع - لميزات الخريطة\n" +
                          "• 📁 الملفات - لحفظ تقدم اللعبة\n\n" +
                          "سيتم استخدام هذه الصلاحيات فقط داخل اللعبة.");
        
        builder.setPositiveButton("سماح", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // طلب الصلاحيات
                ActivityCompat.requestPermissions(MainActivity.this, 
                    permissions.toArray(new String[0]), 
                    PERMISSION_REQUEST_CODE);
            }
        });
        
        builder.setNegativeButton("عدم السماح", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // فتح اللعبة حتى بدون صلاحيات
                openGame();
            }
        });
        
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // بعد إجابة المستخدم، افتح اللعبة
            openGame();
        }
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
