package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码 —— 申请验证码请求参数。
 * <p>
 * 必须同时提供账号名与手机号，且两者都要与数据库记录匹配才会发码。
 * 只凭其中一项即可重置密码的设计是不可接受的（知道用户名就能改别人密码）。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "找回密码-申请验证码")
public class ForgotPasswordSendDTO implements Serializable {

    @Schema(description = "账号名", example = "staff01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入账号名")
    private String username;

    @Schema(description = "账号绑定的手机号", example = "13800000002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
