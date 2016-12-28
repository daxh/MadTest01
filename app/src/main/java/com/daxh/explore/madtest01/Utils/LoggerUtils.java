package com.daxh.explore.madtest01.utils;

import com.orhanobut.logger.Logger;
import com.orhanobut.logger.Settings;

public class LoggerUtils {

    public static final String TAG = "mad";

    public static void brief(){
        configureLogger(TAG, 0, false);
    }

    public static void explicit(int methods){
        configureLogger(TAG, methods, true);
    }

    public static void explicit(){
        explicit(5);
    }

    public static void configureLogger(String tag, int methods, boolean showThreads){
        Settings settings = Logger
                .init(tag)                  // default PRETTYLOGGER or use just init()
                .methodCount(methods);      // default 2
        if (!showThreads) {
            settings.hideThreadInfo();      // default shown
        }
    }
}
