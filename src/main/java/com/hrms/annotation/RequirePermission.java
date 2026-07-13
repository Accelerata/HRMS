package com.hrms.annotation;

import java.lang.annotation.*;

/**
 * 功能权限注解
 * 标记在 Controller 方法上，由 PermissionAspect 校验当前用户是否持有指定权限
 *
 * 用法：
 *   @RequirePermission("dept:view")
 *   @RequirePermission({"dept:manage", "system:admin"})  // 满足任一即可
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限码（如 dept:view, emp:salary:view）。多个值满足任一即可 */
    String[] value();

    /** 是否需要全部满足（默认 false = 满足任一即可） */
    boolean requireAll() default false;
}
