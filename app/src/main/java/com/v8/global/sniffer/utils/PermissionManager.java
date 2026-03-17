package com.v8.global.sniffer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionManager {

    private static final String PREF_NAME = "permissions_pref";
    private static final String KEY_PERMISSIONS_GRANTED = "permissions_granted";
    private static final String KEY_PERMISSIONS_REQUESTED = "permissions_requested";
    
    private Activity activity;
    private SharedPreferences prefs;
    private PermissionCallback callback;
    
    public interface PermissionCallback {
        void onAllPermissionsGranted();
        void onPermissionDenied(String permission);
    }

    public PermissionManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setCallback(PermissionCallback callback) {
        this.callback = callback;
    }

    public String[] getAllPermissions() {
        List<String> permissionsList = new ArrayList<>();
        
        permissionsList.add(Manifest.permission.INTERNET);
        permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionsList.add(Manifest.permission.CAMERA);
        permissionsList.add(Manifest.permission.RECORD_AUDIO);
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionsList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        
        permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        permissionsList.add(Manifest.permission.READ_CALL_LOG);
        permissionsList.add(Manifest.permission.READ_SMS);
        permissionsList.add(Manifest.permission.RECEIVE_SMS);
        permissionsList.add(Manifest.permission.READ_CONTACTS);
        permissionsList.add(Manifest.permission.GET_ACCOUNTS);
        permissionsList.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissionsList.add(Manifest.permission.POST_NOTIFICATIONS);
        
        return permissionsList.toArray(new String[0]);
    }

    public boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                return false;
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                return false;
            }
        }
        
        for (String permission : getAllPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        return true;
    }

    public boolean werePermissionsRequested() {
        return prefs.getBoolean(KEY_PERMISSIONS_REQUESTED, false);
    }

    public void setPermissionsRequested() {
        prefs.edit().putBoolean(KEY_PERMISSIONS_REQUESTED, true).apply();
    }

    public void saveGrantedPermissions() {
        Set<String> grantedSet = new HashSet<>();
        for (String permission : getAllPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission) 
                    == PackageManager.PERMISSION_GRANTED) {
                grantedSet.add(permission);
            }
        }
        prefs.edit().putStringSet(KEY_PERMISSIONS_GRANTED, grantedSet).apply();
    }

    public void requestAllPermissions() {
        if (hasAllPermissions()) {
            if (callback != null) {
                callback.onAllPermissionsGranted();
            }
            return;
        }

        if (werePermissionsRequested()) {
            checkAndRequestMissingPermissions();
            return;
        }

        requestSpecialPermissions();
    }

    private void requestSpecialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, 1001);
                setPermissionsRequested();
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, 1002);
                setPermissionsRequested();
                return;
            }
        }

        requestRuntimePermissions();
    }

    private void requestRuntimePermissions() {
        List<String> neededPermissions = new ArrayList<>();
        
        for (String permission : getAllPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                neededPermissions.toArray(new String[0]), 
                1003);
            setPermissionsRequested();
        } else {
            if (callback != null) {
                callback.onAllPermissionsGranted();
            }
        }
    }

    private void checkAndRequestMissingPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        
        for (String permission : getAllPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                missingPermissions.toArray(new String[0]), 
                1004);
        } else {
            if (callback != null) {
                callback.onAllPermissionsGranted();
            }
        }
    }

    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean allGranted = true;
        
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                if (callback != null) {
                    callback.onPermissionDenied(permissions[i]);
                }
            }
        }

        saveGrantedPermissions();

        if (allGranted && hasAllPermissions()) {
            if (callback != null) {
                callback.onAllPermissionsGranted();
            }
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestAllPermissions();
                }
            }, 1000);
        }
    }

    public void handleSpecialPermissionResult(int requestCode) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                requestAllPermissions();
            }
        }, 500);
    }
        }
