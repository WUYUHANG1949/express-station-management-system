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
 * 货架库位实体，对应表 shelf。
 * <p>
 * 每个快件入库时会占用一个库位（used_count + 1），取件核销时释放（used_count - 1），
 * 这一对操作必须在同一个事务中完成，否则会出现"库存不一致"。
 *
 * @author 吴宇航
 */
@Data
@TableName("shelf")
@Schema(description = "货架库位")
public class Shelf implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "库位ID")
    private Long id;

    @Schema(description = "所属驿站ID")
    private Long stationId;

    @Schema(description = "库位编号，如 A-01-01")
    private String shelfCode;

    @Schema(description = "库区")
    private String area;

    @Schema(description = "库位容量（件）")
    private Integer capacity;

    @Schema(description = "已使用数量（件）")
    private Integer usedCount;

    @Schema(description = "状态：1 可用 0 停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 驿站名称，关联查询填充 */
    @TableField(exist = false)
    @Schema(description = "驿站名称")
    private String stationName;

    /** 剩余容量，由 capacity - usedCount 计算得到 */
    @TableField(exist = false)
    @Schema(description = "剩余容量")
    private Integer freeCount;
}
