package com.zjsu.pjt.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // 使用UUID作为主键
    private UUID id;


    @NotBlank(message = "用户id不能为空")
    @Column(unique = true, nullable = false)
    private String userId;


    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50之间")
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少为6位")
    private String password;


    @NotBlank(message = "号码不能为空")
    @Column(unique = true, nullable = false)
    private String phone;


    @Min(value = 0, message = "年龄不能为负数")
    @NotNull(message = "年龄不能为空")
    private Integer age;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 系统生成创建时间
}
