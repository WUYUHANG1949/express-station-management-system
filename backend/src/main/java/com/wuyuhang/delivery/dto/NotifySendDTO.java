package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 发送通知请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "发送通知请求")
public class NotifySendDTO implements Serializable {

    @NotNull(message = "快件ID不能为空")
    @Schema(description = "快件ID")
    private Long parcelId;

    @NotBlank(message = "通知类型不能为空")
    @Schema(description = "通知类型：IN_STORE/OVERDUE/PICKUP_DONE/EXCEPTION")
    private String notifyType;

    @Schema(description = "通知渠道：SMS/APP/PHONE，默认 SMS")
    private String channel = "SMS";

    @Schema(description = "自定义通知内容；留空则由系统按模板自动生成")
    private String content;

    @Schema(description = "接收手机号；留空则取快件的收件人手机号")
    private String receiverPhone;
}
