package com.v8.global.sniffer;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPerm = findViewById(R.id.btn_perm);
        btnPerm.setOnClickListener(v -> {
            // فتح قائمة صلاحيات "الوصول للإشعارات" يدوياً
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        });
    }
}
