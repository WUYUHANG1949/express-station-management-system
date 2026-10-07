package com.wuyuhang.delivery.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 异常件查询条件。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "异常件查询条件")
public class ExceptionQuery implements Serializable {

    @Schema(description = "运单号（模糊匹配）")
    private String waybillNo;

    @Schema(description = "异常类型")
    private String exceptionType;

    @Schema(description = "处理状态")
    private String handleStatus;

    @Schema(description = "驿站ID")
    private Long stationId;
}
