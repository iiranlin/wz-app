package com.cars.material.net;

public class RequestMode {

    /**
     * GET请求
     *
     * @param requestId  请求码
     * @param requestUrl URL请求地址
     * @param params     入参
     * @param callback   回调接口
     * @param clazz      需要解析的实体类
     * @param isList     实体类是否是List
     */
    public static void getRequest(int requestId, String requestUrl, RequestParams params,
                                  ResponseCallback callback, Class<?> clazz, boolean isList) {
        CommonOkHttpClient.get(requestId, CommonRequest.createGetRequest(requestUrl, params),
                new ResponseHandler(callback, clazz), isList);
    }

    /**
     * POST请求
     *
     * @param requestId  请求码
     * @param requestUrl URL请求地址
     * @param params     入参
     * @param callback   回调接口
     * @param clazz      需要解析的实体类
     * @param isList     实体类是否是List
     */

    public static void postRequest(int requestId, String requestUrl, RequestParams params,
                                   ResponseCallback callback, Class<?> clazz, boolean isList) {
        CommonOkHttpClient.post(requestId, CommonRequest.createPostRequest(requestUrl, params),
                new ResponseHandler(callback, clazz), isList);

    }

    /**
     * POST请求（sm4加密）
     *
     * @param requestId  请求码
     * @param requestUrl URL请求地址
     * @param params     入参
     * @param callback   回调接口
     * @param clazz      需要解析的实体类
     * @param isList     实体类是否是List
     */

    public static void postRequestSm4(int requestId, String requestUrl, RequestParams params,
                                   ResponseCallback callback, Class<?> clazz, boolean isList) {
        CommonOkHttpClient.post(requestId, CommonRequest.createPostRequestSm4(requestUrl, params),
                new ResponseHandler(callback, clazz), isList);

    }

    /**
     * 根据tag取消请求
     */
    public static void cancelTag(String... tags) {
        for (String tag : tags) {
            CommonOkHttpClient.cancelTag(tag);
        }
    }

    /**
     * 取消所有请求
     */
    public static void cancelAll() {
        CommonOkHttpClient.cancelAll();
    }
}
