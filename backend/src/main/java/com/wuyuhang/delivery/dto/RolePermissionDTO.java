package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色权限分配请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "角色权限分配请求")
public class RolePermissionDTO implements Serializable {

    @Schema(description = "权限ID集合")
    private List<Long> permIds = new ArrayList<>();
}
