package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 寄件登记请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "寄件登记请求")
public class ShipOrderDTO implements Serializable {

    @NotNull(message = "受理驿站不能为空")
    @Schema(description = "受理驿站ID")
    private Long stationId;

    @NotBlank(message = "快递公司不能为空")
    @Schema(description = "快递公司")
    private String expressCompany;

    @NotBlank(message = "寄件人姓名不能为空")
    @Schema(description = "寄件人姓名")
    private String senderName;

    @NotBlank(message = "寄件人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "寄件人手机号格式不正确")
    @Schema(description = "寄件人手机号")
    private String senderPhone;

    @NotBlank(message = "寄件人地址不能为空")
    @Size(max = 255, message = "寄件人地址长度不能超过 255 个字符")
    @Schema(description = "寄件人地址")
    private String senderAddress;

    @NotBlank(message = "收件人姓名不能为空")
    @Schema(description = "收件人姓名")
    private String receiverName;

    @NotBlank(message = "收件人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收件人手机号格式不正确")
    @Schema(description = "收件人手机号")
    private String receiverPhone;

    @NotBlank(message = "收件人地址不能为空")
    @Size(max = 255, message = "收件人地址长度不能超过 255 个字符")
    @Schema(description = "收件人地址")
    private String receiverAddress;

    @Schema(description = "快件类型")
    private String parcelType = "NORMAL";

    @Schema(description = "重量(kg)")
    private BigDecimal weight;

    @NotNull(message = "运费不能为空")
    @Schema(description = "运费(元)")
    private BigDecimal freight;

    @Schema(description = "保价金额(元)")
    private BigDecimal insuredValue;
}
