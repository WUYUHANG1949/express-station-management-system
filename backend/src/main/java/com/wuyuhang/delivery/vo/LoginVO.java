package com.wuyuhang.delivery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回 VO。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "登录结果")
public class LoginVO implements Serializable {

    @Schema(description = "JWT 令牌")
    private String token;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "有效期（秒）")
    private Long expiresIn;

    @Schema(description = "登录用户信息")
    private UserInfoVO userInfo;

    public LoginVO() {
    }

    public LoginVO(String token, Long expiresIn, UserInfoVO userInfo) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userInfo = userInfo;
    }
}
