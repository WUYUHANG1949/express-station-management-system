package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.ChangePasswordDTO;
import com.wuyuhang.delivery.dto.ProfileDTO;
import com.wuyuhang.delivery.dto.UserSaveDTO;
import com.wuyuhang.delivery.dto.query.UserQuery;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.UserService;
import com.wuyuhang.delivery.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口。
 *
 * @author 吴宇航
 */
@Tag(name = "02-用户管理", description = "用户的增删改查、状态与密码管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @RequiresPermission("system:user:list")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           UserQuery query) {
        return Result.success(userService.page(pageNum, pageSize, query));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @RequiresPermission("system:user:add")
    public Result<Long> create(@Valid @RequestBody UserSaveDTO dto) {
        return Result.success("新增成功", userService.create(dto));
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @RequiresPermission("system:user:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveDTO dto) {
        userService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @RequiresPermission("system:user:delete")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/禁用账号")
    @PutMapping("/{id}/status")
    @RequiresPermission("system:user:edit")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success(status == 1 ? "已启用" : "已禁用", null);
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    @RequiresPermission("system:user:reset")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.success("密码已重置", null);
    }

    @Operation(summary = "修改本人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.success("资料已更新", null);
    }

    @Operation(summary = "修改本人密码")
    @PutMapping("/self/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }
}
