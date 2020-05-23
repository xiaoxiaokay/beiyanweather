package com.beiyanweather.android.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {

    /**
     *  发起一条HTTP请求只需要调用sendOkHttpRequest()方法；
     *  传入请求地址；
     *  注册一个回调来处理服务器响应。
     */
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback)
    {
        // 创建OkHttpClient实例
        OkHttpClient client = new OkHttpClient();
        // 想要发起请求就需要创建一个Request对象,可以通过url()方法设置目标的网络地址
        Request request = new Request.Builder().url(address).build();
        // 调用OkHttpClient的newCall()方法创建一个Call对象，调用它的enqueue方法来发送请求并获取服务器返回的数据
        client.newCall(request).enqueue(callback);
    }
}
