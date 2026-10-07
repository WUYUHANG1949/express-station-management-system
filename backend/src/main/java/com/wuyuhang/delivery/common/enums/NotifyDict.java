package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 通知相关枚举集合。
 * <p>
 * 包含通知类型、发送渠道、发送结果三类字典，集中在一个类中便于维护，
 * 并为前端提供统一的中文名称映射（接口返回 xxxName 字段）。
 *
 * @author 吴宇航
 */
public final class NotifyDict {

    private NotifyDict() {
    }

    /**
     * 通知类型。
     */
    @Getter
    public enum NotifyType {

        /** 到件通知：快件入库后告知收件人取件码 */
        IN_STORE("到件通知"),
        /** 逾期催取：超过免费保管期后提醒尽快取件 */
        OVERDUE("逾期催取"),
        /** 取件确认：核销完成后告知已取件 */
        PICKUP_DONE("取件确认"),
        /** 异常通知：件出现破损、地址不详等问题时告知 */
        EXCEPTION("异常通知");

        private final String label;

        NotifyType(String label) {
            this.label = label;
        }

        public static String labelOf(String code) {
            if (code == null) {
                return "";
            }
            for (NotifyType type : values()) {
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
            for (NotifyType type : values()) {
                if (type.name().equals(code)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 通知渠道。
     */
    @Getter
    public enum Channel {

        /** 短信（真实项目对接短信网关，本项目以记录形式模拟） */
        SMS("短信"),
        /** 站内通知 */
        APP("站内通知"),
        /** 电话通知 */
        PHONE("电话");

        private final String label;

        Channel(String label) {
            this.label = label;
        }

        public static String labelOf(String code) {
            if (code == null) {
                return "";
            }
            for (Channel channel : values()) {
                if (channel.name().equals(code)) {
                    return channel.label;
                }
            }
            return code;
        }

        public static boolean isValid(String code) {
            if (code == null) {
                return false;
            }
            for (Channel channel : values()) {
                if (channel.name().equals(code)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 发送结果。
     */
    @Getter
    public enum SendStatus {

        /** 发送成功 */
        SUCCESS("发送成功"),
        /** 发送失败 */
        FAILED("发送失败");

        private final String label;

        SendStatus(String label) {
            this.label = label;
        }

        public static String labelOf(String code) {
            if (code == null) {
                return "";
            }
            for (SendStatus status : values()) {
                if (status.name().equals(code)) {
                    return status.label;
                }
            }
            return code;
        }
    }
}
