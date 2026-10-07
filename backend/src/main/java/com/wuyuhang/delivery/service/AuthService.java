package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.dto.LoginDTO;
import com.wuyuhang.delivery.dto.RegisterDTO;
import com.wuyuhang.delivery.entity.Station;
import com.wuyuhang.delivery.entity.SysPermission;
import com.wuyuhang.delivery.entity.SysRole;
import com.wuyuhang.delivery.entity.SysUser;
import com.wuyuhang.delivery.entity.SysUserRole;
import com.wuyuhang.delivery.mapper.StationMapper;
import com.wuyuhang.delivery.mapper.SysPermissionMapper;
import com.wuyuhang.delivery.mapper.SysRoleMapper;
import com.wuyuhang.delivery.mapper.SysUserMapper;
import com.wuyuhang.delivery.mapper.SysUserRoleMapper;
import com.wuyuhang.delivery.security.JwtUtil;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import com.wuyuhang.delivery.vo.LoginVO;
import com.wuyuhang.delivery.vo.MenuVO;
import com.wuyuhang.delivery.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务：登录、注册、获取当前用户信息与菜单。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final StationMapper stationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录。
     * <p>
     * 出于安全考虑，"用户不存在"与"密码错误"返回同一提示语，
     * 避免攻击者通过错误提示枚举系统内已存在的账号。
     */
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 查询角色与权限，随令牌一起下发
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermCodesByUserId(user.getId());

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .stationId(user.getStationId())
                .roles(roles)
                .permissions(permissions)
                .build();

        String token = jwtUtil.generateToken(loginUser);

        // 记录最近登录时间
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        log.info("用户 [{}] 登录成功，角色 {}", user.getUsername(), roles);
        return new LoginVO(token, jwtUtil.getExpiration(), buildUserInfo(user, roles, permissions));
    }

    /**
     * 注册普通用户。新用户默认只拥有 USER 角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.selectByUsername(dto.getUsername()) != null) {
            throw new BusinessException("该用户名已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // 密码使用 BCrypt 加密后落库，数据库中不保存明文
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setGender(0);
        user.setStatus(1);
        userMapper.insert(user);

        // 绑定默认角色 USER
        SysRole defaultRole = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, "USER").last("LIMIT 1"));
        if (defaultRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = permissionMapper.selectPermCodesByUserId(user.getId());
        log.info("新用户 [{}] 注册成功", user.getUsername());
        return buildUserInfo(user, roles, permissions);
    }

    /**
     * 获取当前登录用户信息（前端刷新页面后重新拉取用户状态）。
     */
    public UserInfoVO currentUser() {
        LoginUser loginUser = UserContext.require();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在或已被删除");
        }
        return buildUserInfo(user, loginUser.getRoles(), loginUser.getPermissions());
    }

    /**
     * 获取当前登录用户可见的菜单树，前端据此动态渲染侧边栏。
     */
    public List<MenuVO> currentMenus() {
        LoginUser loginUser = UserContext.require();
        List<SysPermission> permissions = permissionMapper.selectMenusByUserId(loginUser.getUserId());
        return buildMenuTree(permissions);
    }

    // ==================== 私有方法 ====================

    /**
     * 组装用户信息 VO。
     */
    private UserInfoVO buildUserInfo(SysUser user, List<String> roles, List<String> permissions) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setGender(user.getGender());
        vo.setAvatar(user.getAvatar());
        vo.setStationId(user.getStationId());
        if (user.getStationId() != null) {
            Station station = stationMapper.selectById(user.getStationId());
            if (station != null) {
                vo.setStationName(station.getStationName());
            }
        }
        vo.setRoles(roles == null ? List.of() : roles);
        vo.setPermissions(permissions == null ? List.of() : permissions);
        return vo;
    }

    /**
     * 把扁平的菜单列表组装成树形结构。
     * <p>
     * 时间复杂度 O(n)：先按父ID分组，再一次性挂载子节点，避免递归查询数据库。
     */
    private List<MenuVO> buildMenuTree(List<SysPermission> permissions) {
        Map<Long, MenuVO> voMap = new LinkedHashMap<>();
        for (SysPermission permission : permissions) {
            MenuVO vo = new MenuVO();
            vo.setId(permission.getId());
            vo.setPermCode(permission.getPermCode());
            vo.setPermName(permission.getPermName());
            vo.setPath(permission.getPath());
            vo.setIcon(permission.getIcon());
            voMap.put(permission.getId(), vo);
        }

        List<MenuVO> roots = new ArrayList<>();
        for (SysPermission permission : permissions) {
            MenuVO current = voMap.get(permission.getId());
            Long parentId = permission.getParentId();
            if (parentId == null || parentId == 0L || !voMap.containsKey(parentId)) {
                roots.add(current);
            } else {
                voMap.get(parentId).getChildren().add(current);
            }
        }
        return roots;
    }
}
