package com.example.springbootfasttest.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import com.zhiyue.star.basic.common.util.CoderUtils;
import com.zhiyue.star.basic.common.util.IdUtils;
import com.zhiyue.star.basic.common.util.JsonUtils;
import com.zhiyue.star.basic.common.util.StringUtilsPlus;

/**
 * OkHttp工具类
 * 
 * @author zhaoyc
 * @version 创建时间：2017年10月18日 上午10:40:54
 */
public class OkHttpUtil {
    /**
     * 默认 MEDIA_TYPE application/json; charset=utf-8
     */
    private static final MediaType MEDIA_TYPE_APPLICATION_JSON_UTF8 = MediaType.parse("application/json; charset=utf-8");

    /**
     * OkHttpClient
     */
    private static OkHttpClient client;

    /**
     * 设置底层读超时,以毫秒为单位。值0指定无限超时。
     *
     */
    private volatile static int readTimeout = 5 * 60 * 1000;
    /**
     * 设置底层写超时,以毫秒为单位。值0指定无限超时。
     *
     */
    private volatile static int writeTimeout = 5 * 60 * 1000;
    /**
     * 设置底层连接超时,以毫秒为单位。值0指定无限超时。
     *
     */
    private volatile static int connectTimeout = 5 * 60 * 1000;

    /**
     * 是否忽略SSL
     */
    private volatile static boolean ignoreSsl = false;

    /**
     * 网络连接失败重试次数
     */
    private static final int RETRY_TIMES = 5;

    /**
     * 连接池
     */
    private static final ConnectionPool CONNECTION_POOL = new ConnectionPool(20, 5, TimeUnit.MINUTES);

    static {
        boolean proxy = false;
        if (proxy) {
            // 走本地翻墙代理, 需要开启翻墙端口要对得上, 一般用于测试
            client = new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 10808))).build();
        } else {
            client = new OkHttpClient();
        }

        client = client.newBuilder().connectionPool(CONNECTION_POOL).build();
        client = client.newBuilder().readTimeout(readTimeout, TimeUnit.MILLISECONDS).build();
        client = client.newBuilder().writeTimeout(writeTimeout, TimeUnit.MILLISECONDS).build();
        client = client.newBuilder().connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).build();

        try {
            if (ignoreSsl) {
                ignoreSslSocketFactory();
            }
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            LogManager.getLogger().error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * GET请求
     * 
     * @param url 请求url地址
     * @throws Exception
     */
    public static String doGet(String url) throws Exception {
        return doGet(url, null, null);
    }

    /**
     * GET请求
     * 
     * @param url 请求url地址
     * @param parameters 请求参数，如果不为空则要求url不包含任何参数
     * @throws Exception
     */
    public static String doGet(String url, Map<String, String> parameters) throws Exception {
        return doGet(url, parameters, null);
    }



    /**
     * 同步GET请求
     * 
     * @param url 请求url地址
     * @param parameters 请求参数，如果不为空则要求url不包含任何参数
     * @param headers 请求头
     * @throws Exception
     */
    public synchronized static String doGetSync(String url, Map<String, String> parameters, Map<String, String> headers) throws Exception {
        return doGet(url, parameters, headers);
    }

    /**
     * GET请求
     * 
     * @param url 请求url地址
     * @param parameters 请求参数，如果不为空则要求url不包含任何参数
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doGet(String url, Map<String, String> parameters, Map<String, String> headers) throws Exception {
        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        url = buildUrl(url, parameters); // 把参数加到url后面

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, \n\n请求参数: \n{}, 请求headers: {}", url, parameters, headers);

        Request.Builder builder = new Request.Builder().url(url).get();

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    /**
     * GET请求下载
     * 
     * @param url 请求url地址
     * @param parameters 请求参数，如果不为空则要求url不包含任何参数
     * @param headers 请求头
     * @param outputFile 输出本地文件，如：abc/aa.pdf
     * @return
     * @throws Exception
     */
    public static void download(String url, Map<String, String> parameters, Map<String, String> headers, String outputFile) throws Exception {
        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        url = buildUrl(url, parameters); // 把参数加到url后面

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, \n\n请求参数: \n{}, 请求headers: {}, outputFile:{}", url, parameters, headers, outputFile);

        Request.Builder builder = new Request.Builder().url(url).get();

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                FileUtils.copyInputStreamToFile(response.body().byteStream(), new File(outputFile));
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

    }

    /**
     * GET请求下载
     * 
     * @param url 请求url地址
     * @param parameters 请求参数，如果不为空则要求url不包含任何参数
     * @param headers 请求头
     * @param responseHeaders 返回头
     * @return
     * @throws Exception
     */
    public static byte[] download(String url, Map<String, String> parameters, Map<String, String> headers, Map<String, String> responseHeaders) throws Exception {
        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        url = buildUrl(url, parameters); // 把参数加到url后面

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, \n\n请求参数: \n{}, 请求headers: {}, responseHeaders:{}", url, parameters, headers, responseHeaders);

        Request.Builder builder = new Request.Builder().url(url).get();

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                if (responseHeaders != null) {
                    response.headers().forEach(pair -> {
                        responseHeaders.put(pair.getFirst(), pair.getSecond());
                    });
                }

                return response.body().bytes();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }


    /**
     * url参数封装
     * 
     * @param url 不包含参数
     * @param parameters 参数列表
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String buildUrl(String url, Map<String, String> parameters) throws UnsupportedEncodingException {
        if (parameters == null) {
            return url;
        }

        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(url);

        StringBuilder sbQuery = new StringBuilder();
        for (Entry<String, String> query : parameters.entrySet()) {
            String key = query.getKey();
            String value = query.getValue();

            if (sbQuery.length() > 0) {
                sbQuery.append("&");
            }

            if (StringUtils.isBlank(key) && !StringUtils.isBlank(value)) {
                sbQuery.append(CoderUtils.urlEncode(value));
            } else if (!StringUtils.isBlank(key)) {
                sbQuery.append(key);
                if (!StringUtils.isBlank(value)) {
                    sbQuery.append("=");
                    sbQuery.append(CoderUtils.urlEncode(value));
                }
            }
        }

        if (sbQuery.length() >= 1) {
            sbUrl.append("?").append(sbQuery);
        }

        return sbUrl.toString();
    }

    /**
     * Post请求，json body参数
     *
     * @param url 请求url地址
     * @return
     * @throws Exception
     */
    public static String doJsonPost(String url) throws Exception {
        return doJsonPost(url, null, null, null);
    }


    /**
     * Post请求，json body参数
     *
     * @param url 请求url地址
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @return
     * @throws Exception
     */
    public static String doJsonPost(String url, String jsonBody) throws Exception {
        return doJsonPost(url, null, jsonBody, null);
    }


    /**
     * Post请求，json body参数
     *
     * @param url 请求url地址
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doJsonPost(String url, String jsonBody, Map<String, String> headers) throws Exception {
        return doJsonPost(url, null, jsonBody, headers);
    }


    /**
     * 同步Post请求，json body参数
     *
     * @param url 请求url地址
     * @param contentType 如 application/json; charset=utf-8
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public synchronized static String doJsonPostSync(String url, String contentType, String jsonBody, Map<String, String> headers) throws Exception {
        return doJsonPost(url, contentType, jsonBody, headers);
    }


    /**
     * Post请求，json body参数
     *
     * @param url 请求url地址
     * @param contentType 如 application/json; charset=utf-8
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doJsonPost(String url, String contentType, String jsonBody, Map<String, String> headers) throws Exception {

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, 请求contentType: {}, \n\n请求参数: \n{}, 请求headers: {}", url, contentType, jsonBody, headers);

        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        MediaType mediaType = MEDIA_TYPE_APPLICATION_JSON_UTF8;
        if (StringUtils.isNotBlank(contentType)) {
            mediaType = MediaType.parse(contentType);
        }

        if (jsonBody == null || jsonBody.isEmpty()) {
            jsonBody = "{}";
        }

        // 参数设置
        RequestBody body = RequestBody.create(jsonBody, mediaType);
        Request.Builder builder = new Request.Builder().url(url).post(body);

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }

    /**
     * Patch请求，json body参数
     *
     * @param url 请求url地址
     * @param contentType 如 application/json; charset=utf-8
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doJsonPatch(String url, String contentType, String jsonBody, Map<String, String> headers) throws Exception {

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, 请求contentType: {}, \n\n请求参数: \n{}, 请求headers: {}", url, contentType, jsonBody, headers);

        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        MediaType mediaType = MEDIA_TYPE_APPLICATION_JSON_UTF8;
        if (StringUtils.isNotBlank(contentType)) {
            mediaType = MediaType.parse(contentType);
        }

        if (jsonBody == null || jsonBody.isEmpty()) {
            jsonBody = "{}";
        }

        // 参数设置
        RequestBody body = RequestBody.create(jsonBody, mediaType);
        Request.Builder builder = new Request.Builder().url(url).patch(body);

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }


    /**
     * Post请求，Form表单参数
     *
     * @param url 请求url地址
     * @return
     * @throws Exception
     */
    public static String doPost(String url) throws Exception {
        return doPost(url, null, null);
    }

    /**
     * Post请求，Form表单参数
     *
     * @param url 请求url地址
     * @param parameters 表单参数
     * @return
     * @throws Exception
     */
    public static String doPost(String url, Map<String, String> parameters) throws Exception {
        return doPost(url, parameters, null);
    }

    /**
     * 同步Post请求，Form表单参数
     *
     * @param url 请求url地址
     * @param parameters 表单参数
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public synchronized static String doPostSync(String url, Map<String, String> parameters, Map<String, String> headers) throws Exception {
        return doPost(url, parameters, headers);
    }

    /**
     * Post请求，Form表单参数
     *
     * @param url 请求url地址
     * @param parameters 表单参数
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doPost(String url, Map<String, String> parameters, Map<String, String> headers) throws Exception {

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, \n\n请求参数: \n{}, 请求headers: {}", url, parameters, headers);

        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        // 参数设置
        FormBody.Builder body = new FormBody.Builder();
        if (parameters != null && parameters.size() >= 1) {
            for (Entry<String, String> entity : parameters.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    body.addEncoded(key, CoderUtils.urlEncode(value));
                }
            }

        }

        // header设置
        final Request.Builder builder = new Request.Builder().url(url);
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.post(body.build()).build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }

    /**
     * Put请求，json body参数
     *
     * @param url 请求url地址
     * @param contentType 如 application/json; charset=utf-8
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doJsonPut(String url, String contentType, String jsonBody, Map<String, String> headers) throws Exception {

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, 请求contentType: {}, \n\n请求参数: \n{}, 请求headers: {}", url, contentType, jsonBody, headers);

        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        MediaType mediaType = MEDIA_TYPE_APPLICATION_JSON_UTF8;
        if (StringUtils.isNotBlank(contentType)) {
            mediaType = MediaType.parse(contentType);
        }

        if (jsonBody == null || jsonBody.isEmpty()) {
            jsonBody = "{}";
        }

        // 参数设置
        RequestBody body = RequestBody.create(jsonBody, mediaType);
        Request.Builder builder = new Request.Builder().url(url).put(body);

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }


    /**
     * Put请求，Form表单参数
     *
     * @param url 请求url地址
     * @param parameters 表单参数
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doPut(String url, Map<String, String> parameters, Map<String, String> headers) throws Exception {

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, \n\n请求参数: \n{}, 请求headers: {}", url, parameters, headers);

        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        // 参数设置
        FormBody.Builder body = new FormBody.Builder();
        if (parameters != null && parameters.size() >= 1) {
            for (Entry<String, String> entity : parameters.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    body.addEncoded(key, CoderUtils.urlEncode(value));
                }
            }

        }

        // header设置
        final Request.Builder builder = new Request.Builder().url(url);
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.put(body.build()).build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }

    /**
     * Delete请求，json body参数
     *
     * @param url 请求url地址
     * @param contentType 如 application/json; charset=utf-8
     * @param jsonBody body参数，格式：{"username":"admin","password":"123456"}
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String doJsonDelete(String url, String contentType, String jsonBody, Map<String, String> headers) throws Exception {

        // 不能打印多行，避免请求日志服务造成死循环，发送日志会判断url不是日志服务才发送
        LogManager.getLogger().debug("请求地址: {}, 请求contentType: {}, \n\n请求参数: \n{}, 请求headers: {}", url, contentType, jsonBody, headers);

        if (url == null) {
            throw new RuntimeException("url can not empty");
        }

        MediaType mediaType = MEDIA_TYPE_APPLICATION_JSON_UTF8;
        if (StringUtils.isNotBlank(contentType)) {
            mediaType = MediaType.parse(contentType);
        }

        if (jsonBody == null || jsonBody.isEmpty()) {
            jsonBody = "{}";
        }

        // 参数设置
        RequestBody body = RequestBody.create(jsonBody, mediaType);
        Request.Builder builder = new Request.Builder().url(url).delete(body);

        // header设置
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }


    /**
     * 文件上传
     * 
     * @param url 请求url地址
     * @param filePath 文件路径，包含文件名
     * @param parameters 表单参数
     * @param headers 请求头
     * @return
     * @throws Exception
     */
    public static String upload(String url, String filePath, Map<String, String> parameters, Map<String, String> headers) throws Exception {
        File file = new File(filePath);
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        // 添加文件
        bodyBuilder.addFormDataPart("file", file.getName(), fileBody);

        // 参数设置
        if (parameters != null && parameters.size() >= 1) {
            for (Entry<String, String> entity : parameters.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    bodyBuilder.addFormDataPart(key, value);
                }
            }
        }

        // header设置
        final Request.Builder builder = new Request.Builder().url(url);
        if (headers != null && headers.size() >= 1) {
            for (Entry<String, String> entity : headers.entrySet()) {
                String key = entity.getKey();
                String value = entity.getValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    builder.addHeader(key, value);
                }
            }
        }

        Request request = builder.post(bodyBuilder.build()).url(url).build();

        for (int i = 0; i < RETRY_TIMES; i++) {
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (SocketException e) {
                boolean isRetry = e instanceof ConnectException;
                isRetry = isRetry ? true : StringUtilsPlus.isContainsConnectException(ExceptionUtils.getStackTrace(e));

                // 只有连接失败才休眠后重试, 最后一次重试抛异常
                if (i == RETRY_TIMES - 1 || !isRetry) {
                    throw new SocketException(ExceptionUtils.getStackTrace(e));
                }
                Thread.sleep(1000);
            }
        }

        return null;
    }


    /**
     * 忽略SSL
     * 
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     *
     * @see OkHttpClient.Builder#sslSocketFactory(SSLSocketFactory , X509TrustManager)
     */
    public static void ignoreSslSocketFactory() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {trustManager}, null);
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        client = client.newBuilder().sslSocketFactory(sslSocketFactory, trustManager).hostnameVerifier(getHostnameVerifier()).build();
    }

    /**
     * HostnameVerifier
     * 
     * @return HostnameVerifier
     */
    private static HostnameVerifier getHostnameVerifier() {
        return DefaultHostnameVerifier.INSTANCE;
    }

    /**
     * 
     * @author zhaoyc
     * @create:2024年3月27日 下午3:45:27
     */
    private enum DefaultHostnameVerifier implements HostnameVerifier {
        //
        INSTANCE;

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }


    public static void main(String[] args) throws Exception {

        Map<String, String> headers = new HashMap<>(3);
        headers.put("systemCode", "test");
        headers.put("uuid", IdUtils.nextFormatId());
        headers.put("test", null);

        Map<String, String> parameters = new HashMap<>(2);
        parameters.put("configKey", "fb");
        parameters.put("test", null);

        String res = doGet("http://172.16.30.118:3889/base-configs/get", parameters, headers);
        System.out.println(res);

        headers = new HashMap<>(3);
        headers.put("systemCode", "test");
        headers.put("uuid", IdUtils.nextFormatId());
        headers.put("test", null);

        parameters = new HashMap<>(5);
        parameters.put("msg", "钉钉测试内容");
        parameters.put("serverName", "msg");
        parameters.put("serverEnvActive", "test");
        parameters.put("test", null);

        res = doPost("http://172.16.30.118:50006/yjl/notify/dingtalk", parameters, headers);
        System.out.println(res);


        parameters = new HashMap<>(2);
        parameters.put("username", "18898759328");
        parameters.put("password", "abc@123456");

        res = OkHttpUtil.doJsonPost("http://172.31.71.146:8080/auth/open/auth/login", JsonUtils.toJson(parameters));
        System.out.println(res);
    }

}
