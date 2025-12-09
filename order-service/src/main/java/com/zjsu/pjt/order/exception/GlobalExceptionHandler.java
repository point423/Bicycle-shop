package com.zjsu.pjt.order.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常（自定义）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<Void>> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.error(400, e.getMessage()));
    }

    // 资源不存在异常
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response<Void>> handleResourceNotFound(ResourceNotFoundException e) {
        log.error("资源不存在：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Response.error(404, e.getMessage()));
    }

    // 参数校验异常（@Valid）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleValidException(MethodArgumentNotValidException e) {
        // 提取字段错误信息
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.error("参数校验失败：{}", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.error(400, errorMsg));
    }

    // 其他未捕获异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception e) {
        log.error("服务器异常：", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error(500, "Internal server error: " + e.getMessage()));
    }
}