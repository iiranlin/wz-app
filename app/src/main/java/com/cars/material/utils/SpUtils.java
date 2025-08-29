package com.cars.material.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SpUtils {

    private static SharedPreferences mSp;
    private static final String SP_NAME = "material";

    //用户信息
    public static final String USER_NAME = "user_name";
    public static final String PASS_WORD = "pass_word";
    public static final String SAVE_PASS_WORD = "save_pass_word";
    public static final String TOKEN = "token";


    /**
     * 存布尔值
     */
    public static void putBoolean(Context context, String key, boolean value) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        mSp.edit().putBoolean(key, value).commit();
    }

    /**
     * 取布尔值
     */
    public static boolean getBoolean(Context context, String key, boolean defValue) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        return mSp.getBoolean(key, defValue);
    }

    /**
     * 存int方法
     */
    public static void putInt(Context context, String key, int value) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        mSp.edit().putInt(key, value).commit();
    }

    /**
     * 取int方法
     */
    public static int getInt(Context context, String key, int defValue) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        return mSp.getInt(key, defValue);
    }

    /**
     * 存String方法
     */
    public static void putString(Context context, String key, String value) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        mSp.edit().putString(key, value).commit();
    }

    /**
     * 取String方法
     */
    public static String getString(Context context, String key, String defValue) {
        if (mSp == null) {
            mSp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        return mSp.getString(key, defValue);
    }
}
