package com.rabbiter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 * 
 * @author Skyforever
 * @since 2025-05-01
 */
@SpringBootApplication
@MapperScan("com.rabbiter.*.mapper")
public class HealthManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthManagerApplication.class, args);
    }
}
