package com.hrms.config;

import com.hrms.interceptor.JwtTokenInterceptor;
import com.hrms.interceptor.SessionTimeoutInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 拦截器注册配置
 * 注册顺序即执行顺序：
 * 1. JwtTokenInterceptor — 校验 Token + 写入 BaseContext
 * 2. SessionTimeoutInterceptor — 检查会话超时 + 刷新 TTL
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtTokenInterceptor jwtTokenInterceptor;

    @Autowired(required = false)
    private SessionTimeoutInterceptor sessionTimeoutInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/captcha",
                        "/error"
                );

        // Session timeout interceptor runs AFTER JWT (needs BaseContext populated)
        if (sessionTimeoutInterceptor != null) {
            registry.addInterceptor(sessionTimeoutInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns(
                            "/api/v1/auth/login",
                            "/api/v1/auth/captcha",
                            "/error"
                    );
        }
    }

}
