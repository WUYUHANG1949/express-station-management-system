package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 取件/逾期通知记录实体，对应表 notify_record。
 * <p>
 * 驿站到件后需要通知收件人取件，逾期后需要催取。本表记录每条通知的
 * 类型、渠道、内容、发送结果与操作人，既是业务凭证也是纠纷追溯依据。
 *
 * @author 吴宇航
 */
@Data
@TableName("notify_record")
@Schema(description = "通知记录")
public class NotifyRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "通知记录ID")
    private Long id;

    @Schema(description = "快件ID")
    private Long parcelId;

    @Schema(description = "运单号")
    private String waybillNo;

    @Schema(description = "取件码")
    private String pickupCode;

    @Schema(description = "通知类型：IN_STORE/OVERDUE/PICKUP_DONE/EXCEPTION")
    private String notifyType;

    @Schema(description = "通知渠道：SMS/APP/PHONE")
    private String channel;

    @Schema(description = "接收手机号")
    private String receiverPhone;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "发送结果：SUCCESS/FAILED")
    private String sendStatus;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    // ==================== 关联查询填充字段 ====================

    @TableField(exist = false)
    @Schema(description = "收件人姓名")
    private String receiverName;

    @TableField(exist = false)
    @Schema(description = "驿站名称")
    private String stationName;

    @TableField(exist = false)
    @Schema(description = "通知类型中文名称")
    private String notifyTypeName;

    @TableField(exist = false)
    @Schema(description = "通知渠道中文名称")
    private String channelName;

    @TableField(exist = false)
    @Schema(description = "发送结果中文名称")
    private String sendStatusName;
}
