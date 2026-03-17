package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.v8.global.sniffer.game.MainGameActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تجاوز كل شيء وافتح اللعبة مباشرة بعد ثانية واحدة
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                openGame();
            }
        }, 1000);
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
