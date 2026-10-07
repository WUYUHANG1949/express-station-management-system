package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限关联 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 清空某角色的全部权限（重新分配前先删除）。
     */
    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 清空引用了某权限的关联记录（删除权限前调用）。
     */
    @Delete("DELETE FROM sys_role_permission WHERE perm_id = #{permId}")
    int deleteByPermId(@Param("permId") Long permId);
}
