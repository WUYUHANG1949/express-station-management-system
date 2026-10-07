package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体，对应表 sys_role。
 *
 * @author 吴宇航
 */
@Data
@TableName("sys_role")
@Schema(description = "角色")
public class SysRole implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色编码：ADMIN/STAFF/USER")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：1 启用 0 停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
