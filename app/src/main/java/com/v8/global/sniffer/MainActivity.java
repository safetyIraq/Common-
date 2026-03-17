package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.v8.global.sniffer.game.MainGameActivity;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // افتح اللعبة مباشرة
        startActivity(new Intent(this, MainGameActivity.class));
        finish();
    }
}
