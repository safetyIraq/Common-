package com.v8.global.sniffer;

import android.os.Build;
import android.os.StatFs;
import java.io.File;

public class Environment {
    
    public static File getExternalStorageDirectory() {
        return android.os.Environment.getExternalStorageDirectory();
    }
    
    public static boolean isExternalStorageManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return android.os.Environment.isExternalStorageManager();
        }
        return true;
    }
    
    public static long getAvailableStorageSize() {
        StatFs stat = new StatFs(getExternalStorageDirectory().getPath());
        long bytesAvailable = (long) stat.getBlockSizeLong() * (long) stat.getAvailableBlocksLong();
        return bytesAvailable / (1024 * 1024);
    }
}
