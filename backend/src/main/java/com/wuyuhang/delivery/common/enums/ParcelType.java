package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 快件类型枚举。
 *
 * @author 吴宇航
 */
@Getter
public enum ParcelType {

    /** 普通件 */
    NORMAL("普通件"),
    /** 小件 */
    SMALL("小件"),
    /** 大件 */
    LARGE("大件"),
    /** 易碎品 */
    FRAGILE("易碎品"),
    /** 文件 */
    DOCUMENT("文件"),
    /** 生鲜 */
    COLD("生鲜");

    private final String label;

    ParcelType(String label) {
        this.label = label;
    }

    public static String labelOf(String code) {
        if (code == null) {
            return "";
        }
        for (ParcelType type : values()) {
            if (type.name().equals(code)) {
                return type.label;
            }
        }
        return code;
    }
}
