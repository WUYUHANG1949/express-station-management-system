package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.dto.ForgotPasswordResetDTO;
import com.wuyuhang.delivery.dto.ForgotPasswordSendDTO;
import com.wuyuhang.delivery.entity.SmsCode;
import com.wuyuhang.delivery.entity.SysUser;
import com.wuyuhang.delivery.mapper.SmsCodeMapper;
import com.wuyuhang.delivery.mapper.SysUserMapper;
import com.wuyuhang.delivery.sms.SmsMessage;
import com.wuyuhang.delivery.sms.SmsProperties;
import com.wuyuhang.delivery.sms.SmsSenderRouter;
import com.wuyuhang.delivery.vo.ForgotPasswordSendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 找回密码服务：申请验证码 → 校验验证码 → 重置密码。
 * <p>
 * 安全设计（这是答辩会被追问的点）：
 * <ol>
 *   <li><b>双要素校验</b>：账号名与手机号必须同时匹配数据库记录，避免只凭用户名重置他人密码；</li>
 *   <li><b>有效期</b>：验证码 {@code sms.expire-minutes} 分钟后失效（默认 5 分钟）；</li>
 *   <li><b>一次性</b>：校验成功后立即置 {@code used = 1}，同一个码不能重复兑换；</li>
 *   <li><b>错误次数上限</b>：累计输错 {@code sms.max-attempts} 次即视为失效，防 6 位码暴力枚举；</li>
 *   <li><b>发送限流</b>：同一手机号 {@code sms.interval-seconds} 秒内只能申请一次，
 *       每天最多 {@code sms.daily-limit} 次，防止被当作短信轰炸机；</li>
 *   <li><b>账号状态校验</b>：已禁用的账号不允许找回密码。</li>
 * </ol>
 * 此外，无论账号是否存在，对外提示都统一为「账号与手机号不匹配」，
 * 避免把接口变成"用户名是否存在"的探测工具。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    /** 场景：找回密码。 */
    public static final String SCENE_PASSWORD_RESET = "PASSWORD_RESET";

    private final SmsCodeMapper smsCodeMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsSenderRouter senderRouter;
    private final SmsProperties properties;

    private final SecureRandom random = new SecureRandom();

    /**
     * 申请验证码。
     *
     * @param dto      账号名 + 手机号
     * @param clientIp 请求方 IP（审计与限流）
     * @return 发送结果（mock 通道下会附带验证码，便于演示与自动化测试）
     */
    @Transactional(rollbackFor = Exception.class)
    public ForgotPasswordSendVO sendCode(ForgotPasswordSendDTO dto, String clientIp) {
        SysUser user = requireMatchedUser(dto.getUsername(), dto.getPhone());
        checkRateLimit(user.getPhone());

        String code = generateCode();
        SmsCode record = new SmsCode();
        record.setPhone(user.getPhone());
        record.setUsername(user.getUsername());
        record.setCode(code);
        record.setScene(SCENE_PASSWORD_RESET);
        record.setExpireTime(LocalDateTime.now().plusMinutes(properties.getExpireMinutes()));
        record.setUsed(0);
        record.setAttempts(0);
        record.setClientIp(clientIp);
        record.setCreateTime(LocalDateTime.now());
        smsCodeMapper.insert(record);

        boolean sent = senderRouter.send(SmsMessage.builder()
                .phone(user.getPhone())
                .username(user.getUsername())
                .code(code)
                .expireMinutes(properties.getExpireMinutes())
                .scene(SCENE_PASSWORD_RESET)
                .build());
        if (!sent) {
            // 发送失败则把这条码作废，避免用户拿到一个"永远收不到"的验证码
            record.setUsed(1);
            smsCodeMapper.updateById(record);
            throw new BusinessException("验证码发送失败，请稍后重试或联系管理员");
        }

        String masked = maskPhone(user.getPhone());
        ForgotPasswordSendVO vo = new ForgotPasswordSendVO();
        vo.setProvider(senderRouter.activeProvider());
        vo.setMaskedPhone(masked);
        vo.setExpireMinutes(properties.getExpireMinutes());
        // 只有 mock 通道 + 演示开关同时成立时才回传验证码
        vo.setDemoCode(senderRouter.canExposeCode() ? code : null);
        vo.setMessage("验证码已发送至 " + masked + "，" + properties.getExpireMinutes() + " 分钟内有效");
        return vo;
    }

    /**
     * 校验验证码并重置密码。
     *
     * @param dto 账号名 + 手机号 + 验证码 + 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ForgotPasswordResetDTO dto) {
        SysUser user = requireMatchedUser(dto.getUsername(), dto.getPhone());

        SmsCode record = smsCodeMapper.selectOne(Wrappers.<SmsCode>lambdaQuery()
                .eq(SmsCode::getPhone, user.getPhone())
                .eq(SmsCode::getUsername, user.getUsername())
                .eq(SmsCode::getScene, SCENE_PASSWORD_RESET)
                .eq(SmsCode::getUsed, 0)
                .orderByDesc(SmsCode::getId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        if (record.getExpireTime().isBefore(LocalDateTime.now())) {
            record.setUsed(1);
            smsCodeMapper.updateById(record);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (record.getAttempts() != null && record.getAttempts() >= properties.getMaxAttempts()) {
            record.setUsed(1);
            smsCodeMapper.updateById(record);
            throw new BusinessException("验证码错误次数过多，请重新获取");
        }
        if (!record.getCode().equals(dto.getCode())) {
            record.setAttempts((record.getAttempts() == null ? 0 : record.getAttempts()) + 1);
            smsCodeMapper.updateById(record);
            int left = properties.getMaxAttempts() - record.getAttempts();
            throw new BusinessException(left > 0
                    ? "验证码不正确，还可尝试 " + left + " 次"
                    : "验证码错误次数过多，请重新获取");
        }

        // 校验通过：先作废验证码，再更新密码
        record.setUsed(1);
        smsCodeMapper.updateById(record);

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);

        log.info("账号 [{}] 通过手机号验证重置了密码", user.getUsername());
    }

    /**
     * 校验账号名与手机号是否匹配。
     * <p>
     * 所有失败情形统一提示为「账号与手机号不匹配」，不区分"账号不存在"与"手机号不对"，
     * 避免接口被用来探测用户名是否存在。
     */
    private SysUser requireMatchedUser(String username, String phone) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || !phone.equals(user.getPhone())) {
            throw new BusinessException("账号与手机号不匹配，请核对后重试");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
        }
        return user;
    }

    /**
     * 发送限流：间隔 + 每日上限。
     */
    private void checkRateLimit(String phone) {
        LocalDateTime now = LocalDateTime.now();

        SmsCode last = smsCodeMapper.selectOne(Wrappers.<SmsCode>lambdaQuery()
                .eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getScene, SCENE_PASSWORD_RESET)
                .orderByDesc(SmsCode::getId)
                .last("LIMIT 1"));
        if (last != null && last.getCreateTime() != null) {
            long seconds = Duration.between(last.getCreateTime(), now).getSeconds();
            if (seconds < properties.getIntervalSeconds()) {
                // 取整可能因毫秒截断出现 seconds = -1，这里夹到 [1, interval] 区间，
                // 避免出现「请 61 秒后再试」这种超过上限的提示
                long wait = Math.max(1L, Math.min(properties.getIntervalSeconds(), properties.getIntervalSeconds() - seconds));
                throw new BusinessException("请求过于频繁，请 " + wait + " 秒后再试");
            }
        }

        long todayCount = smsCodeMapper.selectCount(Wrappers.<SmsCode>lambdaQuery()
                .eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getScene, SCENE_PASSWORD_RESET)
                .ge(SmsCode::getCreateTime, LocalDateTime.of(now.toLocalDate(), LocalTime.MIN)));
        if (todayCount >= properties.getDailyLimit()) {
            throw new BusinessException("该手机号今日验证码发送次数已达上限，请明天再试");
        }
    }

    /** 生成指定位数的数字验证码（使用 SecureRandom，避免可预测序列）。 */
    private String generateCode() {
        int len = properties.getCodeLength();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /** 手机号打码。 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
