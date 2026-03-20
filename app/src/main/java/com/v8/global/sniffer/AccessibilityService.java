package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // هنا تقدر تراقب الشاشة
    }
    
    @Override
    public void onInterrupt() {}
}
