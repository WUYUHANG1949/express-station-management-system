package com.wuyuhang.delivery.security;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.ResultCode;

/**
 * 当前请求的登录用户上下文。
 * <p>
 * 由 {@link JwtInterceptor} 在请求进入时写入，请求结束时必须调用 {@link #clear()} 清理，
 * 否则在 Tomcat 线程池复用线程的场景下会造成用户信息串号（越权）。
 *
 * @author 吴宇航
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 写入当前登录用户。
     */
    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    /**
     * 获取当前登录用户，未登录返回 null。
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 获取当前登录用户，未登录直接抛出 401 业务异常。
     */
    public static LoginUser require() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    /**
     * 当前登录用户ID，未登录抛出 401。
     */
    public static Long userId() {
        return require().getUserId();
    }

    /**
     * 当前登录用户姓名，未登录抛出 401。
     */
    public static String realName() {
        return require().getRealName();
    }

    /**
     * 当前登录用户所属驿站ID，可能为 null（管理员或普通用户）。
     */
    public static Long stationId() {
        return require().getStationId();
    }

    /**
     * 清理上下文，必须在请求结束时调用。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
