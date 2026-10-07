package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 取件核销（出库）请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "取件核销请求")
public class ParcelPickupDTO implements Serializable {

    @NotBlank(message = "运单号不能为空")
    @Schema(description = "快递运单号")
    private String waybillNo;

    @NotBlank(message = "取件码不能为空")
    @Schema(description = "取件码")
    private String pickupCode;

    @NotBlank(message = "实际取件人不能为空")
    @Schema(description = "实际取件人姓名")
    private String receiverName;

    @Schema(description = "取件人手机号")
    private String receiverPhone;

    @Schema(description = "取件方式：SELF 本人自取 AGENT 代取 DELIVERY 送货上门", example = "SELF")
    private String pickupType = "SELF";

    @Schema(description = "核验方式：CODE 取件码 ID_CARD 身份证 PHONE 手机号", example = "CODE")
    private String verifyType = "CODE";

    @Schema(description = "实收保管费(元)")
    private BigDecimal storageFee;

    @Schema(description = "备注")
    private String remark;
}
