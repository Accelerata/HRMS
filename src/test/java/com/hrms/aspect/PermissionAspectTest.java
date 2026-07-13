package com.hrms.aspect;

import com.hrms.annotation.RequirePermission;
import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PermissionAspect 功能权限校验测试
 * TDD RED 阶段
 */
@DisplayName("PermissionAspect 功能权限校验")
class PermissionAspectTest {

    @BeforeEach
    void setUp() {
        BaseContext.clear();
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    // ═══════════════ 权限检查核心逻辑 ═══════════════

    @Nested
    @DisplayName("单权限校验")
    class SinglePermission {

        @Test
        @DisplayName("持有权限时应通过")
        void shouldPassWhenHasPermission() {
            BaseContext.setPermissions(Set.of("dept:view", "emp:view"));

            assertDoesNotThrow(() ->
                    PermissionAspect.checkPermission(new String[]{"dept:view"}, false));
        }

        @Test
        @DisplayName("未持有权限时应抛出 403")
        void shouldThrow403WhenNoPermission() {
            BaseContext.setPermissions(Set.of("emp:view"));

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(new String[]{"dept:view"}, false));

            assertEquals(403, ex.getCode());
            assertTrue(ex.getMessage().contains("权限不足") || ex.getMessage().contains("无权"));
        }

        @Test
        @DisplayName("权限列表为 null 时应抛出 403")
        void shouldThrow403WhenPermissionsNull() {
            BaseContext.setPermissions(null);

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(new String[]{"dept:view"}, false));

            assertEquals(403, ex.getCode());
        }

        @Test
        @DisplayName("权限列表为空时应抛出 403")
        void shouldThrow403WhenPermissionsEmpty() {
            BaseContext.setPermissions(Set.of());

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(new String[]{"dept:view"}, false));

            assertEquals(403, ex.getCode());
        }
    }

    // ═══════════════ 多权限 OR 校验 ═══════════════

    @Nested
    @DisplayName("多权限 OR 校验（满足任一即可）")
    class OrPermission {

        @Test
        @DisplayName("持有其一即可通过")
        void shouldPassWhenHasOneOfMany() {
            BaseContext.setPermissions(Set.of("emp:view"));

            assertDoesNotThrow(() ->
                    PermissionAspect.checkPermission(
                            new String[]{"dept:manage", "emp:view", "system:admin"}, false));
        }

        @Test
        @DisplayName("一个都未持有时应抛出 403")
        void shouldThrowWhenHasNone() {
            BaseContext.setPermissions(Set.of("emp:view"));

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(
                            new String[]{"dept:manage", "system:admin"}, false));

            assertEquals(403, ex.getCode());
        }
    }

    // ═══════════════ 多权限 AND 校验 ═══════════════

    @Nested
    @DisplayName("多权限 AND 校验（需全部满足）")
    class AndPermission {

        @Test
        @DisplayName("全部持有时应通过")
        void shouldPassWhenHasAll() {
            BaseContext.setPermissions(Set.of("dept:view", "dept:manage", "emp:view"));

            assertDoesNotThrow(() ->
                    PermissionAspect.checkPermission(
                            new String[]{"dept:view", "dept:manage"}, true));
        }

        @Test
        @DisplayName("缺失其一时应抛出 403")
        void shouldThrowWhenMissingOne() {
            BaseContext.setPermissions(Set.of("dept:view"));

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(
                            new String[]{"dept:view", "dept:manage"}, true));

            assertEquals(403, ex.getCode());
        }
    }

    // ═══════════════ 管理员拥有所有权限 ═══════════════

    @Nested
    @DisplayName("管理员超级权限")
    class AdminSuperPermission {

        @Test
        @DisplayName("管理员（dataScope=1）应拥有所有权限")
        void adminShouldHaveAllPermissions() {
            BaseContext.setDataScope(1); // 系统管理员

            assertDoesNotThrow(() ->
                    PermissionAspect.checkPermission(
                            new String[]{"dept:manage", "salary:audit", "system:admin"}, true));
        }

        @Test
        @DisplayName("管理员即使 permissions 为空也能通过")
        void adminShouldPassEvenWithEmptyPermissions() {
            BaseContext.setDataScope(1);
            BaseContext.setPermissions(Set.of());

            assertDoesNotThrow(() ->
                    PermissionAspect.checkPermission(new String[]{"any:permission"}, false));
        }
    }

    // ═══════════════ 边界情况 ═══════════════

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("要求权限码为 null 时应拒绝")
        void shouldRejectNullRequiredPermission() {
            BaseContext.setPermissions(Set.of("dept:view"));

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(null, false));

            assertEquals(403, ex.getCode());
        }

        @Test
        @DisplayName("权限码大小写敏感")
        void shouldBeCaseSensitive() {
            BaseContext.setPermissions(Set.of("DEPT:VIEW")); // 大写

            BaseException ex = assertThrows(BaseException.class, () ->
                    PermissionAspect.checkPermission(new String[]{"dept:view"}, false));

            assertEquals(403, ex.getCode(), "权限码应大小写敏感");
        }
    }
}
