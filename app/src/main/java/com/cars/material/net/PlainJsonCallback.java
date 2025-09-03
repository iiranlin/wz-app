package com.cars.material.net;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.cars.material.utils.NetUtils;

import org.json.JSONObject;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 描述：处理不需要SM4解密的JSON数据回调响应
 */
public class PlainJsonCallback implements Callback {

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

    public PlainJsonCallback(ResponseHandler handle, int requestId, boolean isList) {
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
            // 不进行SM4解密，直接获取响应内容
            result = response.body().string();
            android.util.Log.d("PlainJsonCallback", "原始响应数据: " + result);
            
            mDeliveryHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleResponse(result);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mDeliveryHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFailure(mRequestId, new OkHttpException(OTHER_ERROR, "响应解析失败: " + e.getMessage()));
                }
            });
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
            android.util.Log.d("PlainJsonCallback", "开始解析JSON: " + result);
            
            // 直接解析整个响应为目标对象
            if (mClass != null) {
                Object object;
                if (mIsList) {
                    object = JSON.parseArray(result, mClass);
                } else {
                    object = JSON.parseObject(result, mClass);
                }
                android.util.Log.d("PlainJsonCallback", "解析结果: " + JSON.toJSONString(object));
                mListener.onSuccess(mRequestId, object);
            } else {
                // 如果没有指定类型，直接返回字符串
                mListener.onSuccess(mRequestId, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("PlainJsonCallback", "JSON解析失败: " + e.getMessage());
            mListener.onFailure(mRequestId, new OkHttpException(OTHER_ERROR, "JSON解析失败: " + e.getMessage()));
        }
    }
}
