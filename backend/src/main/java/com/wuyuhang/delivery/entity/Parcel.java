package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快件实体，对应表 parcel —— 本系统的核心业务表。
 * <p>
 * 状态流转：
 * <pre>
 *   IN_STORE(在库待取) --取件核销--> PICKED_UP(已取件)
 *   IN_STORE(在库待取) --派送-->     DELIVERING(派送中) --签收--> PICKED_UP(已取件)
 *   IN_STORE(在库待取) --登记异常--> EXCEPTION(异常件) --退回--> RETURNED(已退回)
 * </pre>
 *
 * @author 吴宇航
 */
@Data
@TableName("parcel")
@Schema(description = "快件")
public class Parcel implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "快件ID")
    private Long id;

    @Schema(description = "快递运单号")
    private String waybillNo;

    @Schema(description = "所在驿站ID")
    private Long stationId;

    @Schema(description = "存放库位ID")
    private Long shelfId;

    @Schema(description = "快递公司")
    private String expressCompany;

    @Schema(description = "快件类型：NORMAL/SMALL/LARGE/FRAGILE/DOCUMENT/COLD")
    private String parcelType;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人手机号")
    private String receiverPhone;

    @Schema(description = "取件码（8 位数字）")
    private String pickupCode;

    @Schema(description = "重量(kg)")
    private BigDecimal weight;

    @Schema(description = "代收运费(元)")
    private BigDecimal freight;

    @Schema(description = "状态：IN_STORE/PICKED_UP/DELIVERING/EXCEPTION/RETURNED")
    private String status;

    @Schema(description = "入库时间")
    private LocalDateTime inTime;

    @Schema(description = "取件/签收时间")
    private LocalDateTime pickupTime;

    @Schema(description = "免费保管天数")
    private Integer overdueDays;

    @Schema(description = "逾期保管费(元)")
    private BigDecimal storageFee;

    @Schema(description = "入库操作员ID")
    private Long operatorId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    // ==================== 以下为关联查询填充字段，非数据库字段 ====================

    @TableField(exist = false)
    @Schema(description = "驿站名称")
    private String stationName;

    @TableField(exist = false)
    @Schema(description = "库位编号")
    private String shelfCode;

    @TableField(exist = false)
    @Schema(description = "入库操作员姓名")
    private String operatorName;

    @TableField(exist = false)
    @Schema(description = "状态中文名称，如「在库待取」")
    private String statusName;

    @TableField(exist = false)
    @Schema(description = "快件类型中文名称，如「普通件」")
    private String parcelTypeName;

    @TableField(exist = false)
    @Schema(description = "已保管天数")
    private Integer storageDays;

    @TableField(exist = false)
    @Schema(description = "逾期天数（已超过免费保管期的天数，未逾期为 0）")
    private Integer overdueDayCount;

    @TableField(exist = false)
    @Schema(description = "逾期保管费(元)")
    private BigDecimal overdueFee;
}
