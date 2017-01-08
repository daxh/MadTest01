package com.daxh.explore.madtest01;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class MadTest01App extends Application {

    private static MadTest01App instance = null;

    public static MadTest01App instance() {
        return instance;
    }

    private RefWatcher refWatcher;
    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this);
    }

    public void watch(Object o) {
        refWatcher.watch(o);
    }
}
