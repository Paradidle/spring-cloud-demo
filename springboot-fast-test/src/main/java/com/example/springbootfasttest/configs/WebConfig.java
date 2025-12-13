package com.example.springbootfasttest.configs;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * CreateDate:2025/12/13
 * </p>
 *
 * @author chenyupeng
 * @history Mender:chenyupeng；Date:2025/12/13；
 */

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加JAXB2消息转换器，用于XML到Java对象的转换
        converters.add(new Jaxb2RootElementHttpMessageConverter());
    }
}
