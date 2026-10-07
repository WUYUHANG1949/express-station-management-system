package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper。
 * <p>
 * 用户最终权限 = 该用户所有角色所拥有权限的并集。
 *
 * @author 吴宇航
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询某用户拥有的全部权限标识（去重）。
     * <p>
     * 注意：MySQL 8 不允许 {@code SELECT DISTINCT} 搭配「不在 SELECT 列表中的 ORDER BY 列」，
     * 因此这里用 {@code GROUP BY perm_code ORDER BY MIN(p.sort)} 实现去重 + 按权限表排序。
     */
    @Select("""
            SELECT p.perm_code
            FROM sys_user_role ur
                     JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                     JOIN sys_permission p ON p.id = rp.perm_id
                     JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND p.status = 1
              AND r.status = 1
            GROUP BY p.perm_code
            ORDER BY MIN(p.sort)
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询某用户可见的菜单权限，按父级、排序号升序返回，Service 层再组装成树。
     */
    @Select("""
            SELECT DISTINCT p.*
            FROM sys_user_role ur
                     JOIN sys_role_permission rp ON rp.role_id = ur.role_id
                     JOIN sys_permission p ON p.id = rp.perm_id
                     JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND p.status = 1
              AND r.status = 1
              AND p.perm_type = 'MENU'
            ORDER BY p.parent_id, p.sort
            """)
    List<SysPermission> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 查询某角色已分配的权限ID集合。
     */
    @Select("SELECT perm_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermIdsByRoleId(@Param("roleId") Long roleId);
}
