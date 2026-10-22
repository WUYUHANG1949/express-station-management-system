package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.ForgotPasswordResetDTO;
import com.wuyuhang.delivery.dto.ForgotPasswordSendDTO;
import com.wuyuhang.delivery.dto.LoginDTO;
import com.wuyuhang.delivery.service.AuthService;
import com.wuyuhang.delivery.service.SmsCodeService;
import com.wuyuhang.delivery.vo.ForgotPasswordSendVO;
import com.wuyuhang.delivery.vo.LoginVO;
import com.wuyuhang.delivery.vo.MenuVO;
import com.wuyuhang.delivery.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录、注册（已关闭）、找回密码、退出、获取当前用户与菜单。
 *
 * @author 吴宇航
 */
@Tag(name = "01-认证管理", description = "登录、找回密码、退出与当前用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsCodeService smsCodeService;

    @Operation(summary = "用户登录", description = "登录成功返回 JWT 令牌与用户权限集合")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /**
     * 自助注册已关闭。
     * <p>
     * 收件人不需要账号：改用免登录自助查询（{@code /api/public/pickup-query}
     * 支持手机号 / 取件码 / 运单号）。系统内的登录账号由管理员在「用户管理」中创建，
     * 因此不再开放注册入口。
     * <p>
     * 这里保留路径并明确返回提示，而不是直接删掉映射：
     * 老版本前端或书签访问时能拿到可读的说明，而不是 404。
     */
    @Operation(summary = "用户注册（已关闭）",
            description = "自助注册已下线：收件人无需账号，改用免登录自助查询；"
                    + "系统内账号由管理员创建")
    @PostMapping("/register")
    public Result<Void> register() {
        throw new BusinessException("自助注册已关闭：收件人无需账号，请直接使用自助查询；"
                + "系统内账号请联系管理员创建");
    }

    /**
     * 找回密码第一步：申请验证码。
     * <p>
     * 免登录接口。必须同时提交账号名与手机号，且两者与数据库记录一致才发码，
     * 避免"知道用户名就能重置他人密码"。
     * <p>
     * 发送通道由 {@code sms.provider} 决定（mock / mail / aliyun）：
     * 默认 mock 只写日志并把验证码回传到响应里，便于没有短信网关时完整演示；
     * 切到 mail 后验证码会真实投递到配置的演示邮箱。
     */
    @Operation(summary = "找回密码-申请验证码",
            description = "校验账号名与手机号是否匹配，匹配则发送 6 位验证码（5 分钟有效，带发送限流）")
    @PostMapping("/forgot-password/code")
    public Result<ForgotPasswordSendVO> forgotPasswordCode(@Valid @RequestBody ForgotPasswordSendDTO dto,
                                                          HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return Result.success("验证码已发送",
                smsCodeService.sendCode(dto, ip));
    }

    /**
     * 找回密码第二步：校验验证码并重置密码。
     * <p>
     * 免登录接口。验证码一次性使用、5 分钟过期、错误累计 5 次作废。
     */
    @Operation(summary = "找回密码-重置密码",
            description = "校验验证码后重置密码；验证码一次性使用，错误次数超限即失效")
    @PostMapping("/forgot-password/reset")
    public Result<Void> forgotPasswordReset(@Valid @RequestBody ForgotPasswordResetDTO dto) {
        smsCodeService.resetPassword(dto);
        return Result.success("密码已重置，请用新密码登录", null);
    }

    @Operation(summary = "退出登录", description = "JWT 为无状态令牌，前端清除本地令牌即可")    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("已退出登录", null);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "获取当前用户菜单树", description = "前端据此动态渲染侧边栏")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.success(authService.currentMenus());
    }
}
