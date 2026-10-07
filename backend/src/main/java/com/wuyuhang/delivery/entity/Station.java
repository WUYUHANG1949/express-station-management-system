package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 驿站实体，对应表 station。
 *
 * @author 吴宇航
 */
@Data
@TableName("station")
@Schema(description = "驿站")
public class Station implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "驿站ID")
    private Long id;

    @Schema(description = "驿站编号")
    private String stationCode;

    @Schema(description = "驿站名称")
    private String stationName;

    @Schema(description = "驿站地址")
    private String address;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "负责人")
    private String managerName;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "货位总容量")
    private Integer capacity;

    @Schema(description = "状态：1 营业 0 停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 货位数量，统计查询时填充 */
    @TableField(exist = false)
    @Schema(description = "货位数量")
    private Integer shelfCount;

    /** 在用货位数量，统计查询时填充 */
    @TableField(exist = false)
    @Schema(description = "在库快件数")
    private Integer usedCount;
}
