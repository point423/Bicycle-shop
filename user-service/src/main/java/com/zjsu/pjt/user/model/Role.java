package com.zjsu.pjt.user.model;

/**
 * 用户角色枚举
 * 定义了系统中所有可用的角色
 */
public enum Role {
    /**
     * 普通用户，拥有浏览商品、下单等基本权限
     */
    USER,

    /**
     * 管理员，拥有管理商品、查看所有订单等高级权限
     */
    ADMIN
}
