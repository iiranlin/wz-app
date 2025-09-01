package com.cars.material.net;

import com.cars.material.BuildConfig;

public class RequestUrlManager {

//    public static final String HOST = "http://39.107.57.20:38222/api";
//    public static final String MOBILE_HOST = "http://39.107.57.20:38222/";
//    public static final String HOST_NAME = "39.107.57.20";

    public static final String HOST = BuildConfig.API_URL;
    public static final String MOBILE_HOST = BuildConfig.SERVER_URL;
    public static final String HOST_NAME = BuildConfig.HOST_NAME;

    //验证码
    public static final String GET_VERIFICATION_CODE = "/blcd-base/auth/randomImage";
    //用户登录
    public static final String GET_LOGIN_INFO = "/blcd-base/auth/login";
    //短信登录登录
    public static final String GET_SMS_LOGIN_INFO = "/blcd-base/auth/loginByPhone";
    //获取手机验证码
    public static final String GET_SMS_VERIFICATION_CODE = "/prodmgr-inv/auth/sendCaptcha";
}
