package com.cars.material.net;

public interface ResponseCallback {
    //请求成功回调事件处理
    void onSuccess(int requestId, Object responseObj);

    //请求失败回调事件处理
    void onFailure(int requestId, OkHttpException failuer);

}
