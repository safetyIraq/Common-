package com.v8.global.sniffer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("restartservice".equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ControlService.class);
            context.startService(serviceIntent);
        }
    }
}
