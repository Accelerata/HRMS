package com.hrms.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置
 * 扫描 Mapper 接口
 */
@Configuration
@MapperScan("com.hrms.mapper")
public class MyBatisConfig {
}
