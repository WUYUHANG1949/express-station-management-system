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
 * 异常件记录实体，对应表 exception_record。
 * <p>
 * 破损、丢失、地址错误、拒收、长期未取等异常情形单独流转，
 * 与正常快件区分开，便于驿站统计责任与跟进处理进度。
 *
 * @author 吴宇航
 */
@Data
@TableName("exception_record")
@Schema(description = "异常件记录")
public class ExceptionRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "异常记录ID")
    private Long id;

    @Schema(description = "快件ID")
    private Long parcelId;

    @Schema(description = "运单号")
    private String waybillNo;

    @Schema(description = "异常类型：DAMAGED/LOST/ADDRESS_ERROR/REFUSED/TIMEOUT/OTHER")
    private String exceptionType;

    @Schema(description = "异常描述")
    private String description;

    @Schema(description = "处理状态：PENDING/HANDLING/RESOLVED")
    private String handleStatus;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理人姓名")
    private String handlerName;

    @Schema(description = "处理结果")
    private String handleResult;

    @Schema(description = "驿站ID")
    private Long stationId;

    @Schema(description = "登记时间")
    private LocalDateTime createTime;

    @Schema(description = "处理完成时间")
    private LocalDateTime handleTime;

    // ==================== 关联查询填充字段 ====================

    @TableField(exist = false)
    @Schema(description = "驿站名称")
    private String stationName;

    @TableField(exist = false)
    @Schema(description = "收件人姓名")
    private String receiverName;

    @TableField(exist = false)
    @Schema(description = "收件人手机号")
    private String receiverPhone;

    @TableField(exist = false)
    @Schema(description = "异常类型中文名称，如「破损」")
    private String exceptionTypeName;

    @TableField(exist = false)
    @Schema(description = "处理状态中文名称，如「待处理」")
    private String handleStatusName;
}
