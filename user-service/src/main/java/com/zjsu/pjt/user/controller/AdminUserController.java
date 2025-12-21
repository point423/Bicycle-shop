package com.zjsu.pjt.user.controller;

import com.zjsu.pjt.user.model.User;
import com.zjsu.pjt.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users") // 使用 /api/admin 前缀
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    /**
     * 管理员获取所有用户列表
     * @return 用户列表
     */
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {


        List<User> users = userRepository.findAll();



        return ResponseEntity.ok(users);
    }

    /**
     * 管理员根据ID删除指定用户
     * @param id 要删除的用户ID
     * @return 204 No Content 表示成功
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {


        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }}