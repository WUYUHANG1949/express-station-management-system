package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 取件方式与核验方式枚举。
 *
 * @author 吴宇航
 */
public final class PickupDict {

    private PickupDict() {
    }

    /**
     * 取件方式。
     */
    @Getter
    public enum PickupType {

        /** 本人自取 */
        SELF("本人自取"),
        /** 他人代取 */
        AGENT("他人代取"),
        /** 送货上门 */
        DELIVERY("送货上门");

        private final String label;

        PickupType(String label) {
            this.label = label;
        }

        public static String labelOf(String code) {
            if (code == null) {
                return "";
            }
            for (PickupType type : values()) {
                if (type.name().equals(code)) {
                    return type.label;
                }
            }
            return code;
        }

        public static boolean isValid(String code) {
            if (code == null) {
                return false;
            }
            for (PickupType type : values()) {
                if (type.name().equals(code)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 核验方式。
     */
    @Getter
    public enum VerifyType {

        /** 取件码核验 */
        CODE("取件码核验"),
        /** 身份证核验 */
        ID_CARD("身份证核验"),
        /** 手机号核验 */
        PHONE("手机号核验");

        private final String label;

        VerifyType(String label) {
            this.label = label;
        }

        public static String labelOf(String code) {
            if (code == null) {
                return "";
            }
            for (VerifyType type : values()) {
                if (type.name().equals(code)) {
                    return type.label;
                }
            }
            return code;
        }

        public static boolean isValid(String code) {
            if (code == null) {
                return false;
            }
            for (VerifyType type : values()) {
                if (type.name().equals(code)) {
                    return true;
                }
            }
            return false;
        }
    }
}
