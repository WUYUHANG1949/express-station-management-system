package com.wuyuhang.delivery.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询条件。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "用户查询条件")
public class UserQuery implements Serializable {

    @Schema(description = "登录账号（模糊匹配）")
    private String username;

    @Schema(description = "真实姓名（模糊匹配）")
    private String realName;

    @Schema(description = "手机号（模糊匹配）")
    private String phone;

    @Schema(description = "账号状态：1 正常 0 禁用")
    private Integer status;

    @Schema(description = "角色编码：ADMIN/STAFF/USER")
    private String roleCode;

    @Schema(description = "所属驿站ID")
    private Long stationId;
}
