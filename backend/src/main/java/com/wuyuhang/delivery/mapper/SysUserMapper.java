package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按登录账号查询用户（含已逻辑删除的会被自动过滤）。
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 统计某驿站下的在岗员工数量，删除驿站前做校验用。
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE station_id = #{stationId} AND deleted = 0")
    int countByStationId(@Param("stationId") Long stationId);

    /**
     * 查询某用户拥有的角色编码集合。
     */
    @Select("""
            SELECT r.role_code
            FROM sys_user_role ur
                     JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
