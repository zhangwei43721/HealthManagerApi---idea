package com.rabbiter.healthsys.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Web 配置类，用于添加静态资源映射和解决中文乱码问题
 */
@Configuration
@Slf4j
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
    
    /**
     * 添加拦截器，确保所有请求参数以UTF-8编码处理
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                try {
                    request.setCharacterEncoding("UTF-8");
                    response.setCharacterEncoding("UTF-8");
                } catch (Exception e) {
                    log.error("设置请求/响应编码失败：{}", e.getMessage(), e);
                }
                return true; // 继续处理请求
            }
        }).addPathPatterns("/**"); // 拦截所有请求
    }
} 