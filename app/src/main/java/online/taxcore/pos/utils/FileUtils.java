package online.taxcore.pos.utils;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;

public class FileUtils {
    /**
     * TAG for log messages.
     */
    static final String TAG = "FileUtils";
    private static final boolean DEBUG = true; // Set to true to enable logging

    /**
     * @return Number of kilo bytes available on External storage
     */
    public static long getAvailableSpaceInKB() {
        final long SIZE_KB = 1024L;
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long availableSpace = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        return availableSpace / SIZE_KB;
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
            logDir(context.getCacheDir());
        } catch (Exception e) {
            Log.e(TAG, "trimCache: ", e);
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    private static void logDir(File dir) {
        if (!DEBUG) return;
        Log.d(TAG, "Dir=" + dir);
        File[] files = dir.listFiles();
        for (File file : files) {
            Log.d(TAG, "File=" + file.getPath());
        }
    }
}
