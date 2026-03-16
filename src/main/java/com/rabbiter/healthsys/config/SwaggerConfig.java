package com.rabbiter.healthsys.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人健康管理系统接口文档")
                        .description("个人健康管理系统")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Skyforever")
                        )
                );
    }
}
