package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 新增 / 编辑用户请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "用户保存请求")
public class UserSaveDTO implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 位")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,19}$",
            message = "用户名需以字母开头，只能包含字母、数字和下划线")
    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "密码：新增时必填；编辑时留空表示不修改")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别：0 未知 1 男 2 女")
    private Integer gender = 0;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "账号状态：1 正常 0 禁用")
    private Integer status = 1;

    @Schema(description = "角色ID集合")
    private List<Long> roleIds = new ArrayList<>();
}
