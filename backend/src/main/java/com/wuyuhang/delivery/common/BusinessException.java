package com.wuyuhang.delivery.common;

import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * Service 层校验不通过时直接抛出，由 {@link GlobalExceptionHandler} 统一捕获，
 * 转换成 {@code code = 1001} 的响应，{@code message} 可直接提示给前端用户。
 *
 * @author 吴宇航
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
