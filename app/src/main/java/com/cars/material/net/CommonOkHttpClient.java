package com.cars.material.net;

import com.cars.material.interceptor.RequestInterceptor;

import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class CommonOkHttpClient {

    private static final int TIME_OUT = 30;
    private volatile static OkHttpClient mOkHttpClient;

    static {
        //获取缓存路径
//        File cacheDir = FileUtils.getDiskCacheDir();
        //设置缓存的大小
//        int cacheSize = 10 * 1024 * 1024;
        //创建我们Client对象的构建者
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder
                //为构建者设置超时时间
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
                ////websocket轮训间隔(单位:秒)
//        .pingInterval(20, TimeUnit.SECONDS)
                //设置缓存
//                .cache(new Cache(cacheDir.getAbsoluteFile(), cacheSize))
                //允许重定向
                .followRedirects(true)
                //设置拦截器
                .addInterceptor(new RequestInterceptor())
                //添加https支持
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession sslSession) {
                        if (RequestUrlManager.HOST_NAME.equals(hostname)) {
                            return true;
                        } else {
                            HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                            return hv.verify(hostname, sslSession);
                        }
                    }
                })
                .sslSocketFactory(HttpsUtils.initSSLSocketFactory(), HttpsUtils.initTrustManager());
        mOkHttpClient = okHttpBuilder.build();
    }

    /**
     * GET请求
     */
    public static Call get(int requestId, Request request, ResponseHandler handler, boolean isList) {
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new CommonJsonCallback(handler, requestId, isList));
        return call;
    }

    /**
     * POST请求
     */
    public static Call post(int requestId, Request request, ResponseHandler handler, boolean isList) {
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new CommonJsonCallback(handler, requestId, isList));
        return call;
    }

    /**
     * 根据tag取消请求
     *
     * @param tag
     */
    public static void cancelTag(Object tag) {
        if (tag == null) {
            return;
        }
        for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
        for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
            if (tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }

    /**
     * 取消所有请求
     */
    public static void cancelAll() {
        for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
            call.cancel();
        }

        for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
            call.cancel();
        }
    }

    public static OkHttpClient getInstence() {
        return mOkHttpClient;
    }
}
