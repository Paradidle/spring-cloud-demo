package com.example.springbootfasttest.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import com.example.springbootfasttest.result.CustomHttpServletRequestWrapper;

import static com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat.UUID;

@Component
@Slf4j
public class LoggingFilter implements Filter {
    private static final String ignoredPaths = "/ncOrderForGoods/syncOrder,/ncAICallRecords/receive," +
            "/passport/sendCode,/passport/login,/ncUser/getEncryptCode,/nc/activityInfo/,/login/generateUrlLink";

    private List<String> ignoredPathList;

    private static final String TRACE_ID = "traceId";
    private static final String TRACE_KEY = "traceKey";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ignoredPathList = Arrays.asList(ignoredPaths.split(","));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String requestUri = httpRequest.getRequestURI();
            // 检查请求路径是否在忽略列表中
            if (ignoredPathList != null && ignoredPathList.contains(requestUri)) {
                // 直接放行
                chain.doFilter(request, response);
                return;
            }
            //拦截swagger请求
            if (requestUri.startsWith("/swagger-ui") ||
                    requestUri.startsWith("/v2/api-docs") ||
                    requestUri.startsWith("/swagger-resources") ||
                    requestUri.startsWith("/webjars") ||
                    requestUri.startsWith("/doc.html") ||
                    requestUri.startsWith("/api-docs")) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
                return;
            }
            String traceId = httpRequest.getHeader(TRACE_ID);
            if (ObjectUtils.isEmpty(traceId)) {
                traceId = UUID.toString();
            }
            MDC.put(TRACE_ID, traceId);
            // 记录请求开始时间
            long startTime = System.currentTimeMillis();
            // 使用自定义包装类
            CustomHttpServletRequestWrapper requestWrapper = new CustomHttpServletRequestWrapper(httpRequest);
            // 记录请求日志
            logRequestInfo(requestWrapper);
            try {
                // 继续处理请求
                chain.doFilter(requestWrapper, response);
            } finally {
                // 记录请求结束时间
                long endTime = System.currentTimeMillis();
                // 计算请求处理时间
                long processingTime = endTime - startTime;
                // 打印请求处理时间
                log.info("请求结束 - Request URL: {} 处理时间: {} ms", httpRequest.getRequestURL(), processingTime);
                MDC.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void logRequestInfo(CustomHttpServletRequestWrapper request) throws IOException {
        log.info("请求开始 - Request URL: " + request.getRequestURL());
        log.info("Request Method: " + request.getMethod());
        // 处理 GET 请求的查询参数
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            Map<String, String> queryParams = getQueryParams(request);
            log.info("Request Query Parameters: " + queryParams);
        }
        // 处理 POST 请求的请求体
        else if ("POST".equalsIgnoreCase(request.getMethod())) {
            String requestBody = getRequestBody(request);
            log.info("Request Body: " + requestBody);
        }
    }

    private Map<String, String> getQueryParams(HttpServletRequest request) {
        Map<String, String> queryParams = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            queryParams.put(paramName, paramValue);
        }
        return queryParams;
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        return body.toString();
    }

}