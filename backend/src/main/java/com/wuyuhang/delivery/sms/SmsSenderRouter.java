package com.wuyuhang.delivery.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发送通道路由器：按 {@code sms.provider} 选择具体实现。
 * <p>
 * Spring 会把所有 {@link SmsSender} 实现注入进来，这里按 {@link SmsSender#provider()}
 * 匹配配置值；配置写错时退化为 mock 并打印警告，保证系统永远不会因为配置问题而无法发码。
 *
 * @author 吴宇航
 */
@Slf4j
@Component
public class SmsSenderRouter {

    private final SmsProperties properties;
    private final List<SmsSender> senders;
    private final SmsSender active;

    public SmsSenderRouter(SmsProperties properties, List<SmsSender> senders) {
        this.properties = properties;
        this.senders = senders;
        String want = properties.getProvider() == null ? "mock" : properties.getProvider().trim().toLowerCase();
        SmsSender matched = null;
        for (SmsSender sender : senders) {
            if (sender.provider().equalsIgnoreCase(want)) {
                matched = sender;
                break;
            }
        }
        if (matched == null) {
            log.warn("未找到 sms.provider={} 对应的发送通道，已退化为 mock。可选值：{}",
                    want, senders.stream().map(SmsSender::provider).toList());
            for (SmsSender sender : senders) {
                if ("mock".equals(sender.provider())) {
                    matched = sender;
                    break;
                }
            }
        }
        this.active = matched;
        log.info("验证码发送通道：{}", active == null ? "未配置" : active.provider());
    }

    /**
     * 当前生效的通道标识。
     *
     * @return mock / mail / aliyun
     */
    public String activeProvider() {
        return active == null ? "none" : active.provider();
    }

    /**
     * 是否允许把验证码回传到接口响应（仅 mock 通道 + 演示开关同时成立时为 true）。
     *
     * @return 是否回传
     */
    public boolean canExposeCode() {
        return properties.isExposeCodeInResponse() && "mock".equals(activeProvider());
    }

    /**
     * 发送验证码。
     *
     * @param message 验证码信息
     * @return 是否成功
     */
    public boolean send(SmsMessage message) {
        if (active == null) {
            return false;
        }
        return active.send(message);
    }
}
