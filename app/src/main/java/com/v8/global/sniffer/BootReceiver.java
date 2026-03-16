package com.v8.global.sniffer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // هنا نضمن إن الخدمة تشتغل أول ما يفتح الجهاز
            context.startService(new Intent(context, NotificationService.class));
        }
    }
}
