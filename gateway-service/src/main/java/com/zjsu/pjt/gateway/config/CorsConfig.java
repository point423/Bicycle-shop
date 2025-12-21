package com.zjsu.pjt.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 2. 配置允许的来源
        config.addAllowedOrigin("*"); // 允许所有来源。在生产环境中应配置为你的前端域名，例如 "http://yourdomain.com"

        // 3. 配置允许的请求头
        config.addAllowedHeader("*"); // 允许所有请求头，包括 Authorization

        // 4. 配置允许的HTTP方法
        config.addAllowedMethod("*"); // 允许所有方法，包括 GET, POST, PUT, DELETE, OPTIONS

        // 5. 是否允许携带 Cookie 等凭证
        // config.setAllowCredentials(true); // 如果需要，可以开启，但 allowedOrigins 不能为 "*"

        // 6. 创建 CORS 配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());

        // 7. 为所有路径应用这个CORS配置
        source.registerCorsConfiguration("/**", config);

        // 8. 返回 CorsWebFilter 实例
        return new CorsWebFilter(source);
    }
}