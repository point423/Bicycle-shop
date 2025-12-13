package com.zjsu.pjt.user.controller;

import com.zjsu.pjt.user.model.User; // 1. 修正：导入正确的 User 类
import com.zjsu.pjt.user.service.UserService;
import com.zjsu.pjt.user.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService UserService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 使用 getUserByUsername 进行查找，这在新的 Repository 中是支持的
        User User = UserService.getUserByUsername(request.getUsername());


        if (User == null || !User.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }

        // Token 生成和响应都保持不变，因为我们操作的仍然是 User 对象
        String token = jwtUtil.generateToken(User.getId(), User.getUsername(), User.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token, User));
    }

    /**
     * 登录请求的数据传输对象 (DTO)
     */
    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 登录响应的数据传输对象 (DTO)
     */
    @Data
    @AllArgsConstructor
    static class LoginResponse {
        private String token;
        // 5. 修正：字段类型应为 User
        private User User;
    }
}
