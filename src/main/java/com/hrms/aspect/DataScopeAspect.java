package com.hrms.aspect;

import com.hrms.annotation.DataScope;
import com.hrms.common.context.BaseContext;
import com.hrms.utils.DataScopeHelper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据权限 AOP 切面
 *
 * 拦截 @DataScope 注解的方法，自动生成 SQL 数据隔离条件，
 * 写入方法参数的 "dataScopeSql" 键（如果参数是 Map）或
 * 调用 setter 方法注入（如果参数是 POJO）。
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    /**
     * 切点：所有带 @DataScope 注解的方法
     */
    @Pointcut("@annotation(com.hrms.annotation.DataScope)")
    public void dataScopePointcut() {
    }

    @Around("dataScopePointcut() && @annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {

        // 根据当前用户的数据权限生成 SQL 过滤条件
        String sqlFilter = DataScopeHelper.getSqlFilter(
                dataScope.deptAlias(),
                dataScope.deptColumn(),
                dataScope.userColumn()
        );

        log.debug("DataScope SQL filter: [{}] for user={}, role={}, scope={}",
                sqlFilter,
                BaseContext.getCurrentUsername(),
                BaseContext.getCurrentRoleCode(),
                BaseContext.getDataScope());

        // 将 SQL 过滤条件注入到方法参数中
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) arg;
                paramMap.put("dataScopeSql", sqlFilter);
            }
        }

        return joinPoint.proceed(args);
    }
}
