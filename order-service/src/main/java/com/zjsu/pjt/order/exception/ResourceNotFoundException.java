package com.zjsu.pjt.order.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends RuntimeException {

    private final HttpStatus status;

    // 我们修改构造器，让它更通用，只接收一个最终的错误消息
    public ResourceNotFoundException(String message) {
        super(message);
        // 对于“资源未找到”的异常，状态码总是 404
        this.status = HttpStatus.NOT_FOUND;
    }

    // 提供一个 getter 方法，以便异常处理器可以获取状态码
    public HttpStatus getStatus() {
        return status;
    }
}
