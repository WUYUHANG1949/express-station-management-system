package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 驿站业务量排行 VO。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "驿站业务量排行")
public class StationRankVO implements Serializable {

    @Schema(description = "驿站名称")
    private String stationName;

    @Schema(description = "累计收件量")
    private Integer parcelCount;

    @Schema(description = "累计取件量")
    private Integer pickupCount;
}
