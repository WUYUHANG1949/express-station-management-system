package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求参数。注册成功后默认分配 USER（普通用户）角色。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "注册请求")
public class RegisterDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,19}$",
            message = "用户名需以字母开头，只能包含字母、数字和下划线")
    @Schema(description = "登录账号", example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    @Schema(description = "密码", example = "123456")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "123456")
    private String confirmPassword;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过 50 个字符")
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13900000099")
    private String phone;
}
