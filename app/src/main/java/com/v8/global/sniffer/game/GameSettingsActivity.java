package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import com.v8.global.sniffer.R;

public class GameSettingsActivity extends Activity {

    private TextView tvAppInfo;
    private Button btnResetScore, btnOpenSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);

        // تهيئة العناصر
        initViews();
        
        // عرض معلومات التطبيق
        displayAppInfo();
        
        // إعداد أزرار التحكم
        setupClickListeners();
    }

    private void initViews() {
        tvAppInfo = findViewById(R.id.tv_app_info);
        btnResetScore = findViewById(R.id.btn_reset_score);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
    }

    private void displayAppInfo() {
        String appInfo = "🧠 Memory Challenge v1.0\n\n" +
                        "📱 الجهاز: " + android.os.Build.MODEL + "\n" +
                        "🤖 Android: " + android.os.Build.VERSION.RELEASE + "\n" +
                        "🎮 أفضل لعبة ذاكرة";
        
        tvAppInfo.setText(appInfo);
    }

    private void setupClickListeners() {
        // زر إعادة تعيين النتيجة
        btnResetScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // تأثير النقر
                v.startAnimation(AnimationUtils.loadAnimation(GameSettingsActivity.this, R.anim.click_effect));
                
                // إعادة تعيين أفضل نتيجة
                resetHighScore();
            }
        });

        // زر فتح إعدادات النظام
        btnOpenSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(GameSettingsActivity.this, R.anim.click_effect));
                
                // فتح إعدادات النظام
                openSystemSettings();
            }
        });
    }

    private void resetHighScore() {
        SharedPreferences prefs = getSharedPreferences("game_prefs", MODE_PRIVATE);
        int currentHighScore = prefs.getInt("high_score", 0);
        
        if (currentHighScore > 0) {
            prefs.edit().putInt("high_score", 0).apply();
            
            // عرض رسالة تأكيد
            android.widget.Toast.makeText(this, 
                "✅ تم إعادة تعيين أفضل نتيجة", 
                android.widget.Toast.LENGTH_SHORT).show();
        } else {
            android.widget.Toast.makeText(this, 
                "📊 لا توجد نتائج محفوظة", 
                android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void openSystemSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // يمكن إضافة أي تحديثات عند العودة للإعدادات
    }
}
