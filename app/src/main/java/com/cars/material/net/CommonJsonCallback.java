package com.cars.material.net;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.cars.material.utils.NetUtils;
import com.cars.material.utils.Sm4Util;

import org.json.JSONObject;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 描述：专门处理JSON数据的回调响应
 */

public class CommonJsonCallback implements Callback {

    private final String RESULT_CODE = "code";
    private final String RESULT_MSG = "message";
    private final String RESULT_DATA = "data";

    private final int NETWORK_ERROR = -1; //网络失败
    private final int TIMEOUT_ERROR = -2; //请求超时
    private final int OTHER_ERROR = -3; //未知错误

    private Handler mDeliveryHandler;
    private ResponseCallback mListener;
    private Class<?> mClass;
    private int mRequestId;
    private boolean mIsList;

    public CommonJsonCallback(ResponseHandler handle, int requestId, boolean isList) {
        this.mRequestId = requestId;
        this.mListener = handle.mListener;
        this.mClass = handle.mClass;
        this.mDeliveryHandler = new Handler(Looper.getMainLooper());
        this.mIsList = isList;
    }

    /**
     * 请求失败的处理
     */
    @Override
    public void onFailure(@NonNull Call call, @NonNull final IOException e) {
        if (call.isCanceled()) {
            return;
        }
        mDeliveryHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!NetUtils.isConnected()) {
                    mListener.onFailure(mRequestId, new OkHttpException(NETWORK_ERROR, "请检查网络"));
                } else if (e instanceof SocketTimeoutException) {
                    mListener.onFailure(mRequestId, new OkHttpException(TIMEOUT_ERROR, "请求超时"));
                } else if (e instanceof ConnectException) {
                    mListener.onFailure(mRequestId, new OkHttpException(OTHER_ERROR, "请求服务器失败"));
                } else {
                    mListener.onFailure(mRequestId, new OkHttpException(OTHER_ERROR, e.getMessage()));
                }
            }
        });
    }

    /**
     * 请求成功的处理回调在主线程
     */
    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        final String result;
        try {
            result = Sm4Util.decrypt(response.body().string());
            mDeliveryHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleResponse(result);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理Http成功的响应
     */
    private void handleResponse(String result) {
        if (TextUtils.isEmpty(result)) {
            mListener.onFailure(mRequestId, new OkHttpException(OTHER_ERROR, "数据异常"));
            return;
        }
        try {
            JSONObject responseObj = new JSONObject(result);
            if (responseObj.has(RESULT_CODE)) {
                if (responseObj.getInt(RESULT_CODE) == 0) {
                    if (mClass == null) {
                        if (responseObj.has(RESULT_DATA)) {
                            mListener.onSuccess(mRequestId, responseObj.getString(RESULT_DATA));
                            return;
                        }
                    } else {
                        if (responseObj.has(RESULT_DATA)) {
                            String data = responseObj.getString(RESULT_DATA);
                            if (TextUtils.isEmpty(data)) {
                                mListener.onSuccess(mRequestId, responseObj.getString(RESULT_MSG));
                            } else {
                                Object object;

                                if (mIsList) {
                                    object = JSON.parseArray(responseObj.getString(RESULT_DATA), mClass);
                                } else {
                                    object = JSON.parseObject(responseObj.getString(RESULT_DATA), mClass);
                                }
                                mListener.onSuccess(mRequestId, object);
                            }
                        } else {
                            mListener.onSuccess(mRequestId, responseObj.getString(RESULT_MSG));
                        }
                    }
                } else { //将服务端返回的异常回调到应用层去处理
                    if (responseObj.has(RESULT_MSG)) {
                        mListener.onFailure(mRequestId, new OkHttpException(responseObj.getInt(RESULT_CODE), responseObj.getString(RESULT_MSG)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mListener.onFailure(mRequestId, new OkHttpException(OTHER_ERROR, "服务器异常"));
        }
    }
}
