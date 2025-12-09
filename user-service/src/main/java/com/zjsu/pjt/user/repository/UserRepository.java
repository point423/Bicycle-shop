package com.zjsu.pjt.user.repository;

import com.zjsu.pjt.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // 按用户id查询（唯一）
    Optional<User> findByUserId(String userId);



    // 按号码查询（唯一）
    Optional<User> findByPhone(String phone);



    // 按用户名查询（唯一）
    Optional<User> findByUsername(String username);



    // 检查用户id是否已存在
    boolean existsByUserId(String UserId);

    // 检查号码是否已存在
    boolean existsByPhone(String phone);

    // 检查用户名是否已存在
    boolean existsByUsername(String username);
}