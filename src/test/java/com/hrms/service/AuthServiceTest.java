package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.LoginDTO;
import com.hrms.entity.SysRole;
import com.hrms.entity.SysUser;
import com.hrms.mapper.SysPermissionMapper;
import com.hrms.mapper.SysRoleMapper;
import com.hrms.mapper.SysUserMapper;
import com.hrms.utils.PasswordUtil;
import com.hrms.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * AuthService 单元测试
 * TDD RED 阶段 —— 先写测试，确保测试因功能缺失而失败
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 登录认证服务")
class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysPermissionMapper sysPermissionMapper;

    @InjectMocks
    private AuthService authService;

    private SysUser mockUser;
    private SysRole mockRole;

    @BeforeEach
    void setUp() {
        // 注入 @Value 字段（Mockito 不会自动注入 Spring 的 @Value）
        ReflectionTestUtils.setField(authService, "secretKey",
                "test-secret-key-for-unit-test-must-be-256-bits!!");
        ReflectionTestUtils.setField(authService, "ttlMillis", 86400000L);

        // 构建模拟用户
        mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername("zhangsan");
        mockUser.setPassword(PasswordUtil.encode("ValidPass1"));
        mockUser.setStatus(1); // 正常
        mockUser.setPwdUpdateTime(LocalDateTime.now().minusDays(30));

        // 构建模拟角色 (HR 专员)
        mockRole = new SysRole();
        mockRole.setId(1L);
        mockRole.setRoleName("HR专员");
        mockRole.setRoleCode("ROLE_HR");
        mockRole.setDataScope(2); // 全部员工
        mockRole.setStatus(1);
    }

    /** 便捷方法：Mock 权限查询 */
    private void mockPermissions(List<String> permissions) {
        when(sysPermissionMapper.findPermissionCodesByUserId(anyLong()))
                .thenReturn(permissions != null ? permissions : List.of());
    }

    // ═══════════════ 成功登录 ═══════════════

    @Nested
    @DisplayName("成功登录场景")
    class SuccessfulLogin {

        @Test
        @DisplayName("HR 专员使用正确密码登录成功，Token 包含完整 Claims")
        void shouldLoginSuccessfullyAndReturnToken() {
            // Given
            LoginDTO dto = new LoginDTO();
            dto.setUsername("zhangsan");
            dto.setPassword("ValidPass1");

            when(sysUserMapper.findByUsername("zhangsan")).thenReturn(mockUser);
            when(sysRoleMapper.findByUserId(1L)).thenReturn(mockRole);
            mockPermissions(List.of("dept:view", "emp:view"));

            // When
            LoginVO result = authService.login(dto);

            // Then
            assertNotNull(result, "登录结果不应为 null");
            assertNotNull(result.getToken(), "Token 不应为 null");
            assertEquals(1L, result.getUserId());
            assertEquals("zhangsan", result.getUsername());
            assertEquals("ROLE_HR", result.getRoleCode());
            assertEquals("HR专员", result.getRoleName());
            assertEquals(2, result.getDataScope());
            assertNotNull(result.getPermissions());
            assertTrue(result.getPermissions().contains("dept:view"));
        }

        @Test
        @DisplayName("管理员（无 employeeId）登录成功")
        void shouldLoginAdminWithoutEmployeeId() {
            mockUser.setId(2L);
            mockUser.setUsername("admin");
            mockUser.setPassword(PasswordUtil.encode("AdminPass1"));
            mockRole.setRoleCode("ROLE_ADMIN");
            mockRole.setRoleName("系统管理员");
            mockRole.setDataScope(1);

            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            dto.setPassword("AdminPass1");

            when(sysUserMapper.findByUsername("admin")).thenReturn(mockUser);
            when(sysRoleMapper.findByUserId(2L)).thenReturn(mockRole);
            mockPermissions(List.of("*"));

            LoginVO result = authService.login(dto);

            assertNotNull(result);
            assertEquals("admin", result.getUsername());
            assertEquals("ROLE_ADMIN", result.getRoleCode());
            assertEquals(1, result.getDataScope());
            assertNull(result.getEmployeeId(), "管理员可能没有关联的员工ID");
        }
    }

    // ═══════════════ 登录失败 ═══════════════

    @Nested
    @DisplayName("登录失败场景")
    class FailedLogin {

        @Test
        @DisplayName("用户名不存在应抛出异常")
        void shouldThrowWhenUserNotFound() {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("nonexistent");
            dto.setPassword("SomePass1");

            when(sysUserMapper.findByUsername("nonexistent")).thenReturn(null);

            BaseException ex = assertThrows(BaseException.class,
                    () -> authService.login(dto),
                    "用户名不存在应抛出 BaseException");
            assertTrue(ex.getMessage().contains("用户名") || ex.getMessage().contains("密码"),
                    "错误消息应提示用户名或密码错误");
        }

        @Test
        @DisplayName("密码错误应抛出异常")
        void shouldThrowWhenPasswordWrong() {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("zhangsan");
            dto.setPassword("WrongPass1");

            when(sysUserMapper.findByUsername("zhangsan")).thenReturn(mockUser);

            BaseException ex = assertThrows(BaseException.class,
                    () -> authService.login(dto),
                    "密码错误应抛出 BaseException");
            assertTrue(ex.getMessage().contains("用户名") || ex.getMessage().contains("密码"),
                    "错误消息应提示用户名或密码错误");
        }

        @Test
        @DisplayName("账号被禁用应抛出异常")
        void shouldThrowWhenAccountDisabled() {
            mockUser.setStatus(0); // 禁用

            LoginDTO dto = new LoginDTO();
            dto.setUsername("zhangsan");
            dto.setPassword("ValidPass1");

            when(sysUserMapper.findByUsername("zhangsan")).thenReturn(mockUser);

            BaseException ex = assertThrows(BaseException.class,
                    () -> authService.login(dto),
                    "禁用账号应抛出 BaseException");
            assertTrue(ex.getMessage().contains("禁用") || ex.getMessage().contains("停用"),
                    "错误消息应提示账号已禁用: " + ex.getMessage());
        }
    }

    // ═══════════════ Token 校验 ═══════════════

    @Nested
    @DisplayName("Token 生成校验")
    class TokenValidation {

        @Test
        @DisplayName("生成的 Token 应为非空字符串且包含三段式结构")
        void tokenShouldHaveJwtFormat() {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("zhangsan");
            dto.setPassword("ValidPass1");

            when(sysUserMapper.findByUsername("zhangsan")).thenReturn(mockUser);
            when(sysRoleMapper.findByUserId(1L)).thenReturn(mockRole);
            mockPermissions(List.of("dept:view"));

            LoginVO result = authService.login(dto);

            assertNotNull(result.getToken());
            String[] parts = result.getToken().split("\\.");
            assertEquals(3, parts.length, "JWT Token 应包含 Header.Payload.Signature 三段");
        }

        @Test
        @DisplayName("不同用户登录应生成不同的 Token")
        void differentUsersShouldHaveDifferentTokens() {
            SysUser user2 = new SysUser();
            user2.setId(3L);
            user2.setUsername("lisi");
            user2.setPassword(PasswordUtil.encode("TestPass2"));
            user2.setStatus(1);

            SysRole role2 = new SysRole();
            role2.setId(2L);
            role2.setRoleName("普通员工");
            role2.setRoleCode("ROLE_EMPLOYEE");
            role2.setDataScope(5);
            role2.setStatus(1);

            LoginDTO dto1 = new LoginDTO();
            dto1.setUsername("zhangsan");
            dto1.setPassword("ValidPass1");

            LoginDTO dto2 = new LoginDTO();
            dto2.setUsername("lisi");
            dto2.setPassword("TestPass2");

            when(sysUserMapper.findByUsername("zhangsan")).thenReturn(mockUser);
            when(sysRoleMapper.findByUserId(1L)).thenReturn(mockRole);
            when(sysUserMapper.findByUsername("lisi")).thenReturn(user2);
            when(sysRoleMapper.findByUserId(3L)).thenReturn(role2);
            mockPermissions(List.of("dept:view"));

            LoginVO result1 = authService.login(dto1);
            LoginVO result2 = authService.login(dto2);

            assertNotEquals(result1.getToken(), result2.getToken(),
                    "不同用户应生成不同的 Token");
        }
    }
}
