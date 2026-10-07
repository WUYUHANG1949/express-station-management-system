package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 异常件处理状态枚举。
 *
 * @author 吴宇航
 */
@Getter
public enum HandleStatus {

    /** 待处理 */
    PENDING("待处理"),
    /** 处理中 */
    HANDLING("处理中"),
    /** 已解决 */
    RESOLVED("已解决");

    private final String label;

    HandleStatus(String label) {
        this.label = label;
    }

    public static String labelOf(String code) {
        if (code == null) {
            return "";
        }
        for (HandleStatus status : values()) {
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
        for (HandleStatus status : values()) {
            if (status.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
