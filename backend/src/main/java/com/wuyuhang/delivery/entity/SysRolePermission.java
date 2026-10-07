package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-权限关联实体，对应表 sys_role_permission。
 *
 * @author 吴宇航
 */
@Data
@TableName("sys_role_permission")
@Schema(description = "角色权限关联")
public class SysRolePermission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "权限ID")
    private Long permId;
}
