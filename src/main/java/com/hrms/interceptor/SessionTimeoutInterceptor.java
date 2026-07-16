package com.hrms.interceptor;

import com.hrms.common.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 会话超时拦截器
 *
 * 在 JwtTokenInterceptor 之后执行，通过 Redis 记录用户最后活跃时间。
 * 超过 30 分钟无操作则判定会话过期，返回 401 强制重新登录。
 * Redis 不可用时降级放行（记录告警日志），避免全站不可用。
 */
@Slf4j
@Component
public class SessionTimeoutInterceptor implements HandlerInterceptor {

    private static final String SESSION_KEY_PREFIX = "session:active:";
    private static final long SESSION_TIMEOUT_SECONDS = 30 * 60; // 30分钟

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // 预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Long userId = BaseContext.getCurrentUserId();
        if (userId == null) {
            // 未认证（JWT拦截器未通过），由JWT拦截器处理
            return true;
        }

        String sessionKey = SESSION_KEY_PREFIX + userId;

        try {
            // 检查会话是否超时
            Boolean hasActive = redisTemplate.hasKey(sessionKey);
            if (hasActive == null || !hasActive) {
                // 会话不存在或已过期 → 需要重新登录
                log.info("会话超时: userId={}", userId);
                response.setStatus(440); // 440 Login Timeout (非标准但常用的会话超时码)
                response.setContentType("application/json;charset=UTF-8");
                try {
                    response.getWriter().write("{\"code\":440,\"message\":\"会话已超时，请重新登录\"}");
                } catch (Exception ignored) {
                }
                return false;
            }

            // 刷新 TTL（每次请求延长 30 分钟）
            redisTemplate.expire(sessionKey, SESSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (Exception e) {
            // Redis 不可用时降级放行，避免全站不可用
            log.error("会话超时检查失败(Redis异常)，降级放行: userId={}, error={}", userId, e.getMessage());
        }

        return true;
    }

    /**
     * 登录成功后创建会话
     */
    public void createSession(Long userId) {
        if (userId == null) return;
        String sessionKey = SESSION_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(sessionKey, System.currentTimeMillis(),
                    SESSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("会话已创建: userId={}, ttl={}s", userId, SESSION_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.error("会话创建失败(Redis异常): userId={}", userId, e);
        }
    }

    /**
     * 登出时销毁会话
     */
    public void destroySession(Long userId) {
        if (userId == null) return;
        String sessionKey = SESSION_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(sessionKey);
            log.info("会话已销毁: userId={}", userId);
        } catch (Exception e) {
            log.error("会话销毁失败(Redis异常): userId={}", userId, e);
        }
    }
}
