package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 出入库趋势 VO，前端 ECharts 折线图直接使用。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "出入库趋势")
public class TrendVO implements Serializable {

    @Schema(description = "日期轴，如 [\"10-01\",\"10-02\"]")
    private List<String> dates = new ArrayList<>();

    @Schema(description = "每日入库量")
    private List<Integer> inCounts = new ArrayList<>();

    @Schema(description = "每日取件量")
    private List<Integer> pickupCounts = new ArrayList<>();
}
