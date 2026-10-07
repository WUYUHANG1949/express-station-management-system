package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 系统角色编码枚举。
 *
 * @author 吴宇航
 */
@Getter
public enum RoleCode {

    /** 系统管理员：拥有全部权限 */
    ADMIN("系统管理员"),
    /** 驿站员工：负责日常收件、取件、寄件、异常件业务 */
    STAFF("驿站员工"),
    /** 普通用户：收件人，只能查询本人名下快件 */
    USER("普通用户");

    private final String label;

    RoleCode(String label) {
        this.label = label;
    }

    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (RoleCode roleCode : values()) {
            if (roleCode.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
