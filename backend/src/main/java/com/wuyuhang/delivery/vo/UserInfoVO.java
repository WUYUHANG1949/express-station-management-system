package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户信息 VO，用于前端保存用户状态、渲染菜单与按钮权限。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "登录用户信息")
public class UserInfoVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "所属驿站名称")
    private String stationName;

    @Schema(description = "角色编码集合，如 [\"ADMIN\"]")
    private List<String> roles = new ArrayList<>();

    @Schema(description = "权限标识集合")
    private List<String> permissions = new ArrayList<>();
}
