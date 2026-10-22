package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.LoginDTO;
import com.wuyuhang.delivery.service.AuthService;
import com.wuyuhang.delivery.vo.LoginVO;
import com.wuyuhang.delivery.vo.MenuVO;
import com.wuyuhang.delivery.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证接口：登录、注册、退出、获取当前用户与菜单。
 *
 * @author 吴宇航
 */
@Tag(name = "01-认证管理", description = "登录、注册、退出与当前用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @Operation(summary = "退出登录", description = "JWT 为无状态令牌，前端清除本地令牌即可")
    @PostMapping("/logout")
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
