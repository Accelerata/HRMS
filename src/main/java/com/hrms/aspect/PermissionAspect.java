package com.hrms.aspect;

import com.hrms.annotation.RequirePermission;
import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 功能权限 AOP 切面
 * 拦截 @RequirePermission 注解，校验当前用户是否持有指定权限码
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    /**
     * 切点：方法上的 @RequirePermission
     */
    @Around("@annotation(requirePermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        checkPermission(requirePermission.value(), requirePermission.requireAll());
        return joinPoint.proceed();
    }

    /**
     * 权限校验核心逻辑（可被静态调用，方便单元测试）
     *
     * @param requiredCodes 需要的权限码
     * @param requireAll    是否需要全部满足
     */
    public static void checkPermission(String[] requiredCodes, boolean requireAll) {
        // 参数校验
        if (requiredCodes == null || requiredCodes.length == 0) {
            throw BaseException.forbidden("权限不足，无法访问");
        }

        // 系统管理员（dataScope=1）拥有所有权限
        Integer dataScope = BaseContext.getDataScope();
        if (dataScope != null && dataScope == 1) {
            return;
        }

        // 获取当前用户权限码集合
        Set<String> userPermissions = BaseContext.getPermissions();

        if (userPermissions == null || userPermissions.isEmpty()) {
            log.warn("权限拒绝: 用户无任何权限, 需要 {}",
                    String.join(",", requiredCodes));
            throw BaseException.forbidden("权限不足，无法访问该资源");
        }

        if (requireAll) {
            // 需全部满足
            for (String code : requiredCodes) {
                if (!userPermissions.contains(code)) {
                    log.warn("权限拒绝: 缺少 {} (需要全部: {})",
                            code, String.join(",", requiredCodes));
                    throw BaseException.forbidden("权限不足，无法访问该资源");
                }
            }
        } else {
            // 满足任一即可
            for (String code : requiredCodes) {
                if (userPermissions.contains(code)) {
                    return;
                }
            }
            log.warn("权限拒绝: 需要 {} 中任一项, 当前权限: {}",
                    String.join(",", requiredCodes),
                    String.join(",", userPermissions));
            throw BaseException.forbidden("权限不足，无法访问该资源");
        }
    }
}
