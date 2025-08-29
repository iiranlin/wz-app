package com.cars.material.net;

public class RequestParamsFactory {

    private volatile static RequestParamsFactory mInstance;

    private RequestParamsFactory() {

    }

    public static RequestParamsFactory getInstance() {
        if (mInstance == null) {
            synchronized (RequestParamsFactory.class) {
                if (mInstance == null) {
                    mInstance = new RequestParamsFactory();
                }
            }
        }
        return mInstance;
    }

    /**
     * 无参默认方法
     *
     * @return
     */
    public RequestParams getDefaultParams() {
        RequestParams requestParams = new RequestParams();
        return requestParams;
    }

    /**
     * 用户登录
     *
     * @return
     */
    public RequestParams getLoginParams(String data) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("data", data);
        return requestParams;
    }

    /**
     * 短信登录
     *
     * @return
     */
    public RequestParams getSmsLoginParams(String phone, String code, String deviceNum) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("phone", phone);
        requestParams.put("code", code);
        requestParams.put("deviceNum", deviceNum);
        return requestParams;
    }

    /**
     * 获取手机验证码
     *
     * @param phone
     * @return
     */
    public RequestParams getSmsVerificationCode(String phone) {
        RequestParams requestParams = new RequestParams();
        requestParams.put("phone", phone);
        return requestParams;
    }
}
