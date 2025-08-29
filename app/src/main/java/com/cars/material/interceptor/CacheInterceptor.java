package com.cars.material.interceptor;

import android.text.TextUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        String cacheControl = request.cacheControl().toString();
        if(TextUtils.isEmpty(cacheControl)){
            cacheControl = "max-age=60";
        }
        return response.newBuilder().removeHeader("Pragma").header("Cache-Control",cacheControl).build();
    }
}
