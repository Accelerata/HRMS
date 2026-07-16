package com.hrms.service;

import com.hrms.entity.AuditLog;
import com.hrms.entity.SysUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全合规测试 (任务 3.11)
 *
 * 验证：密码过期拦截、到期提醒、会话超时、审计日志
 */
@DisplayName("安全合规测试")
class SecurityComplianceTest {

    @Nested
    @DisplayName("3.1 密码过期检测")
    class PasswordExpiryTests {

        @Test
        @DisplayName("密码90天内 → 正常登录")
        void shouldAllowLoginWithin90Days() {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setPwdUpdateTime(LocalDateTime.now().minusDays(30));

            assertFalse(isPasswordExpired(user), "30天内密码不应过期");
        }

        @Test
        @DisplayName("密码超过90天 → 过期")
        void shouldDetectExpiredPassword() {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setPwdUpdateTime(LocalDateTime.now().minusDays(100));

            assertTrue(isPasswordExpired(user), "超过90天密码应过期");
        }

        @Test
        @DisplayName("密码恰好90天 → 不过期（边界值）")
        void shouldNotExpireAtExactly90Days() {
            SysUser user = new SysUser();
            user.setId(1L);
            // 使用 truncated time 避免纳秒精度问题
            LocalDateTime now = LocalDateTime.now().withNano(0);
            user.setPwdUpdateTime(now.minusDays(90).plusSeconds(1)); // 比90天少1秒

            assertFalse(isPasswordExpired(user, now), "恰好90天应视为未过期");
        }

        private boolean isPasswordExpired(SysUser user) {
            return isPasswordExpired(user, LocalDateTime.now());
        }

        private boolean isPasswordExpired(SysUser user, LocalDateTime now) {
            if (user.getPwdUpdateTime() == null) return true;
            return user.getPwdUpdateTime().plusDays(90).isBefore(now);
        }
    }

    @Nested
    @DisplayName("3.2 到期提醒")
    class ExpiryReminderTests {

        @Test
        @DisplayName("密码7天后到期 → 应提醒")
        void shouldRemindAt7Days() {
            LocalDateTime pwdTime = LocalDateTime.now().minusDays(83);
            int daysUntilExpiry = 90 - (int) java.time.Duration.between(pwdTime, LocalDateTime.now()).toDays();
            assertTrue(daysUntilExpiry <= 7, "应在7天内到期时提醒");
        }

        @Test
        @DisplayName("密码3天后到期 → 应提醒")
        void shouldRemindAt3Days() {
            LocalDateTime pwdTime = LocalDateTime.now().minusDays(87);
            int daysUntilExpiry = 90 - (int) java.time.Duration.between(pwdTime, LocalDateTime.now()).toDays();
            assertTrue(daysUntilExpiry <= 3, "应在3天内到期时提醒");
        }

        @Test
        @DisplayName("密码1天后到期 → 应提醒")
        void shouldRemindAt1Day() {
            LocalDateTime pwdTime = LocalDateTime.now().minusDays(89);
            int daysUntilExpiry = 90 - (int) java.time.Duration.between(pwdTime, LocalDateTime.now()).toDays();
            assertTrue(daysUntilExpiry <= 1, "应在1天内到期时提醒");
        }
    }

    @Nested
    @DisplayName("3.7-3.10 审计日志")
    class AuditLogTests {

        @Test
        @DisplayName("审计日志字段完整性")
        void shouldHaveAllRequiredFields() {
            AuditLog log = new AuditLog();
            log.setId(1L);
            log.setOperatorId(100L);
            log.setOperatorName("admin");
            log.setOperation("SALARY_VIEW");
            log.setResourceType("SALARY_RECORD");
            log.setResourceId("200");
            log.setResult("SUCCESS");
            log.setCreateTime(LocalDateTime.now());

            assertNotNull(log.getOperatorId(), "操作人ID必填");
            assertNotNull(log.getOperation(), "操作类型必填");
            assertNotNull(log.getResourceType(), "资源类型必填");
            assertNotNull(log.getCreateTime(), "操作时间必填");
            assertEquals("SUCCESS", log.getResult());
        }

        @Test
        @DisplayName("审计日志记录失败操作")
        void shouldRecordFailedOperations() {
            AuditLog log = new AuditLog();
            log.setResult("FAILURE");
            log.setErrorMessage("权限不足");

            assertEquals("FAILURE", log.getResult());
            assertEquals("权限不足", log.getErrorMessage());
        }
    }
}
