package com.wuyuhang.delivery.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解。
 * <p>
 * 标注在 Controller 的方法或类上，表示访问该接口需要拥有的权限标识。
 * 例如：{@code @RequiresPermission("parcel:in")} 表示只有拥有"收件登记"权限的角色才能调用。
 *
 * @author 吴宇航
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * 需要的权限标识，可写多个。
     */
    String[] value();

    /**
     * 多个权限之间的逻辑关系。
     */
    Logical logical() default Logical.AND;

    /**
     * 逻辑关系枚举。
     */
    enum Logical {
        /** 必须同时拥有全部权限 */
        AND,
        /** 拥有其中任意一个即可 */
        OR
    }
}
