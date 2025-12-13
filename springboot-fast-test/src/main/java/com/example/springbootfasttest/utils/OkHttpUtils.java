package com.example.springbootfasttest.utils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * OkHttp 工具类
 * 支持同步/异步的 GET 和 POST 请求
 * 使用单例模式，建议在整个应用中共享此实例
 */
public class OkHttpUtils {

    // 单例实例
    private static OkHttpUtils instance;
    private final OkHttpClient client;

    // 私有构造函数，初始化OkHttpClient
    private OkHttpUtils() {
        // 创建日志拦截器（默认级别为BASIC，生产环境可关闭）
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        // 构建OkHttpClient
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)      // 连接超时
                .readTimeout(30, TimeUnit.SECONDS)         // 读取超时
                .writeTimeout(30, TimeUnit.SECONDS)        // 写入超时
                .addInterceptor(loggingInterceptor)        // 添加日志拦截器
                .build();
    }

    /**
     * 获取单例实例
     */
    public static synchronized OkHttpUtils getInstance() {
        if (instance == null) {
            instance = new OkHttpUtils();
        }
        return instance;
    }

    /**
     * 配置自定义的OkHttpClient（用于特殊配置）
     */
    public void setCustomClient(OkHttpClient customClient) {
        // 注意：此操作会替换默认的client，请谨慎使用
        // 在实际应用中，可以考虑使用clone方式或重新设计配置机制
        // this.client = customClient;
    }

    /**
     * 同步GET请求
     * @param url 请求地址
     * @return Response 响应对象
     * @throws IOException 网络异常
     */
    public Response syncGet(String url) throws IOException {
        return syncGet(url, null);
    }

    /**
     * 同步GET请求（带请求头）
     * @param url 请求地址
     * @param headers 请求头Map
     * @return Response 响应对象
     * @throws IOException 网络异常
     */
    public Response syncGet(String url, Map<String, String> headers) throws IOException {
        Request request = buildGetRequest(url, headers);
        return client.newCall(request).execute();
    }

    /**
     * 同步POST请求（表单格式）
     * @param url 请求地址
     * @param formData 表单数据Map
     * @return Response 响应对象
     * @throws IOException 网络异常
     */
    public Response syncPostForm(String url, Map<String, String> formData) throws IOException {
        return syncPostForm(url, formData, null);
    }

    /**
     * 同步POST请求（表单格式，带请求头）
     * @param url 请求地址
     * @param formData 表单数据Map
     * @param headers 请求头Map
     * @return Response 响应对象
     * @throws IOException 网络异常
     */
    public Response syncPostForm(String url, Map<String, String> formData,
                                 Map<String, String> headers) throws IOException {
        RequestBody body = buildFormBody(formData);
        Request request = buildPostRequest(url, body, headers);
        return client.newCall(request).execute();
    }

    /**
     * 同步POST请求（JSON格式）
     * @param url 请求地址
     * @param json JSON字符串
     * @return Response 响应对象
     * @throws IOException 网络异常
     */
    public Response syncPostJson(String url, String json) throws IOException {
        return syncPostJson(url, json, null);
    }

    /**
     * 同步POST请求（JSON格式，带请求头）
     * @param url 请求地址
     * @param json JSON字符串
     * @param headers 请求头Map
     * @return Response 响应对象
     * @throws IOException 网络异常
     */
    public Response syncPostJson(String url, String json,
                                 Map<String, String> headers) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = buildPostRequest(url, body, headers);
        return client.newCall(request).execute();
    }

    /**
     * 异步GET请求
     * @param url 请求地址
     * @param callback 回调接口
     */
    public void asyncGet(String url, Callback callback) {
        asyncGet(url, null, callback);
    }

    /**
     * 异步GET请求（带请求头）
     * @param url 请求地址
     * @param headers 请求头Map
     * @param callback 回调接口
     */
    public void asyncGet(String url, Map<String, String> headers, Callback callback) {
        Request request = buildGetRequest(url, headers);
        client.newCall(request).enqueue(callback);
    }

    /**
     * 异步POST请求（表单格式）
     * @param url 请求地址
     * @param formData 表单数据Map
     * @param callback 回调接口
     */
    public void asyncPostForm(String url, Map<String, String> formData, Callback callback) {
        asyncPostForm(url, formData, null, callback);
    }

    /**
     * 异步POST请求（表单格式，带请求头）
     * @param url 请求地址
     * @param formData 表单数据Map
     * @param headers 请求头Map
     * @param callback 回调接口
     */
    public void asyncPostForm(String url, Map<String, String> formData,
                              Map<String, String> headers, Callback callback) {
        RequestBody body = buildFormBody(formData);
        Request request = buildPostRequest(url, body, headers);
        client.newCall(request).enqueue(callback);
    }

    /**
     * 异步POST请求（JSON格式）
     * @param url 请求地址
     * @param json JSON字符串
     * @param callback 回调接口
     */
    public void asyncPostJson(String url, String json, Callback callback) {
        asyncPostJson(url, json, null, callback);
    }

    /**
     * 异步POST请求（JSON格式，带请求头）
     * @param url 请求地址
     * @param json JSON字符串
     * @param headers 请求头Map
     * @param callback 回调接口
     */
    public void asyncPostJson(String url, String json,
                              Map<String, String> headers, Callback callback) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, json);
        Request request = buildPostRequest(url, body, headers);
        client.newCall(request).enqueue(callback);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 构建GET请求
     */
    private Request buildGetRequest(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        return builder.build();
    }

    /**
     * 构建POST请求
     */
    private Request buildPostRequest(String url, RequestBody body,
                                     Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);
        addHeaders(builder, headers);
        return builder.build();
    }

    /**
     * 构建表单请求体
     */
    private RequestBody buildFormBody(Map<String, String> formData) {
        FormBody.Builder builder = new FormBody.Builder();
        if (formData != null && !formData.isEmpty()) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    /**
     * 添加请求头
     */
    private void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 获取内部的OkHttpClient实例（用于高级定制）
     */
    public OkHttpClient getClient() {
        return client;
    }
}