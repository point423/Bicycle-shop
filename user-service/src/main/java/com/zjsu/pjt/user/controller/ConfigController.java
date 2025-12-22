package com.zjsu.pjt.user.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;



import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RefreshScope // 关键注解：当 Nacos 配置变更时，Spring 会刷新这个 Bean 中的 @Value 属性
public class ConfigController {
    @Autowired
    private Environment env;

    @Value("${business.feature.new-algorithm-enabled:false}") // 冒号后面是默认值
    private Boolean newAlgorithmEnabled;

    @Value("${business.max-page-size:20}")
    private Integer maxPageSize;

    @GetMapping("/env")
    public String getEnv() {
        return env.getProperty("business.feature.new-algorithm-enabled");
    }

    @GetMapping("/current")
    public Map<String, Object> getCurrentConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("newAlgorithmEnabled", newAlgorithmEnabled);
        config.put("maxPageSize", maxPageSize);
        // 为了方便确认是哪个环境，可以打印一下
        System.out.println("Current Config Request: " + config);
        return config;
    }
}