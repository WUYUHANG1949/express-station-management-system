package com.wuyuhang.delivery.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信/验证码相关配置，对应 application.yml 中的 {@code sms.*}。
 * <p>
 * 设计要点：把"发送通道"做成可切换的（mock / mail / aliyun），
 * 业务代码只依赖 {@link SmsSender} 接口，换通道不需要改业务逻辑。
 * <p>
 * 密钥类配置一律支持环境变量占位（如 {@code ${SMS_MAIL_PASSWORD:}}），
 * 不写死在代码与仓库里。
 *
 * @author 吴宇航
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /**
     * 发送通道：mock（默认，只记录不真发）/ mail（邮件投递演示）/ aliyun（阿里云短信）。
     */
    private String provider = "mock";

    /** 验证码位数。 */
    private int codeLength = 6;

    /** 有效期（分钟）。 */
    private int expireMinutes = 5;

    /** 最多允许输错次数，超过即视为失效。 */
    private int maxAttempts = 5;

    /** 同一手机号两次发送的最小间隔（秒），用于限流。 */
    private int intervalSeconds = 60;

    /** 同一手机号每天最多发送次数。 */
    private int dailyLimit = 10;

    /**
     * 演示开关：mock 通道下是否把验证码回传到接口响应里，方便演示与自动化测试。
     * <p>
     * 生产环境必须为 false —— 否则等于把验证码直接告诉调用方。
     * mail / aliyun 通道下该开关不生效。
     */
    private boolean exposeCodeInResponse = true;

    /** 邮件通道配置。 */
    private Mail mail = new Mail();

    /** 阿里云短信通道配置。 */
    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Mail {

        /** 演示收件邮箱：所有验证码统一投递到此邮箱（项目未接入真实短信网关时的替代方案）。 */
        private String to;

        /**
         * 发件人地址。
         * <p>
         * 必须与 {@code spring.mail.username}（认证账号）完全一致：
         * QQ / 163 等邮箱会校验 {@code MAIL FROM} 与认证用户是否同一个，
         * 不一致会直接报 <code>501 Mail from address must be same as authorization user</code>。
         * 因此这里默认取 {@code spring.mail.username}，一般不需要单独配置。
         */
        private String from;

        /** 邮件主题。 */
        private String subject = "【快递驿站系统】找回密码验证码";

        /** 发件人显示名。 */
        private String fromName = "快递驿站快件收发管理系统";
    }

    @Data
    public static class Aliyun {

        /** 阿里云 AccessKeyId，建议通过环境变量 SMS_ALIYUN_AK_ID 注入。 */
        private String accessKeyId;

        /** 阿里云 AccessKeySecret，建议通过环境变量 SMS_ALIYUN_AK_SECRET 注入。 */
        private String accessKeySecret;

        /** 短信签名，需在阿里云控制台申请并通过审核。 */
        private String signName;

        /** 模板 CODE，需在阿里云控制台申请并通过审核，模板变量名默认为 code。 */
        private String templateCode;

        /** 接入区域。 */
        private String regionId = "cn-hangzhou";

        /** 模板中验证码变量的名称。 */
        private String templateParamName = "code";
    }
}
