package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限实体，对应表 sys_permission。
 * <p>
 * 同时承载"菜单"和"按钮"两种粒度：permType = MENU 用于渲染侧边栏，
 * permType = BUTTON 用于前端按钮显隐与后端接口鉴权。
 *
 * @author 吴宇航
 */
@Data
@TableName("sys_permission")
@Schema(description = "权限（菜单/按钮）")
public class SysPermission implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父权限ID，0 表示顶级")
    private Long parentId;

    @Schema(description = "权限标识，如 parcel:create")
    private String permCode;

    @Schema(description = "权限名称")
    private String permName;

    @Schema(description = "类型：MENU 菜单 BUTTON 按钮")
    private String permType;

    @Schema(description = "前端路由地址")
    private String path;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态：1 启用 0 停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 子权限，构建菜单树时使用，不是数据库字段 */
    @TableField(exist = false)
    @Schema(description = "子权限")
    private List<SysPermission> children = new ArrayList<>();
}
