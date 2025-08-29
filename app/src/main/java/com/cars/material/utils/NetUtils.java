package com.cars.material.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.cars.material.application.MaterialApplication;

public class NetUtils {

    /**
     * 判断网络是否连接
     *
     * @return
     */
    public static boolean isConnected() {

        ConnectivityManager connectivity = (ConnectivityManager) MaterialApplication.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (null != connectivity) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (null != info && info.isConnected()) {
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }
}
