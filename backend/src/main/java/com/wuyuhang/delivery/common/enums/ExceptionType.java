package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 异常件类型枚举。
 *
 * @author 吴宇航
 */
@Getter
public enum ExceptionType {

    /** 破损 */
    DAMAGED("破损"),
    /** 丢失 */
    LOST("丢失"),
    /** 地址错误 */
    ADDRESS_ERROR("地址错误"),
    /** 拒收 */
    REFUSED("拒收"),
    /** 长期未取 */
    TIMEOUT("长期未取"),
    /** 其他 */
    OTHER("其他");

    private final String label;

    ExceptionType(String label) {
        this.label = label;
    }

    public static String labelOf(String code) {
        if (code == null) {
            return "";
        }
        for (ExceptionType type : values()) {
            if (type.name().equals(code)) {
                return type.label;
            }
        }
        return code;
    }

    /**
     * 判断是否为合法的异常类型编码。
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        for (ExceptionType type : values()) {
            if (type.name().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
