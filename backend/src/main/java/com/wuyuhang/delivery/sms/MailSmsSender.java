package com.wuyuhang.delivery.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮件发送通道（可切换的"真发"方案之一）。
 * <p>
 * 为什么用邮件替代短信：短信需要企业实名认证、签名与模板审核（1~3 个工作日），
 * 而邮件只需一个支持 SMTP 的邮箱即可立即演示"验证码真的送达了终端"，
 * 且成本为零。项目把"发送"抽象为 {@link SmsSender}，因此两者可以随时互换。
 * <p>
 * 演示约定：所有验证码统一投递到 {@code sms.mail.to} 配置的邮箱，
 * 邮件正文中会注明本应投递的手机号（打码），便于核对是哪位用户的验证码。
 * <p>
 * SMTP 账号密码配置在 application-dev.yml（已 gitignore）或环境变量中，
 * 不写入仓库。
 *
 * @author 吴宇航
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailSmsSender implements SmsSender {

    private final JavaMailSender mailSender;
    private final SmsProperties properties;

    @Override
    public String provider() {
        return "mail";
    }

    @Override
    public boolean send(SmsMessage message) {
        SmsProperties.Mail cfg = properties.getMail();
        if (!StringUtils.hasText(cfg.getTo())) {
            log.warn("[MAIL-SMS] 未配置 sms.mail.to，无法投递验证码；请改回 provider=mock 或在 application-dev.yml 中配置收件邮箱");
            return false;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(cfg.getTo());
            mail.setSubject(cfg.getSubject());
            mail.setText(buildBody(message));
            mailSender.send(mail);
            log.info("[MAIL-SMS] 验证码已投递至 {}（对应手机号 {}，账号 {}）",
                    cfg.getTo(), maskPhone(message.getPhone()), message.getUsername());
            return true;
        } catch (Exception e) {
            // 发送失败不抛给用户，只记录日志，由业务层给出统一提示
            log.error("[MAIL-SMS] 投递失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 组装邮件正文。除验证码外，额外写明手机号（打码）与账号，
     * 避免演示邮箱里堆着多个验证码时分不清是谁的。
     */
    private String buildBody(SmsMessage message) {
        return "快递驿站快件收发管理系统 —— 找回密码验证码\n"
                + "\n"
                + "账号：" + message.getUsername() + "\n"
                + "手机号：" + maskPhone(message.getPhone()) + "（本项目未接入真实短信网关，验证码统一投递到该演示邮箱）\n"
                + "\n"
                + message.toText() + "\n"
                + "\n"
                + "如果不是您本人操作，请忽略本邮件，您的密码不会被修改。\n";
    }

    /** 手机号打码，避免日志与邮件中留存完整号码。 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
