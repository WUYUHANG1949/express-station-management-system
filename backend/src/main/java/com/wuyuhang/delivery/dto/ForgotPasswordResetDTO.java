package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码 —— 校验验证码并重置密码请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "找回密码-重置密码")
public class ForgotPasswordResetDTO implements Serializable {

    @Schema(description = "账号名", example = "staff01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入账号名")
    private String username;

    @Schema(description = "账号绑定的手机号", example = "13800000002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入验证码")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;

    @Schema(description = "新密码", example = "abc123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "密码长度需在 6~20 位之间")
    private String newPassword;
}
