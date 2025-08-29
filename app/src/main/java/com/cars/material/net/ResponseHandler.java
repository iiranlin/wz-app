package com.cars.material.net;

/**
 * 封装回调接口和要转换的实体对象
 */
public class ResponseHandler {

    public ResponseCallback mListener = null;
    public Class<?> mClass = null;

    public ResponseHandler(ResponseCallback listener) {
        this.mListener = listener;
    }

    public ResponseHandler(ResponseCallback listener, Class<?> clazz) {
        this.mListener = listener;
        this.mClass = clazz;
    }
}
