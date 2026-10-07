package com.wuyuhang.delivery.common;

import lombok.Getter;

/**
 * 统一响应状态码。
 * <p>
 * 约定：200 成功；4xx 与 HTTP 语义保持一致；1001 表示可提示给用户的业务异常。
 *
 * @author 吴宇航
 */
@Getter
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 请求参数校验不通过 */
    PARAM_ERROR(400, "请求参数不合法"),
    /** 未登录 / 令牌失效 */
    UNAUTHORIZED(401, "登录已过期，请重新登录"),
    /** 已登录但无权限 */
    FORBIDDEN(403, "无操作权限，请联系管理员"),
    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),
    /** 服务器内部错误 */
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试"),
    /** 业务异常（可直接展示给用户的提示） */
    BUSINESS_ERROR(1001, "业务处理失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
