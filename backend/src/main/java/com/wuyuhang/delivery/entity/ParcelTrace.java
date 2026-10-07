package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 快件轨迹实体，对应表 parcel_trace。
 * <p>
 * 每一次对快件的关键操作都写入一条轨迹，形成"操作留痕"，
 * 交易纠纷时可作为责任追溯依据。表内冗余 waybill_no 与 operator_name 是为了
 * 查询轨迹时不再关联用户表，提升查询性能。
 *
 * @author 吴宇航
 */
@Data
@TableName("parcel_trace")
@Schema(description = "快件轨迹")
public class ParcelTrace implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "轨迹ID")
    private Long id;

    @Schema(description = "快件ID")
    private Long parcelId;

    @Schema(description = "运单号（冗余字段）")
    private String waybillNo;

    @Schema(description = "操作类型：IN_STORE/PICKUP/DELIVER/EXCEPTION/RETURN/TRANSFER/EDIT")
    private String operateType;

    @Schema(description = "操作描述")
    private String operateDesc;

    @Schema(description = "操作驿站ID")
    private Long stationId;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名（冗余字段）")
    private String operatorName;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
