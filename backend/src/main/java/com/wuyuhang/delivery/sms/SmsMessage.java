package com.wuyuhang.delivery.sms;

import lombok.Builder;
import lombok.Data;

/**
 * 发送验证码所需的信息载体。
 *
 * @author 吴宇航
 */
@Data
@Builder
public class SmsMessage {

    /** 接收方手机号（真实短信通道使用）。 */
    private String phone;

    /** 接收方账号名（写入日志与邮件正文，便于人工核对）。 */
    private String username;

    /** 验证码。 */
    private String code;

    /** 有效期（分钟），用于生成文案。 */
    private int expireMinutes;

    /** 场景：PASSWORD_RESET。 */
    private String scene;

    /**
     * 渲染后的纯文本内容，供 mock 日志与邮件正文复用，
     * 保证三种通道下用户看到的是同一套文案。
     *
     * @return 文本内容
     */
    public String toText() {
        return String.format(
                "您好：您的验证码是 %s，%d 分钟内有效。如非本人操作请忽略本条信息。（场景：%s）",
                code, expireMinutes, "PASSWORD_RESET".equals(scene) ? "找回密码" : scene);
    }
}
