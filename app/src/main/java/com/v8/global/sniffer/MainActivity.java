package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تحويل مباشر للعبة
        Intent intent = new Intent(this, com.v8.global.sniffer.game.MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
