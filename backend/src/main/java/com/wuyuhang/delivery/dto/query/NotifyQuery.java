package com.wuyuhang.delivery.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 通知记录查询条件。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "通知记录查询条件")
public class NotifyQuery implements Serializable {

    @Schema(description = "运单号（模糊匹配）")
    private String waybillNo;

    @Schema(description = "接收手机号（模糊匹配）")
    private String receiverPhone;

    @Schema(description = "通知类型：IN_STORE/OVERDUE/PICKUP_DONE/EXCEPTION")
    private String notifyType;

    @Schema(description = "通知渠道：SMS/APP/PHONE")
    private String channel;

    @Schema(description = "发送结果：SUCCESS/FAILED")
    private String sendStatus;

    @Schema(description = "驿站ID")
    private Long stationId;

    @Schema(description = "发送开始日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;

    @Schema(description = "发送结束日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;
}
