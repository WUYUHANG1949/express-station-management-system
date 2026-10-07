package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色保存请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "角色保存请求")
public class RoleSaveDTO implements Serializable {

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码，如 ADMIN/STAFF/USER")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "排序号")
    private Integer sort = 0;

    @Schema(description = "状态：1 启用 0 停用")
    private Integer status = 1;

    @Schema(description = "权限ID集合，可为空，之后单独分配")
    private List<Long> permIds = new ArrayList<>();
}
