package com.wuyuhang.delivery.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 快件查询条件。
 * <p>
 * 所有字段均可为空，为空时对应的查询条件不参与 SQL 拼接，实现"条件组合查询"。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "快件查询条件")
public class ParcelQuery implements Serializable {

    @Schema(description = "运单号（模糊匹配）")
    private String waybillNo;

    @Schema(description = "取件码（精确匹配）")
    private String pickupCode;

    @Schema(description = "收件人姓名（模糊匹配）")
    private String receiverName;

    @Schema(description = "收件人手机号（模糊匹配）")
    private String receiverPhone;

    @Schema(description = "快递公司")
    private String expressCompany;

    @Schema(description = "快件状态")
    private String status;

    @Schema(description = "快件类型")
    private String parcelType;

    @Schema(description = "驿站ID")
    private Long stationId;

    @Schema(description = "入库开始日期，格式 yyyy-MM-dd")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;

    @Schema(description = "入库结束日期，格式 yyyy-MM-dd")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;

    @Schema(description = "通用关键字（取件核销页使用：取件码/运单号/手机号/姓名）")
    private String keyword;

    /** 普通用户只能查询本人手机号下的快件，由 Service 层强制写入，前端无法伪造 */
    @Schema(description = "数据权限：仅查询该手机号的快件", hidden = true)
    private String limitPhone;
}
