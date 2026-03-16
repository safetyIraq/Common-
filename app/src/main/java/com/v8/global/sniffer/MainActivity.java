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
        Button btn = findViewById(R.id.btn_perm);
        btn.setOnClickListener(v -> 
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        );
    }
}
