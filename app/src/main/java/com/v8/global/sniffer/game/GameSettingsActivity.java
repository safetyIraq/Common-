package com.v8.global.sniffer.game;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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

        tvAppInfo = findViewById(R.id.tv_app_info);
        btnResetScore = findViewById(R.id.btn_reset_score);
        btnOpenSettings = findViewById(R.id.btn_open_settings);

        tvAppInfo.setText("Memory Challenge v1.0\n"
                + "الجهاز: " + android.os.Build.MODEL + "\n"
                + "Android: " + android.os.Build.VERSION.RELEASE);

        btnResetScore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("game_prefs", MODE_PRIVATE)
                        .edit().putInt("high_score", 0).apply();
                v.startAnimation(AnimationUtils.loadAnimation(GameSettingsActivity.this, R.anim.click_effect));
            }
        });

        btnOpenSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
    }
}
