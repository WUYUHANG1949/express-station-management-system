package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 寄件单状态枚举。
 * <p>
 * 状态机：PENDING 待揽收 -> ACCEPTED 已揽收 -> SHIPPED 已发出；未发出前可 CANCELLED 已取消。
 *
 * @author 吴宇航
 */
@Getter
public enum ShipStatus {

    /** 待揽收 */
    PENDING("待揽收"),
    /** 已揽收 */
    ACCEPTED("已揽收"),
    /** 已发出 */
    SHIPPED("已发出"),
    /** 已取消 */
    CANCELLED("已取消");

    private final String label;

    ShipStatus(String label) {
        this.label = label;
    }

    public static String labelOf(String code) {
        if (code == null) {
            return "";
        }
        for (ShipStatus status : values()) {
            if (status.name().equals(code)) {
                return status.label;
            }
        }
        return code;
    }

    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (ShipStatus status : values()) {
            if (status.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
