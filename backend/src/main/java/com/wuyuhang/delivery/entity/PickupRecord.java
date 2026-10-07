package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 取件/出库记录实体，对应表 pickup_record。
 * <p>
 * 取件核销成功后写入本表，记录"谁在什么时间、用什么方式、把哪一件快件交给了谁"，
 * 与 parcel_trace 的区别是：本表偏重业务凭证（含核销取件码、实收保管费），轨迹表偏重操作流水。
 *
 * @author 吴宇航
 */
@Data
@TableName("pickup_record")
@Schema(description = "取件记录")
public class PickupRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "取件记录ID")
    private Long id;

    @Schema(description = "快件ID")
    private Long parcelId;

    @Schema(description = "运单号")
    private String waybillNo;

    @Schema(description = "核销取件码")
    private String pickupCode;

    @Schema(description = "实际取件人姓名")
    private String receiverName;

    @Schema(description = "取件人手机号")
    private String receiverPhone;

    @Schema(description = "取件方式：SELF 本人自取 AGENT 代取 DELIVERY 送货上门")
    private String pickupType;

    @Schema(description = "核验方式：CODE 取件码 ID_CARD 身份证 PHONE 手机号")
    private String verifyType;

    @Schema(description = "实收保管费(元)")
    private BigDecimal storageFee;

    @Schema(description = "驿站ID")
    private Long stationId;

    @Schema(description = "核销操作员ID")
    private Long operatorId;

    @Schema(description = "核销操作员姓名")
    private String operatorName;

    @Schema(description = "取件时间")
    private LocalDateTime pickupTime;

    @Schema(description = "备注")
    private String remark;
}
