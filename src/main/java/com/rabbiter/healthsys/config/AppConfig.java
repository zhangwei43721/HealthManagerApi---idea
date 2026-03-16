package com.rabbiter.healthsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 应用配置类，用于定义 Bean。
 */
@Configuration
public class AppConfig {

    /**
     * 定义 RestTemplate Bean，用于发送 HTTP 请求。
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 