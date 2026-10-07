package com.wuyuhang.delivery.vo;

import com.wuyuhang.delivery.entity.Parcel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 货位地图中的一个格子（一个库位）。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "货位地图条目")
public class ShelfMapVO implements Serializable {

    @Schema(description = "库位ID")
    private Long shelfId;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "所属驿站名称", example = "幸福小区快递驿站")
    private String stationName;

    @Schema(description = "库位编号，如 A-01-01")
    private String shelfCode;

    @Schema(description = "库区")
    private String area;

    @Schema(description = "库位容量")
    private Integer capacity;

    @Schema(description = "已占用数量")
    private Integer usedCount;

    @Schema(description = "剩余容量")
    private Integer freeCount;

    @Schema(description = "占用率（百分比，保留 1 位小数）")
    private BigDecimal rate;

    @Schema(description = "状态：1 可用 0 停用")
    private Integer status;

    /**
     * 占用程度：EMPTY 空 / NORMAL 正常 / BUSY 较满 / FULL 已满。
     * 前端据此给格子上色（绿 / 蓝 / 橙 / 红）。
     */
    @Schema(description = "占用程度：EMPTY/NORMAL/BUSY/FULL")
    private String level;

    @Schema(description = "该库位上的快件列表")
    private List<Parcel> parcels = new ArrayList<>();

    public ShelfMapVO() {
    }

    public ShelfMapVO(Long shelfId, Long stationId, String stationName,
                      String shelfCode, String area, Integer capacity, Integer usedCount, Integer status) {
        this.shelfId = shelfId;
        this.stationId = stationId;
        this.stationName = stationName;
        this.shelfCode = shelfCode;
        this.area = area;
        this.capacity = capacity == null ? 0 : capacity;
        this.usedCount = usedCount == null ? 0 : usedCount;
        this.status = status;
        this.freeCount = Math.max(this.capacity - this.usedCount, 0);
        this.rate = this.capacity == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(this.usedCount * 100.0 / this.capacity).setScale(1, RoundingMode.HALF_UP);
        this.level = computeLevel(this.capacity, this.usedCount);
    }

    /**
     * 根据占用率划分占用程度。库位停用时也标记为停用等级由前端单独处理。
     */
    private static String computeLevel(int capacity, int used) {
        if (capacity <= 0 || used <= 0) {
            return "EMPTY";
        }
        if (used >= capacity) {
            return "FULL";
        }
        double rate = used * 1.0 / capacity;
        return rate >= 0.8 ? "BUSY" : "NORMAL";
    }
}
