package com.wuyuhang.delivery.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 统一把各类异常转换成 {@link Result}，避免异常堆栈直接暴露给前端，
 * 同时保证前端拿到的永远是同一种数据结构。
 *
 * @author 吴宇航
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：可预期的、需要提示给用户的错误。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{} {}]：{}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * @Valid 校验失败（RequestBody 方式）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.PARAM_ERROR, message.isEmpty() ? ResultCode.PARAM_ERROR.getMessage() : message);
    }

    /**
     * @Valid 校验失败（表单绑定方式）。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败：{}", message);
        return Result.error(ResultCode.PARAM_ERROR, message.isEmpty() ? ResultCode.PARAM_ERROR.getMessage() : message);
    }

    /**
     * 缺少必填请求参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.PARAM_ERROR, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配，例如 id 传了非数字。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(ResultCode.PARAM_ERROR, "参数格式不正确：" + e.getName());
    }

    /**
     * 请求体无法解析。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_ERROR, "请求体格式不正确，请检查提交的 JSON 数据");
    }

    /**
     * 请求方法不支持。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(ResultCode.PARAM_ERROR, "不支持的请求方法：" + e.getMethod());
    }

    /**
     * 接口不存在。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        return Result.error(ResultCode.NOT_FOUND, "接口不存在：" + e.getRequestURL());
    }

    /**
     * 唯一索引冲突，例如重复运单号、重复用户名。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.error("唯一约束冲突", e);
        return Result.error("数据已存在，请勿重复提交");
    }

    /**
     * 兜底异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}
