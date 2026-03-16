package com.v8.global.sniffer;

import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends android.accessibilityservice.AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // يتم برمجة صيد حركات الشاشة هنا لاحقاً
    }
    @Override public void onInterrupt() {}
}
