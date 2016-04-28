package com.axa.crashtest;

import android.os.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by Axa on 26/04/16.
 */
public class Util {

    static Logger mLogger = LoggerFactory.getLogger(MainActivity.class);

    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;
        if (!isExternalStorageWritable()){
            return false;
        }
        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            mLogger.info(file.getPath() +  " folder not exists. Create it...");
            if (!file.mkdirs()) {
                mLogger.error("Problem creating folder " + Environment.getExternalStorageDirectory() + path);
                ret = false;
            }
        }
        return ret;
    }

    /** Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mLogger.info("External Storage is writable");
            return true;
        }
        mLogger.error("External Storage is NOT writable");
        return false;
    }
}
