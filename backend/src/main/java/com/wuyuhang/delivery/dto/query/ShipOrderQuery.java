package com.wuyuhang.delivery.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 寄件单查询条件。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "寄件单查询条件")
public class ShipOrderQuery implements Serializable {

    @Schema(description = "寄件单号（模糊匹配）")
    private String orderNo;

    @Schema(description = "运单号（模糊匹配）")
    private String waybillNo;

    @Schema(description = "寄件人姓名（模糊匹配）")
    private String senderName;

    @Schema(description = "寄件人手机号（模糊匹配）")
    private String senderPhone;

    @Schema(description = "收件人姓名（模糊匹配）")
    private String receiverName;

    @Schema(description = "收件人手机号（模糊匹配）")
    private String receiverPhone;

    @Schema(description = "状态：PENDING/ACCEPTED/SHIPPED/CANCELLED")
    private String status;

    @Schema(description = "驿站ID")
    private Long stationId;

    @Schema(description = "登记开始日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;

    @Schema(description = "登记结束日期")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;
}
