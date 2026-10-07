package com.wuyuhang.delivery.common.enums;

import lombok.Getter;

/**
 * 快件轨迹操作类型枚举。
 *
 * @author 吴宇航
 */
@Getter
public enum OperateType {

    /** 入库登记 */
    IN_STORE("入库登记"),
    /** 取件核销 */
    PICKUP("取件核销"),
    /** 派送 */
    DELIVER("派送"),
    /** 登记异常 */
    EXCEPTION("登记异常"),
    /** 异常处理 */
    EXCEPTION_HANDLE("异常处理"),
    /** 退回 */
    RETURN("退回"),
    /** 转移货位 */
    TRANSFER("转移货位"),
    /** 信息修改 */
    EDIT("信息修改");

    private final String label;

    OperateType(String label) {
        this.label = label;
    }
}
