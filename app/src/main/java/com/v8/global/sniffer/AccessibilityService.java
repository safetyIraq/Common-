package com.v8.global.sniffer;

import android.view.accessibility.AccessibilityEvent;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // سيتم برمجة الصيد هنا
    }

    @Override
    public void onInterrupt() {
    }
}
