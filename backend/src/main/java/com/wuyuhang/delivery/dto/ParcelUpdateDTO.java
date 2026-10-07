package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 编辑快件请求参数。运单号与驿站不允许修改，避免破坏轨迹的连续性。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "编辑快件请求")
public class ParcelUpdateDTO implements Serializable {

    @Size(max = 50, message = "收件人姓名长度不能超过 50 个字符")
    @Schema(description = "收件人姓名")
    private String receiverName;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "收件人手机号格式不正确")
    @Schema(description = "收件人手机号")
    private String receiverPhone;

    @Schema(description = "快件类型")
    private String parcelType;

    @Schema(description = "货位ID，修改后会同步调整新旧货位的占用数量")
    private Long shelfId;

    @Schema(description = "免费保管天数")
    private Integer overdueDays;

    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    @Schema(description = "备注")
    private String remark;
}
