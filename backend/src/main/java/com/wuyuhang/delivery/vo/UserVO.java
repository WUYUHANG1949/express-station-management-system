package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理列表 VO。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别：0 未知 1 男 2 女")
    private Integer gender;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "所属驿站名称")
    private String stationName;

    @Schema(description = "账号状态：1 正常 0 禁用")
    private Integer status;

    @Schema(description = "角色ID集合")
    private List<Long> roleIds = new ArrayList<>();

    @Schema(description = "角色编码集合")
    private List<String> roleCodes = new ArrayList<>();

    @Schema(description = "角色名称集合")
    private List<String> roleNames = new ArrayList<>();

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
