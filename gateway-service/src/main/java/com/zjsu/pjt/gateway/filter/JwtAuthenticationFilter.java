package com.zjsu.pjt.gateway.filter;

import com.zjsu.pjt.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod; // 1. 引入 HttpMethod
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // ✨ 【修改点 1】: 定义更精确的白名单结构 (不再使用)
    // 我们将把这个逻辑直接写在 isWhiteListed 方法中，使其更清晰

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest(); // 获取request对象
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod(); // 获取HTTP方法

        // ✨ 【修改点 2】: 调用新的白名单检查方法，同时传入路径和方法
        if (isWhiteListed(path, method)) {
            log.debug("请求 [{} {}] 匹配白名单，直接放行。", method, path);
            return chain.filter(exchange);
        }

        // 2. 从请求头获取Token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("路径 {} 需要认证，但缺少或格式错误的Authorization请求头", path);
            return handleUnauthorized(exchange, "缺少认证信息");
        }

        String token = authHeader.substring(7);

        // 3. 验证Token有效性
        if (!jwtUtil.validateToken(token)) {
            log.warn("路径 {} 提供了无效的JWT Token", path);
            return handleUnauthorized(exchange, "无效的认证凭证");
        }

        // 4. 解析Token，获取用户信息
        Claims claims = jwtUtil.parseToken(token);
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 5. 角色权限校验
        if (pathMatcher.match("/api/admin/**", path)) {
            log.debug("检测到管理员路径访问: {}", path);
            if (!"ADMIN".equals(role)) {
                log.warn("用户 '{}' (ID: {}) 尝试访问管理员路径 {}，但其角色为 '{}'，权限不足。", username, userId, path, role);
                return handleForbidden(exchange, "权限不足，需要管理员角色");
            }
            log.info("管理员 '{}' 访问授权路径 {} 验证通过。", username, path);
        }

        // 6. 将用户信息添加到请求头，转发给下游服务
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", Objects.toString(role, ""))
                .build();

        ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();

        return chain.filter(modifiedExchange);
    }

    /**
     * ✨ 【修改点 3】: 重写白名单检查方法，实现 "路径 + 方法" 的精确匹配
     * 检查请求是否匹配白名单规则
     * @param path 请求路径
     * @param method 请求的HTTP方法
     * @return 如果匹配白名单则返回 true
     */
    private boolean isWhiteListed(String path, HttpMethod method) {
        // 规则1: 登录接口，POST方法
        if (pathMatcher.match("/api/auth/login", path) && method == HttpMethod.POST) {
            return true;
        }
        // 规则2: 用户注册接口，POST方法
        if (pathMatcher.match("/api/users", path) && method == HttpMethod.POST) {
            return true;
        }
        // 规则3: 创建商品接口，POST方法
        if (pathMatcher.match("/api/products", path) && method == HttpMethod.POST) {
            return true;
        }
        // 规则4: 允许任何人查看商品列表，GET方法
        if (pathMatcher.match("/api/products", path) && method == HttpMethod.GET) {
            return true;
        }
// 规则5: 允许任何人获取图片资源，GET方法
        if (pathMatcher.match("/images/**", path) && method == HttpMethod.GET) {
            return true;
        }

        // 如果以上规则都不匹配，则不在白名单内
        return false;
    }


    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        log.warn(message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
        log.warn(message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
