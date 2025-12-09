// BusinessException.java（业务异常，如重复选购、容量不足）
package com.zjsu.pjt.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;  // HTTP状态码

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
