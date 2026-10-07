package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 寄件登记实体，对应表 ship_order。
 * <p>
 * 与收件（parcel）相反，寄件是"从驿站发出"，因此单独建表，
 * 状态流转：PENDING(待揽收) -> ACCEPTED(已揽收) -> SHIPPED(已发出)，任何阶段可 CANCELLED(已取消)。
 *
 * @author 吴宇航
 */
@Data
@TableName("ship_order")
@Schema(description = "寄件单")
public class ShipOrder implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "寄件单ID")
    private Long id;

    @Schema(description = "寄件单号")
    private String orderNo;

    @Schema(description = "受理驿站ID")
    private Long stationId;

    @Schema(description = "快递公司")
    private String expressCompany;

    @Schema(description = "寄件人姓名")
    private String senderName;

    @Schema(description = "寄件人手机号")
    private String senderPhone;

    @Schema(description = "寄件人地址")
    private String senderAddress;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人手机号")
    private String receiverPhone;

    @Schema(description = "收件人地址")
    private String receiverAddress;

    @Schema(description = "快件类型")
    private String parcelType;

    @Schema(description = "重量(kg)")
    private BigDecimal weight;

    @Schema(description = "运费(元)")
    private BigDecimal freight;

    @Schema(description = "保价金额(元)")
    private BigDecimal insuredValue;

    @Schema(description = "状态：PENDING/ACCEPTED/SHIPPED/CANCELLED")
    private String status;

    @Schema(description = "回填的运单号")
    private String waybillNo;

    @Schema(description = "受理员工ID")
    private Long operatorId;

    @Schema(description = "登记时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    // ==================== 关联查询填充字段 ====================

    @TableField(exist = false)
    @Schema(description = "驿站名称")
    private String stationName;

    @TableField(exist = false)
    @Schema(description = "受理员工姓名")
    private String operatorName;

    @TableField(exist = false)
    @Schema(description = "状态中文名称，如「待揽收」")
    private String statusName;

    @TableField(exist = false)
    @Schema(description = "快件类型中文名称")
    private String parcelTypeName;
}
