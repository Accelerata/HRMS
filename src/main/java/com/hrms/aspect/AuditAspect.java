package com.hrms.aspect;

import com.hrms.annotation.Auditable;
import com.hrms.common.context.BaseContext;
import com.hrms.entity.AuditLog;
import com.hrms.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 审计日志切面
 *
 * 拦截 @Auditable 注解标注的 Controller 方法，
 * 方法成功返回 → 记录 SUCCESS
 * 方法抛出异常 → 记录 FAILURE
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;

    @AfterReturning(pointcut = "@annotation(com.hrms.annotation.Auditable)")
    public void afterSuccess(JoinPoint joinPoint) {
        recordAudit(joinPoint, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "@annotation(com.hrms.annotation.Auditable)", throwing = "ex")
    public void afterFailure(JoinPoint joinPoint, Exception ex) {
        recordAudit(joinPoint, "FAILURE", ex.getMessage());
    }

    private void recordAudit(JoinPoint joinPoint, String result, String errorMessage) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Auditable auditable = method.getAnnotation(Auditable.class);
            if (auditable == null) return;

            AuditLog auditLog = new AuditLog();
            auditLog.setOperatorId(BaseContext.getCurrentUserId());
            auditLog.setOperatorName(BaseContext.getCurrentUsername());
            auditLog.setOperation(auditable.operation());
            auditLog.setResourceType(auditable.resourceType());

            // 资源ID：优先取注解值，否则使用方法参数摘要
            String resourceId = auditable.resourceId();
            if (resourceId == null || resourceId.isEmpty()) {
                resourceId = Arrays.stream(joinPoint.getArgs())
                        .filter(arg -> arg instanceof Number || arg instanceof String)
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
            }
            auditLog.setResourceId(resourceId);

            // 请求参数摘要（截取前500字符）
            String params = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> arg != null)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            if (params != null && params.length() > 500) {
                params = params.substring(0, 500);
            }
            auditLog.setRequestSummary(params);

            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);

            // 客户端IP
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) {
                        ip = request.getRemoteAddr();
                    }
                    auditLog.setClientIp(ip);
                }
            } catch (Exception ignored) {
            }

            auditLogMapper.insert(auditLog);
            log.debug("审计日志记录成功: operation={}, resourceType={}, result={}",
                    auditable.operation(), auditable.resourceType(), result);
        } catch (Exception e) {
            log.error("审计日志记录失败", e);
        }
    }
}
