package com.wuyuhang.delivery.sms;

/**
 * 验证码发送通道的统一抽象。
 * <p>
 * 业务层（{@link com.wuyuhang.delivery.service.SmsCodeService}）只依赖本接口，
 * 具体走"只记日志 / 发邮件 / 发真实短信"由 {@code sms.provider} 配置决定，
 * 因此换通道不需要改动任何业务代码。
 * <p>
 * 实现类通过 {@link #provider()} 暴露自己的通道标识，由
 * {@link SmsSenderRouter} 在启动时按配置挑选，未选中的实现不会生效。
 *
 * @author 吴宇航
 */
public interface SmsSender {

    /**
     * 通道标识，与 {@code sms.provider} 的取值对应：mock / mail / aliyun。
     *
     * @return 通道标识
     */
    String provider();

    /**
     * 发送验证码。
     *
     * @param message 发送所需的信息（手机号、验证码、有效期等）
     * @return 是否发送成功；失败时由调用方决定如何提示用户
     */
    boolean send(SmsMessage message);
}
