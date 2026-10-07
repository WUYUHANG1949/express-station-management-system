package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 异常件登记请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "异常件登记请求")
public class ExceptionCreateDTO implements Serializable {

    @Schema(description = "快件ID，与运单号二选一")
    private Long parcelId;

    @Schema(description = "运单号，与快件ID二选一")
    private String waybillNo;

    @NotBlank(message = "异常类型不能为空")
    @Schema(description = "异常类型：DAMAGED/LOST/ADDRESS_ERROR/REFUSED/TIMEOUT/OTHER")
    private String exceptionType;

    @Schema(description = "异常描述")
    private String description;
}
