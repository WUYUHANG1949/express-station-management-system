package com.wuyuhang.delivery.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模拟发送通道（默认启用）。
 * <p>
 * 只把验证码打到日志里，不产生任何外部调用与费用，便于开发与答辩演示。
 * 配合 {@code sms.expose-code-in-response: true}，接口会把验证码一并返回，
 * 前端可直接展示，无需真实短信或邮箱即可完整走通找回流程。
 * <p>
 * 生产环境应把 provider 切到 aliyun 或 mail，并关闭 expose-code-in-response。
 *
 * @author 吴宇航
 */
@Slf4j
@Component
public class MockSmsSender implements SmsSender {

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public boolean send(SmsMessage message) {
        log.info("[MOCK-SMS] 手机号={} 账号={} 验证码={}（{} 分钟内有效）",
                message.getPhone(), message.getUsername(), message.getCode(), message.getExpireMinutes());
        // 模拟通道永远成功，保证本地开发不依赖任何外部服务
        return true;
    }
}
