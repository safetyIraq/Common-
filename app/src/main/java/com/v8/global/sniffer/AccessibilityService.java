package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // معالجة أحداث الوصول
    }
    
    @Override
    public void onInterrupt() {
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
