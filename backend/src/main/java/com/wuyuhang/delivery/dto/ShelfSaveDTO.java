package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 货位保存请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "货位保存请求")
public class ShelfSaveDTO implements Serializable {

    @NotNull(message = "所属驿站不能为空")
    @Schema(description = "所属驿站ID")
    private Long stationId;

    @NotBlank(message = "库位编号不能为空")
    @Schema(description = "库位编号，如 A-01-01")
    private String shelfCode;

    @Schema(description = "库区")
    private String area;

    @NotNull(message = "库位容量不能为空")
    @Schema(description = "库位容量（件）")
    private Integer capacity;

    @Schema(description = "状态：1 可用 0 停用")
    private Integer status = 1;
}
