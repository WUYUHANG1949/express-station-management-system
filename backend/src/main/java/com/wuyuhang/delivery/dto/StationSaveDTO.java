package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 驿站保存请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "驿站保存请求")
public class StationSaveDTO implements Serializable {

    @NotBlank(message = "驿站编号不能为空")
    @Schema(description = "驿站编号", example = "ST003")
    private String stationCode;

    @NotBlank(message = "驿站名称不能为空")
    @Schema(description = "驿站名称")
    private String stationName;

    @NotBlank(message = "驿站地址不能为空")
    @Size(max = 255, message = "地址长度不能超过 255 个字符")
    @Schema(description = "驿站地址")
    private String address;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "负责人")
    private String managerName;

    @Schema(description = "营业时间")
    private String businessHours = "08:00-21:00";

    @NotNull(message = "货位总容量不能为空")
    @Schema(description = "货位总容量")
    private Integer capacity;

    @Schema(description = "状态：1 营业 0 停用")
    private Integer status = 1;
}
