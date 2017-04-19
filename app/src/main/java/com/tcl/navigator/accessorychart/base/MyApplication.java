package com.tcl.navigator.accessorychart.base;

import android.app.Application;

import com.tcl.navigator.accessorychart.utils.CrashHandler;

/**
 * Created by yaohui on 2017/4/19.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler.getInstance().init(this);
    }
}
