package com.hrms.crypto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化 MyBatis TypeHandler 的静态 EncryptionUtil 引用
 * 因为 MyBatis 通过无参构造创建 TypeHandler，无法依赖注入
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TypeHandlerInitializer {

    private final EncryptionUtil encryptionUtil;

    @PostConstruct
    public void init() {
        EncryptedStringTypeHandler.setEncryptionUtil(encryptionUtil);
        EncryptedBigDecimalTypeHandler.setEncryptionUtil(encryptionUtil);
        log.info("TypeHandler 加密工具注入完成");
    }
}
