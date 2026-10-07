package com.wuyuhang.delivery.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户（存放于 JWT 载荷与 ThreadLocal 中）。
 * <p>
 * 角色与权限随令牌一起下发，因此鉴权过程**不需要再查数据库**；
 * 代价是权限调整后需要用户重新登录才能生效，这一点已在文档中说明。
 *
 * @author 吴宇航
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录用户上下文")
public class LoginUser implements Serializable {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "角色编码集合")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Schema(description = "权限标识集合")
    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    /**
     * 是否拥有指定权限。
     */
    public boolean hasPermission(String permission) {
        return permission != null && permissions != null && permissions.contains(permission);
    }

    /**
     * 是否拥有指定角色。
     */
    public boolean hasRole(String roleCode) {
        return roleCode != null && roles != null && roles.contains(roleCode);
    }

    /**
     * 是否为系统管理员。
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
