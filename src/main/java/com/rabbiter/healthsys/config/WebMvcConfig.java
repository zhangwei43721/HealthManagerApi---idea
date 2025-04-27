package com.rabbiter.healthsys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类，用于添加静态资源映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 添加资源处理器，配置静态资源映射
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 添加头像图片资源映射
        // /images/avatar/ 是访问头像的URL前缀，后面跟文件名
        // 如访问：http://localhost:端口号/images/avatar/文件名.jpg
        registry.addResourceHandler("/images/avatar/**")
                .addResourceLocations("classpath:/images/avatar/");

        // 保留默认的资源处理器
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
} 