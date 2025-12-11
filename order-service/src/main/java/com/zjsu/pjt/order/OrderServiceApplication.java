package com.zjsu.pjt.order;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;

/**
 * 项目启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {
    @Bean
    @LoadBalanced // <--- 【魔法就在这里！】这个注解赋予了RestTemplate负载均衡的能力
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}