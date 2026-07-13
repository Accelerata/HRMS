package com.hrms.interceptor;

import com.hrms.common.constant.JwtClaimsConstant;
import com.hrms.common.context.BaseContext;
import com.hrms.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT Token 拦截器
 * 1. 校验请求头中是否携带有效 Token
 * 2. 解析 Token 中的用户信息写入 ThreadLocal (BaseContext)
 * 3. Token 无效返回 401
 */
@Slf4j
@Component
public class JwtTokenInterceptor implements HandlerInterceptor {

    @Value("${jwt.admin-secret-key}")
    private String secretKey;

    @Value("${jwt.admin-token-name}")
    private String tokenName;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // 预检请求直接放行 (CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头获取 Token
        String token = request.getHeader(tokenName);
        if (token == null || token.isBlank()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            return false;
        }

        // 去掉 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 解析 Token
        try {
            Claims claims = JwtUtil.parseJWT(secretKey, token);

            Long userId = claims.get(JwtClaimsConstant.USER_ID, Long.class);
            String username = claims.get(JwtClaimsConstant.USERNAME, String.class);
            Long employeeId = claims.get(JwtClaimsConstant.EMPLOYEE_ID, Long.class);

            // 阶段二：角色与数据权限
            String roleCode = claims.get(JwtClaimsConstant.ROLE_CODE, String.class);
            Integer dataScope = claims.get(JwtClaimsConstant.DATA_SCOPE, Integer.class);
            Long deptId = claims.get(JwtClaimsConstant.DEPT_ID, Long.class);

            // 阶段三：功能权限码
            String permissionsStr = claims.get(JwtClaimsConstant.PERMISSIONS, String.class);
            Set<String> permissions = permissionsStr != null && !permissionsStr.isEmpty()
                    ? Arrays.stream(permissionsStr.split(","))
                            .map(String::trim)
                            .collect(Collectors.toSet())
                    : Set.of();

            // 写入 ThreadLocal
            BaseContext.setCurrentUserId(userId);
            BaseContext.setCurrentUsername(username);
            BaseContext.setCurrentEmployeeId(employeeId);
            BaseContext.setCurrentRoleCode(roleCode);
            BaseContext.setDataScope(dataScope);
            BaseContext.setCurrentDeptId(deptId);
            BaseContext.setPermissions(permissions);

            log.debug("JWT校验通过: userId={}, username={}, role={}, dataScope={}",
                    userId, username, roleCode, dataScope);
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            return false;

        } catch (SignatureException e) {
            log.warn("Token签名无效: {}", e.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            return false;

        } catch (Exception e) {
            log.error("Token解析异常: {}", e.getMessage());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            return false;
        }
    }

    /**
     * 请求结束后清理 ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        BaseContext.clear();
    }

}
