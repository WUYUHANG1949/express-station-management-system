package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户账号统计 VO。
 * <p>
 * 用户管理页顶部的统计卡片使用。各计数由一条 SQL 的多个标量子查询一次取回，
 * 避免前端为每个指标单独发一次分页请求（那是 6 次往返）。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "用户账号统计")
public class UserStatVO implements Serializable {

    @Schema(description = "账号总数")
    private Integer total = 0;

    @Schema(description = "系统管理员人数")
    private Integer adminCount = 0;

    @Schema(description = "驿站员工人数")
    private Integer staffCount = 0;

    @Schema(description = "普通用户人数")
    private Integer userCount = 0;

    @Schema(description = "启用账号数")
    private Integer enabledCount = 0;

    @Schema(description = "禁用账号数")
    private Integer disabledCount = 0;

    @Schema(description = "未绑定驿站的账号数")
    private Integer noStationCount = 0;
}
