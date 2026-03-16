package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

public class MainActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // طلب الأذونات الضرورية
        requestPermissions();
        
        // إنهاء النشاط بعد ثانية
        new Handler().postDelayed(() -> {
            moveTaskToBack(true);
            finish();
        }, 1000);
    }
    
    private void requestPermissions() {
        // فتح إعدادات الأذونات
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}
