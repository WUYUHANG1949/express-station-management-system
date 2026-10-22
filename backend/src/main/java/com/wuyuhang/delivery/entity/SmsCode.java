package com.wuyuhang.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验证码实体，对应表 sms_code。
 * <p>
 * 用于「忘记密码」场景：用户提交账号名与手机号，校验二者与数据库记录一致后，
 * 由 {@link com.wuyuhang.delivery.sms.SmsSender} 投递 6 位一次性验证码，
 * 再凭验证码重置密码。
 * <p>
 * 为什么不用 JWT 或内存缓存：验证码需要「有效期 + 错误次数 + 一次性」三个约束，
 * 落库后既可防止服务重启丢失，也便于管理员排查，同时不引入 Redis 这一额外部署依赖。
 *
 * @author 吴宇航
 */
@Data
@TableName("sms_code")
@Schema(description = "验证码")
public class SmsCode implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "接收手机号")
    private String phone;

    @Schema(description = "申请找回的账号名")
    private String username;

    @Schema(description = "验证码（6 位数字）")
    private String code;

    @Schema(description = "场景：PASSWORD_RESET")
    private String scene;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否已使用：0 未用 / 1 已用")
    private Integer used;

    @Schema(description = "校验失败次数")
    private Integer attempts;

    @Schema(description = "请求方 IP")
    private String clientIp;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
