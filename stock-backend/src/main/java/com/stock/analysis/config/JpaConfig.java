package com.stock.analysis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.stock.analysis.repository")
@EnableTransactionManagement
public class JpaConfig {
    // JPA配置类 - 启用JPA Repository扫描和事务管理
}