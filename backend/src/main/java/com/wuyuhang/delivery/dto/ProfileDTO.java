package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改本人资料请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "修改个人资料请求")
public class ProfileDTO implements Serializable {

    @Size(max = 50, message = "真实姓名长度不能超过 50 个字符")
    @Schema(description = "真实姓名")
    private String realName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别：0 未知 1 男 2 女")
    private Integer gender;

    @Schema(description = "头像地址")
    private String avatar;
}
