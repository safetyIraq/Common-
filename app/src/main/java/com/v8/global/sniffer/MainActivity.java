package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.v8.global.sniffer.game.MainGameActivity;

public class MainActivity extends Activity implements PermissionHandler.PermissionCallback {

    private PermissionHandler permissionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تهيئة معالج الصلاحيات
        permissionHandler = new PermissionHandler(this);
        permissionHandler.setCallback(this);
        
        // طلب الصلاحيات
        permissionHandler.requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHandler.onActivityResult(requestCode);
    }

    @Override
    public void onAllPermissionsGranted() {
        // كل الصلاحيات ممنوحة - افتح اللعبة
        openGame();
    }

    @Override
    public void onPermissionDenied() {
        // بعض الصلاحيات مرفوضة - افتح اللعبة على أي حال
        openGame();
    }

    private void openGame() {
        Intent intent = new Intent(this, MainGameActivity.class);
        startActivity(intent);
        finish();
    }
}
