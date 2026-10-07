package com.wuyuhang.delivery.security;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证拦截器。
 * <p>
 * 职责：拦截所有需要登录的接口，校验请求头中的 JWT；
 * 校验通过后把用户信息写入 {@link UserContext}，供后续的权限切面与业务代码使用。
 * <p>
 * 注意：这里只做"认证"（你是谁），"鉴权"（你能不能做这件事）由
 * {@link RequiresPermission} 注解配合 {@link PermissionAspect} 完成。
 *
 * @author 吴宇航
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = jwtUtil.resolveToken(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录后再操作");
        }

        LoginUser loginUser = jwtUtil.parseToken(token);
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 线程复用场景下必须清理，避免用户信息串号
        UserContext.clear();
    }
}
