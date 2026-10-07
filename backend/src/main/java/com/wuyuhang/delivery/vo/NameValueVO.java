package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用「名称 - 数值」统计结果，用于饼图 / 柱状图。
 *
 * @author 吴宇航
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "名称-数值统计项")
public class NameValueVO implements Serializable {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数值")
    private Integer value;
}
