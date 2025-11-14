package com.feijimiao.xianyuassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 支持 Vue Router 的 History 模式
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置视图控制器，支持 SPA 路由
     * 当访问前端路由时，返回 index.html，由 Vue Router 处理路由
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 匹配所有非 API 路径，转发到 index.html
        // 这样可以支持 Vue Router 的 History 模式
        registry.addViewController("/{spring:\\w+}")
                .setViewName("forward:/index.html");
        registry.addViewController("/{spring:\\w+}/**")
                .setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}/**")
                .setViewName("forward:/index.html");
    }
}
