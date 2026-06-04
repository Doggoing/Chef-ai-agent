package com.wu.aiagent.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 */
@Configuration
@MapperScan("com.wu.aiagent.mapper")
public class MybatisPlusConfig {
}
