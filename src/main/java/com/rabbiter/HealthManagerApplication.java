package com.rabbiter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("com.rabbiter.*.mapper")
public class HealthManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthManagerApplication.class, args);
    }
}
