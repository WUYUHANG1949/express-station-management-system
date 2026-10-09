package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.dto.ChangePasswordDTO;
import com.wuyuhang.delivery.dto.ProfileDTO;
import com.wuyuhang.delivery.dto.UserSaveDTO;
import com.wuyuhang.delivery.dto.query.UserQuery;
import com.wuyuhang.delivery.entity.Station;
import com.wuyuhang.delivery.entity.SysRole;
import com.wuyuhang.delivery.entity.SysUser;
import com.wuyuhang.delivery.entity.SysUserRole;
import com.wuyuhang.delivery.mapper.StationMapper;
import com.wuyuhang.delivery.mapper.SysRoleMapper;
import com.wuyuhang.delivery.mapper.SysUserMapper;
import com.wuyuhang.delivery.mapper.SysUserRoleMapper;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import com.wuyuhang.delivery.vo.UserVO;
import com.wuyuhang.delivery.vo.UserStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户管理服务。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** 新建用户未指定密码时的默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final StationMapper stationMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表，并批量填充角色信息。
     */
    public PageResult<UserVO> page(long pageNum, long pageSize, UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
                .like(StringUtils.hasText(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .eq(query.getStationId() != null, SysUser::getStationId, query.getStationId());

        // 按角色编码过滤：使用参数占位符 {0}，由 MyBatis 预编译，不存在 SQL 注入风险
        if (StringUtils.hasText(query.getRoleCode())) {
            wrapper.apply("id IN (SELECT ur.user_id FROM sys_user_role ur "
                            + "JOIN sys_role r ON r.id = ur.role_id WHERE r.role_code = {0})",
                    query.getRoleCode());
        }
        wrapper.orderByAsc(SysUser::getId);

        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> voList = convertWithRoles(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), voList);
    }

    /**
     * 新增用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserSaveDTO dto) {
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("该用户名已被占用");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        String rawPassword = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setStationId(dto.getStationId());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userMapper.insert(user);

        saveUserRoles(user.getId(), dto.getRoleIds());
        log.info("新增用户 [{}]，初始角色 {}", dto.getUsername(), dto.getRoleIds());
        return user.getId();
    }

    /**
     * 编辑用户。密码留空表示不修改。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserSaveDTO dto) {
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        // 用户名不允许修改，避免破坏历史操作记录的可追溯性
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setStationId(dto.getStationId());
        user.setStatus(dto.getStatus());
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
            saveUserRoles(id, dto.getRoleIds());
        }
        log.info("编辑用户 [{}]", exist.getUsername());
    }

    /**
     * 逻辑删除用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (UserContext.userId().equals(id)) {
            throw new BusinessException("不能删除当前登录的账号");
        }
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        log.info("删除用户 [{}]", exist.getUsername());
    }

    /**
     * 启用 / 禁用账号。
     */
    public void updateStatus(Long id, Integer status) {
        if (UserContext.userId().equals(id)) {
            throw new BusinessException("不能禁用当前登录的账号");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 管理员重置某用户密码。
     */
    public void resetPassword(Long id, String password) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
        log.info("重置用户 [{}] 的密码", id);
    }

    /**
     * 修改本人资料。
     */
    public void updateProfile(ProfileDTO dto) {
        Long userId = UserContext.userId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
    }

    /**
     * 修改本人密码：必须先校验原密码。
     */
    public void changePassword(ChangePasswordDTO dto) {
        SysUser exist = userMapper.selectById(UserContext.userId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), exist.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        SysUser user = new SysUser();
        user.setId(exist.getId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        log.info("用户 [{}] 修改了自己的密码", exist.getUsername());
    }

    /**
     * 账号统计：供用户管理页顶部的统计卡片使用。
     */
    public UserStatVO stat() {
        UserStatVO vo = userMapper.selectUserStat();
        return vo == null ? new UserStatVO() : vo;
    }

    // ==================== 私有方法 ====================

    /**
     * 保存用户与角色的关联关系。
     */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds.stream().distinct().toList()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    /**
     * 批量把用户实体转换为 VO，并填充角色信息。
     * <p>
     * 采用「两次批量查询 + 内存分组」的方式，避免在循环里逐条查库（N+1 问题）。
     */
    private List<UserVO> convertWithRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> userIds = users.stream().map(SysUser::getId).toList();

        // 批量查询用户-角色关联
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, userIds));
        Map<Long, List<Long>> userRoleMap = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));

        // 批量查询角色
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, SysRole> roleMap = roleIds.isEmpty()
                ? Collections.emptyMap()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        // 批量查询驿站
        List<Long> stationIds = users.stream().map(SysUser::getStationId).filter(java.util.Objects::nonNull).toList();
        Map<Long, String> stationMap = stationIds.isEmpty()
                ? Collections.emptyMap()
                : stationMapper.selectBatchIds(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, Station::getStationName));

        List<UserVO> result = new ArrayList<>(users.size());
        for (SysUser user : users) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setGender(user.getGender());
            vo.setAvatar(user.getAvatar());
            vo.setStationId(user.getStationId());
            vo.setStationName(user.getStationId() == null ? null : stationMap.get(user.getStationId()));
            vo.setStatus(user.getStatus());
            vo.setLastLoginTime(user.getLastLoginTime());
            vo.setCreateTime(user.getCreateTime());

            List<Long> boundRoleIds = userRoleMap.getOrDefault(user.getId(), List.of());
            vo.setRoleIds(boundRoleIds);
            for (Long roleId : boundRoleIds) {
                SysRole role = roleMap.get(roleId);
                if (role != null) {
                    vo.getRoleCodes().add(role.getRoleCode());
                    vo.getRoleNames().add(role.getRoleName());
                }
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 供其它 Service 校验用户是否存在。
     */
    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 当前登录用户实体。
     */
    public SysUser currentUserEntity() {
        LoginUser loginUser = UserContext.require();
        return userMapper.selectById(loginUser.getUserId());
    }
}
