package com.zjsu.pjt.user.controller;

import com.zjsu.pjt.user.exception.BusinessException;
import com.zjsu.pjt.user.model.User;
import com.zjsu.pjt.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 用户管理API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理模块", description = "提供用户管理的创建、查询、更新、删除操作")
public class UserController {
    private final UserService UserService;

    // 新增用户
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User User) {
        return ResponseEntity.status(201).body(UserService.createUser(User));
    }


    //获取所有用户
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(UserService.getAllUsers());
    }


    // 按ID查询用户
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(UserService.getUserById(id));
    }


    // 按用户id查询用户
    @GetMapping("/UserId/{UserId}")
    public ResponseEntity<User> getUserByUserId(@PathVariable String UserId) {
        return ResponseEntity.ok(UserService.getUserByUserId(UserId));
    }

    //按用户名查询用户
    @GetMapping("/Username/{Username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String Username) {
        return ResponseEntity.ok(UserService.getUserByUsername(Username));
    }

    //按号码查询用户
    @GetMapping("/Phone/{Phone}")
    public ResponseEntity<User> getUserByPhone(@PathVariable String Phone) {
        return ResponseEntity.ok(UserService.getUserByPhone(Phone));
    }

    // 更新用户
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable UUID id, @RequestBody User User) {
        return ResponseEntity.ok(UserService.updateUser(id, User));
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        UserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}