package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 找回密码 —— 申请验证码的返回结果。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "找回密码-申请验证码结果")
public class ForgotPasswordSendVO implements Serializable {

    @Schema(description = "提示文案", example = "验证码已发送至 138****0002，5 分钟内有效")
    private String message;

    @Schema(description = "打码后的手机号", example = "138****0002")
    private String maskedPhone;

    @Schema(description = "有效期（分钟）", example = "5")
    private Integer expireMinutes;

    @Schema(description = "当前发送通道：mock / mail / aliyun", example = "mock")
    private String provider;

    /**
     * 仅供演示：mock 通道下把验证码回传，方便没有短信网关时完整演示找回流程。
     * mail / aliyun 通道下该字段为 null。
     */
    @Schema(description = "演示用验证码（仅 mock 通道返回，其他通道为 null）", example = "123456")
    private String demoCode;
}
