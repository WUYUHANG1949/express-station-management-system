package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.SysUser;
import com.wuyuhang.delivery.vo.UserStatVO;
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

    /**
     * 账号统计：一条 SQL 用多个标量子查询取回全部计数，
     * 供用户管理页顶部的统计卡片使用（避免前端为每个指标各发一次分页请求）。
     */
    @Select("""
            SELECT
                (SELECT COUNT(*) FROM sys_user WHERE deleted = 0) AS total,
                (SELECT COUNT(*) FROM sys_user u
                          JOIN sys_user_role ur ON ur.user_id = u.id
                          JOIN sys_role r ON r.id = ur.role_id
                  WHERE u.deleted = 0 AND r.role_code = 'ADMIN') AS adminCount,
                (SELECT COUNT(*) FROM sys_user u
                          JOIN sys_user_role ur ON ur.user_id = u.id
                          JOIN sys_role r ON r.id = ur.role_id
                  WHERE u.deleted = 0 AND r.role_code = 'STAFF') AS staffCount,
                (SELECT COUNT(*) FROM sys_user u
                          JOIN sys_user_role ur ON ur.user_id = u.id
                          JOIN sys_role r ON r.id = ur.role_id
                  WHERE u.deleted = 0 AND r.role_code = 'USER') AS userCount,
                (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 1) AS enabledCount,
                (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 0) AS disabledCount,
                (SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND station_id IS NULL) AS noStationCount
            """)
    UserStatVO selectUserStat();
}
