package com.zjsu.pjt.inventory.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {
    // 状态码（200=成功，400=参数错误，404=资源不存在，500=服务器错误）
    private int code;
    // 提示消息
    private String message;
    // 响应数据（成功时返回，错误时为null）
    private T data;

    // 成功响应（带数据）
    public static <T> Response<T> success(T data) {
        return new Response<>(200, "Success", data);
    }

    // 成功响应（无数据，如删除成功）
    public static <T> Response<T> success() {
        return new Response<>(200, "Success", null);
    }

    // 成功响应（自定义消息，如创建成功）
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(201, message, data);
    }

    // 错误响应
    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, message, null);
    }
}
