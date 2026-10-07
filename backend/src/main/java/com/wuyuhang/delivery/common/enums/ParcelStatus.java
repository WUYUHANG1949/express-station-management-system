package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 快件状态枚举。
 * <p>
 * 状态机：IN_STORE 在库待取；取件核销 -> PICKED_UP；派送 -> DELIVERING -> PICKED_UP；
 * 登记异常 -> EXCEPTION；退回 -> RETURNED。
 *
 * @author 吴宇航
 */
@Getter
public enum ParcelStatus {

    /** 在库待取 */
    IN_STORE("在库待取"),
    /** 已取件（已签收） */
    PICKED_UP("已取件"),
    /** 派送中 */
    DELIVERING("派送中"),
    /** 异常件 */
    EXCEPTION("异常件"),
    /** 已退回 */
    RETURNED("已退回");

    private final String label;

    ParcelStatus(String label) {
        this.label = label;
    }

    /**
     * 根据状态编码获取中文名称，未知编码原样返回。
     */
    public static String labelOf(String code) {
        if (code == null) {
            return "";
        }
        for (ParcelStatus status : values()) {
            if (status.name().equals(code)) {
                return status.label;
            }
        }
        return code;
    }

    /**
     * 判断是否为合法的状态编码。
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (ParcelStatus status : values()) {
            if (status.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
