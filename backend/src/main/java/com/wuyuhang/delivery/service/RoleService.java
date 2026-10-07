package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.dto.RoleSaveDTO;
import com.wuyuhang.delivery.entity.SysPermission;
import com.wuyuhang.delivery.entity.SysRole;
import com.wuyuhang.delivery.entity.SysRolePermission;
import com.wuyuhang.delivery.entity.SysUserRole;
import com.wuyuhang.delivery.mapper.SysPermissionMapper;
import com.wuyuhang.delivery.mapper.SysRoleMapper;
import com.wuyuhang.delivery.mapper.SysRolePermissionMapper;
import com.wuyuhang.delivery.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色与权限管理服务。
 * <p>
 * 采用 RBAC 模型：用户 --(sys_user_role)--> 角色 --(sys_role_permission)--> 权限。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    /** 内置角色不允许删除，避免系统失去管理员 */
    private static final List<String> BUILT_IN_ROLES = List.of("ADMIN", "STAFF", "USER");

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    /**
     * 查询全部启用的角色，用于下拉框。
     */
    public List<SysRole> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSort));
    }

    /**
     * 分页查询角色。
     */
    public PageResult<SysRole> page(long pageNum, long pageSize) {
        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getSort));
        return PageResult.of(page);
    }

    /**
     * 新增角色，可同时分配权限。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleSaveDTO dto) {
        Long count = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (count != null && count > 0) {
            throw new BusinessException("该角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        roleMapper.insert(role);

        assignPermissions(role.getId(), dto.getPermIds());
        log.info("新增角色 [{}]", dto.getRoleCode());
        return role.getId();
    }

    /**
     * 编辑角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleSaveDTO dto) {
        SysRole exist = roleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("角色不存在");
        }
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setSort(dto.getSort());
        role.setStatus(dto.getStatus());
        roleMapper.updateById(role);

        if (dto.getPermIds() != null) {
            assignPermissions(id, dto.getPermIds());
        }
    }

    /**
     * 删除角色。内置角色与仍被用户引用的角色不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (BUILT_IN_ROLES.contains(role.getRoleCode())) {
            throw new BusinessException("内置角色不允许删除");
        }
        Long userCount = userRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException("该角色下仍有 " + userCount + " 个用户，无法删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.deleteByRoleId(id);
        log.info("删除角色 [{}]", role.getRoleCode());
    }

    /**
     * 查询某角色已分配的权限ID。
     */
    public List<Long> getPermissionIds(Long roleId) {
        return permissionMapper.selectPermIdsByRoleId(roleId);
    }

    /**
     * 重新分配角色权限：先清空再写入，保证勾选结果与数据库完全一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException("角色不存在");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permIds == null || permIds.isEmpty()) {
            return;
        }
        for (Long permId : permIds.stream().distinct().toList()) {
            if (permissionMapper.selectById(permId) == null) {
                continue;
            }
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermId(permId);
            rolePermissionMapper.insert(rolePermission);
        }
        log.info("角色 [{}] 权限已更新为 {}", roleId, permIds);
    }

    /**
     * 查询权限树。
     *
     * @param type       MENU 只返回菜单；其它值返回菜单 + 按钮
     * @param onlyEnabled 是否只返回启用状态的权限
     */
    public List<SysPermission> permissionTree(String type, boolean onlyEnabled) {
        var wrapper = Wrappers.<SysPermission>lambdaQuery();
        if ("MENU".equalsIgnoreCase(type)) {
            wrapper.eq(SysPermission::getPermType, "MENU");
        }
        if (onlyEnabled) {
            wrapper.eq(SysPermission::getStatus, 1);
        }
        wrapper.orderByAsc(SysPermission::getParentId).orderByAsc(SysPermission::getSort);
        List<SysPermission> all = permissionMapper.selectList(wrapper);

        Map<Long, SysPermission> map = new LinkedHashMap<>();
        for (SysPermission permission : all) {
            permission.setChildren(new ArrayList<>());
            map.put(permission.getId(), permission);
        }
        List<SysPermission> roots = new ArrayList<>();
        for (SysPermission permission : all) {
            Long parentId = permission.getParentId();
            if (parentId == null || parentId == 0L || !map.containsKey(parentId)) {
                roots.add(permission);
            } else {
                map.get(parentId).getChildren().add(permission);
            }
        }
        return roots;
    }

    /**
     * 查询角色被多少用户使用，删除前提示用。
     */
    public long countUsers(Long roleId) {
        Long count = userRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, roleId));
        return count == null ? 0L : count;
    }
}
