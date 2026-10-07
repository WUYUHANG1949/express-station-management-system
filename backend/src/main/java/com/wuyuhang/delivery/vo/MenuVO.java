package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 前端菜单项 VO，由权限表组装成树后返回给前端动态渲染侧边栏。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "菜单项")
public class MenuVO implements Serializable {

    @Schema(description = "菜单ID（即权限ID）")
    private Long id;

    @Schema(description = "权限标识")
    private String permCode;

    @Schema(description = "菜单名称")
    private String permName;

    @Schema(description = "前端路由地址")
    private String path;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "子菜单")
    private List<MenuVO> children = new ArrayList<>();
}
