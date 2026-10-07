package com.wuyuhang.delivery.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 首页概览统计 VO。
 * <p>
 * 数据库查询只取出货位总数与占用数两个标量，
 * 货位使用率与嵌套的 {@code shelfUsage} 对象由本类计算后输出，
 * 保证前端拿到的 JSON 结构与接口契约一致。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "首页概览统计")
public class StatOverviewVO implements Serializable {

    @Schema(description = "今日入库件数")
    private Integer todayInCount = 0;

    @Schema(description = "今日取件件数")
    private Integer todayPickupCount = 0;

    @Schema(description = "今日寄件单数")
    private Integer todayShipCount = 0;

    @Schema(description = "当前在库件数")
    private Integer inStoreCount = 0;

    @Schema(description = "逾期未取件数")
    private Integer overdueCount = 0;

    @Schema(description = "未处理完的异常件数")
    private Integer exceptionCount = 0;

    @Schema(description = "派送中件数")
    private Integer deliveringCount = 0;

    @Schema(description = "历史累计快件数")
    private Integer totalParcelCount = 0;

    /** 货位总容量，仅参与计算，不直接输出 */
    @JsonIgnore
    private Integer shelfTotal = 0;

    /** 货位已占用数量，仅参与计算，不直接输出 */
    @JsonIgnore
    private Integer shelfUsed = 0;

    /**
     * 货位使用情况，输出为嵌套对象 shelfUsage。
     */
    @Schema(description = "货位使用情况")
    public ShelfUsage getShelfUsage() {
        return new ShelfUsage(shelfTotal, shelfUsed);
    }

    /**
     * 货位使用情况内部类。
     */
    @Data
    @Schema(description = "货位使用情况")
    public static class ShelfUsage implements Serializable {

        @Schema(description = "货位总容量")
        private Integer total;

        @Schema(description = "已占用数量")
        private Integer used;

        @Schema(description = "使用率（百分比，保留 1 位小数）")
        private BigDecimal rate;

        public ShelfUsage(Integer total, Integer used) {
            this.total = total == null ? 0 : total;
            this.used = used == null ? 0 : used;
            this.rate = this.total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(this.used * 100.0 / this.total).setScale(1, RoundingMode.HALF_UP);
        }
    }
}
