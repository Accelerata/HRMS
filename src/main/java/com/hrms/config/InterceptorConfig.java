package com.hrms.config;

import com.hrms.interceptor.JwtTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 拦截器注册配置
 * 独立于 WebMvcConfig，避免循环依赖
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**")               // 拦截所有 API 请求
                .excludePathPatterns(                     // 排除以下路径
                        "/api/v1/auth/login",             // 登录
                        "/api/v1/auth/captcha",           // 验证码
                        "/error"                          // 错误页面
                );
    }

}
