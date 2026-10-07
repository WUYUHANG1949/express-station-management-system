package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 收件登记（入库）请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "收件登记请求")
public class ParcelInStoreDTO implements Serializable {

    @NotBlank(message = "运单号不能为空")
    @Size(max = 40, message = "运单号长度不能超过 40 个字符")
    @Schema(description = "快递运单号", example = "SF1234567890123")
    private String waybillNo;

    @Schema(description = "驿站ID，员工登录时可不传（自动取所属驿站）")
    private Long stationId;

    @NotBlank(message = "快递公司不能为空")
    @Schema(description = "快递公司", example = "顺丰速运")
    private String expressCompany;

    @Schema(description = "快件类型：NORMAL/SMALL/LARGE/FRAGILE/DOCUMENT/COLD", example = "NORMAL")
    private String parcelType = "NORMAL";

    @NotBlank(message = "收件人姓名不能为空")
    @Size(max = 50, message = "收件人姓名长度不能超过 50 个字符")
    @Schema(description = "收件人姓名", example = "王小明")
    private String receiverName;

    @NotBlank(message = "收件人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收件人手机号格式不正确")
    @Schema(description = "收件人手机号", example = "13900000001")
    private String receiverPhone;

    @Schema(description = "重量(kg)", example = "1.5")
    private java.math.BigDecimal weight;

    @Schema(description = "代收运费(元)", example = "0")
    private java.math.BigDecimal freight;

    @Schema(description = "货位ID，为空时由系统自动分配剩余容量最大的库位")
    private Long shelfId;

    @Schema(description = "免费保管天数，默认 3 天")
    private Integer overdueDays;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
