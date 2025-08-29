package com.cars.material.net;

import android.util.Log;

import com.alibaba.fastjson.JSONException;
import com.cars.material.application.MaterialApplication;
import com.cars.material.utils.Sm2Utils;
import com.cars.material.utils.Sm4Util;

import org.json.JSONObject;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 描述： 公共入参
 */

public class CommonRequest {

    /**
     * 创建Get请求的Request
     */
    public static Request createGetRequest(String url, RequestParams params) {
        String urlPath = RequestUrlManager.HOST + url;
        StringBuilder urlBuilder = new StringBuilder(urlPath);
        if (params != null) {
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : params.urlParams.entrySet()) {
                urlBuilder
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue())
                        .append("&");
            }
        }
        String urlStr = urlBuilder.toString();
        return new Request.Builder()
                .url(urlStr.substring(0, urlStr.length() - 1))
                .get()
                .tag(url)
                .build();
    }

    /**
     * 创建Post请求的Request
     */
    public static Request createPostRequest(String url, RequestParams params) {
        String urlPath = RequestUrlManager.HOST + url;
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : params.urlParams.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        Log.e("TAG", "入参JSON= " + jsonObject.toString());
        RequestBody mFormBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
                jsonObject.toString());

        Request request = new Request.Builder()
                .url(urlPath)
                .post(mFormBody)
                .tag(url)
                .header("content-type", "application/json")
                .build();
        return request;
    }

    /**
     * 创建Post请求的Request（sm4）
     */
    public static Request createPostRequestSm4(String url, RequestParams params) {
        String urlPath = RequestUrlManager.HOST + url;
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : params.urlParams.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        Log.e("TAG", "入参JSON= " + jsonObject.toString());
        RequestBody mFormBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
                Sm4Util.encrypt(jsonObject.toString()));

        Request request = new Request.Builder()
                .url(urlPath)
                .post(mFormBody)
                .tag(url)
                .header("content-type", "application/json")
                .build();
        return request;
    }
}
