package com.cars.material.application;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

public class MaterialApplication extends MultiDexApplication {

    public static String SECRETKEY = "045429c430cf25b13f1890bcd27882d001bfa33d16998356a392a9bc654b37f2daaf36ab336b4ddbd0605e456cbbbcb05b5d7788355c7fe09d0021ae41619a0f0c";

    private static Context mContext;
    private static MaterialApplication sInstance;

    public static MaterialApplication getsInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
