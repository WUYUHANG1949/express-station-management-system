package com.wuyuhang.delivery.security;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 接口权限校验切面。
 * <p>
 * 拦截所有标注了 {@link RequiresPermission} 的方法（或类），
 * 校验当前登录用户的权限集合是否满足要求，不满足则抛出 403 业务异常。
 * 这样业务代码里就不需要再写重复的 if 判断，实现"权限与业务解耦"。
 *
 * @author 吴宇航
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(com.wuyuhang.delivery.security.RequiresPermission) "
            + "|| @within(com.wuyuhang.delivery.security.RequiresPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 方法上的注解优先，其次取类上的注解
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(RequiresPermission.class);
        }
        if (annotation == null) {
            return;
        }

        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录后再操作");
        }

        String[] required = annotation.value();
        boolean pass = annotation.logical() == RequiresPermission.Logical.AND
                ? Arrays.stream(required).allMatch(loginUser::hasPermission)
                : Arrays.stream(required).anyMatch(loginUser::hasPermission);

        if (!pass) {
            log.warn("用户 [{}] 越权访问 {}.{}，需要权限 {}",
                    loginUser.getUsername(),
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    Arrays.toString(required));
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
