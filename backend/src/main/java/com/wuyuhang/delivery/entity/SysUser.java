package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体，对应表 sys_user。
 * <p>
 * 三类角色（管理员 / 驿站员工 / 普通用户）共用一张用户表，通过 sys_user_role 关联角色。
 *
 * @author 吴宇航
 */
@Data
@TableName("sys_user")
@Schema(description = "用户")
public class SysUser implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    /** 密码使用 BCrypt 加密存储，序列化时永远不返回给前端 */
    @JsonIgnore
    @Schema(description = "密码（BCrypt 加密）", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别：0 未知 1 男 2 女")
    private Integer gender;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "账号状态：1 正常 0 禁用")
    private Integer status;

    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 该用户所属驿站名称，来自 station 表关联查询，不是数据库字段 */
    @TableField(exist = false)
    @Schema(description = "所属驿站名称")
    private String stationName;
}
