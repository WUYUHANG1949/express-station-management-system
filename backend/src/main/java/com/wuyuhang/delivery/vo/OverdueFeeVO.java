package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 逾期保管费试算结果。
 *
 * @author 吴宇航
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "逾期保管费试算结果")
public class OverdueFeeVO implements Serializable {

    @Schema(description = "已保管天数")
    private Integer storageDays;

    @Schema(description = "免费保管天数")
    private Integer freeDays;

    @Schema(description = "逾期天数")
    private Integer overdueDays;

    @Schema(description = "应缴保管费(元)")
    private BigDecimal overdueFee;
}
