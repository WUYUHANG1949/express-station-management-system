package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.RolePermissionDTO;
import com.wuyuhang.delivery.dto.RoleSaveDTO;
import com.wuyuhang.delivery.entity.SysPermission;
import com.wuyuhang.delivery.entity.SysRole;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.RoleService;
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

import java.util.List;

/**
 * 角色与权限管理接口。
 *
 * @author 吴宇航
 */
@Tag(name = "03-角色权限", description = "角色维护与权限分配")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "查询全部启用角色", description = "供下拉框使用，所有登录用户可读")
    @GetMapping("/roles/list")
    public Result<List<SysRole>> list() {
        return Result.success(roleService.list());
    }

    @Operation(summary = "分页查询角色")
    @GetMapping("/roles/page")
    @RequiresPermission("system:role:list")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(roleService.page(pageNum, pageSize));
    }

    @Operation(summary = "新增角色")
    @PostMapping("/roles")
    @RequiresPermission("system:role:list")
    public Result<Long> create(@Valid @RequestBody RoleSaveDTO dto) {
        return Result.success("新增成功", roleService.create(dto));
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/roles/{id}")
    @RequiresPermission("system:role:list")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveDTO dto) {
        roleService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{id}")
    @RequiresPermission("system:role:list")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "查询角色已分配的权限ID")
    @GetMapping("/roles/{id}/permissions")
    @RequiresPermission("system:role:list")
    public Result<List<Long>> permissions(@PathVariable Long id) {
        return Result.success(roleService.getPermissionIds(id));
    }

    @Operation(summary = "为角色分配权限")
    @PutMapping("/roles/{id}/permissions")
    @RequiresPermission("system:role:assign")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody RolePermissionDTO dto) {
        roleService.assignPermissions(id, dto.getPermIds());
        return Result.success("权限分配成功，相关用户重新登录后生效", null);
    }

    @Operation(summary = "查询权限树", description = "type=MENU 只返回菜单，其它值返回菜单与按钮")
    @GetMapping("/permissions/tree")
    @RequiresPermission({"system:role:list", "system:user:list"})
    public Result<List<SysPermission>> permissionTree(@RequestParam(defaultValue = "ALL") String type) {
        return Result.success(roleService.permissionTree(type, true));
    }
}
