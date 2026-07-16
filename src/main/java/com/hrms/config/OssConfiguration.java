package com.hrms.config;

import com.hrms.properties.AliOssProperties;
import com.hrms.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 配置：装配 AliOssUtil 为 Spring Bean
 */
@Configuration
@RequiredArgsConstructor
public class OssConfiguration {

    private final AliOssProperties aliOssProperties;

    @Bean
    public AliOssUtil aliOssUtil() {
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
