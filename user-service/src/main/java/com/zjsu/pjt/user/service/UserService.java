package com.zjsu.pjt.user.service;

import com.zjsu.pjt.user.exception.BusinessException;
import com.zjsu.pjt.user.model.User;
import com.zjsu.pjt.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 用户业务逻辑
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository UserRepository;

    //获取所有用户列表
    public List<User> getAllUsers() {
        return UserRepository.findAll();
    }

    
    /**
     * 新增用户（校验用户名，用户id，号码唯一性）
     */
    @Transactional
    public User createUser(User User) {
        // 校验用户id唯一性
        if (UserRepository.existsByUserId(User.getUserId())) {
            throw new BusinessException("用户id已存在：" + User.getUserId(), HttpStatus.CONFLICT);
        }
        // 校验号码唯一性
        if (UserRepository.existsByPhone(User.getPhone())) {
            throw new BusinessException("号码已存在：" + User.getPhone(), HttpStatus.CONFLICT);
        }
        // 校验用户名唯一性
        if (UserRepository.existsByUserId(User.getUsername())) {
            throw new BusinessException("用户名已存在：" + User.getUsername(), HttpStatus.CONFLICT);
        }
        return UserRepository.save(User);
    }

    /**
     * 按id查询用户
     */
    public User getUserById(UUID id) {
        return UserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在：ID=" + id, HttpStatus.NOT_FOUND));
    }

    /**
     * 按用户id查询用户
     */
    public User getUserByUserId(String UserId) {
        return UserRepository.findByUserId(UserId)
                .orElseThrow(() -> new BusinessException("用户不存在：用户id=" + UserId, HttpStatus.NOT_FOUND));
    }

    /**
     * 按用户名查询用户
     */
    public User getUserByUsername(String Username) {
        return UserRepository.findByUsername(Username)
                .orElseThrow(() -> new BusinessException("用户不存在：用户名=" + Username, HttpStatus.NOT_FOUND));
    }

    /**
     * 按号码查询用户
     */
    public User getUserByPhone(String Phone) {
        return UserRepository.findByPhone(Phone)
                .orElseThrow(() -> new BusinessException("用户不存在：号码=" + Phone, HttpStatus.NOT_FOUND));
    }


    /**
     * 更新用户信息（不允许修改用户id）
     */
    @Transactional
    public User updateUser(UUID id, User updateUser) {
        User existingUser = getUserById(id);
        // 禁止修改用户id
        if (!existingUser.getUserId().equals(updateUser.getUserId())) {
            throw new BusinessException("不允许修改用户id", HttpStatus.BAD_REQUEST);
        }
        // 更新允许修改的字段
        existingUser.setUsername(updateUser.getUsername());
        existingUser.setPhone(updateUser.getPhone());
        existingUser.setAge(updateUser.getAge());
        existingUser.setPassword(updateUser.getPassword());
        return UserRepository.save(existingUser);
    }

    /**
     * 删除用户（校验是否有关联订单记录）
     */
    @Transactional
    public void deleteUser(UUID id) {
        User User = getUserById(id);
   // TODO:    // 校验是否有关联选课记录
//        if (enrollmentService.hasEnrollmentsForUser(id)) {
//            throw new BusinessException("该学生存在选课记录，无法删除", HttpStatus.CONFLICT);
//        }
        UserRepository.delete(User);
    }
}
