package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.LoginDTO;
import com.wuyuhang.delivery.dto.RegisterDTO;
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

    @Operation(summary = "用户注册", description = "注册成功后默认分配「普通用户」角色")
    @PostMapping("/register")
    public Result<UserInfoVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功，请登录", authService.register(dto));
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
