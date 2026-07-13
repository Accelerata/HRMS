package com.hrms.config;

import com.hrms.json.JacksonObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置
 * - 注册自定义 Jackson 序列化器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 替换默认的 Jackson 转换器为自定义实现
     * 统一处理 LocalDateTime/LocalDate/LocalTime 序列化格式
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 移除原有的 MappingJackson2HttpMessageConverter
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new JacksonObjectMapper());
        converters.add(0, converter);  // 放到第一位，优先使用
    }

}
