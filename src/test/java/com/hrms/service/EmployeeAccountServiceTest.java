package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.Employee;
import com.hrms.entity.SysRole;
import com.hrms.entity.SysUser;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SysRoleMapper;
import com.hrms.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EmployeeAccountService 单元测试
 * 验证创建账号落库、禁用落库、userId 回填
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeAccountService 员工账号服务")
class EmployeeAccountServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private EmployeeAccountService accountService;

    private Employee employee;
    private SysRole employeeRole;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setName("测试员工");
        employee.setPhone("13800138000");

        employeeRole = new SysRole();
        employeeRole.setId(2L);
        employeeRole.setRoleCode("ROLE_EMPLOYEE");
        employeeRole.setRoleName("普通员工");
    }

    // ═══════════════ 创建账号 ═══════════════

    @Nested
    @DisplayName("创建系统账号")
    class CreateAccount {

        @Test
        @DisplayName("正常创建账号：落库、回填 userId、返回初始密码")
        void shouldCreateAccountAndReturnPassword() {
            when(employeeMapper.selectById(1L)).thenReturn(employee);
            when(sysUserMapper.findByUsername("13800138000")).thenReturn(null);
            when(sysRoleMapper.findByRoleCode("ROLE_EMPLOYEE")).thenReturn(employeeRole);

            ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
            doAnswer(inv -> {
                SysUser u = inv.getArgument(0);
                u.setId(100L); // 模拟自增ID回填
                return 1;
            }).when(sysUserMapper).insert(any(SysUser.class));

            String rawPwd = accountService.createAccount(1L, "13800138000", "ROLE_EMPLOYEE");

            // 验证返回密码
            assertNotNull(rawPwd, "应返回初始密码");
            assertTrue(rawPwd.length() >= 8, "密码应至少8位");

            // 验证 SysUser 落库字段
            verify(sysUserMapper).insert(userCaptor.capture());
            SysUser inserted = userCaptor.getValue();
            assertEquals("13800138000", inserted.getUsername(), "username 应为手机号");
            assertEquals(1, inserted.getStatus(), "status 应为1（正常）");
            assertEquals(1, inserted.getForceChangePwd(), "forceChangePwd 应为1");
            assertEquals(0, inserted.getLoginFailCount(), "loginFailCount 应为0");

            // 验证角色绑定
            verify(sysRoleMapper).insertUserRole(eq(100L), eq(2L));

            // 验证 employee.userId 回填
            verify(employeeMapper).update(argThat(e -> e.getUserId() != null && e.getUserId() == 100L));
        }

        @Test
        @DisplayName("员工不存在应抛异常")
        void shouldThrowWhenEmployeeNotFound() {
            when(employeeMapper.selectById(999L)).thenReturn(null);

            BaseException ex = assertThrows(BaseException.class,
                    () -> accountService.createAccount(999L, "13800138000", "ROLE_EMPLOYEE"));
            assertTrue(ex.getMessage().contains("员工不存在"));
            verify(sysUserMapper, never()).insert(any());
        }

        @Test
        @DisplayName("员工已有账号时跳过创建")
        void shouldSkipWhenAlreadyHasAccount() {
            employee.setUserId(50L);
            when(employeeMapper.selectById(1L)).thenReturn(employee);

            String result = accountService.createAccount(1L, "13800138000", "ROLE_EMPLOYEE");

            assertNull(result, "已有账号应返回null");
            verify(sysUserMapper, never()).insert(any());
        }

        @Test
        @DisplayName("手机号已被注册应抛异常")
        void shouldThrowWhenPhoneExists() {
            when(employeeMapper.selectById(1L)).thenReturn(employee);
            SysUser existing = new SysUser();
            existing.setId(99L);
            existing.setUsername("13800138000");
            when(sysUserMapper.findByUsername("13800138000")).thenReturn(existing);

            BaseException ex = assertThrows(BaseException.class,
                    () -> accountService.createAccount(1L, "13800138000", "ROLE_EMPLOYEE"));
            assertTrue(ex.getMessage().contains("已被注册"));
        }

        @Test
        @DisplayName("角色不存在时仍创建账号但不绑定角色")
        void shouldCreateAccountWithoutRoleWhenRoleNotFound() {
            when(employeeMapper.selectById(1L)).thenReturn(employee);
            when(sysUserMapper.findByUsername("13800138000")).thenReturn(null);
            when(sysRoleMapper.findByRoleCode("ROLE_EMPLOYEE")).thenReturn(null);

            doAnswer(inv -> {
                SysUser u = inv.getArgument(0);
                u.setId(100L);
                return 1;
            }).when(sysUserMapper).insert(any(SysUser.class));

            String rawPwd = accountService.createAccount(1L, "13800138000", "ROLE_EMPLOYEE");

            assertNotNull(rawPwd);
            verify(sysRoleMapper, never()).insertUserRole(anyLong(), anyLong());
            // 仍然回填 userId
            verify(employeeMapper).update(argThat(e -> e.getUserId() == 100L));
        }
    }

    // ═══════════════ 禁用账号 ═══════════════

    @Nested
    @DisplayName("禁用系统账号")
    class DisableAccount {

        @Test
        @DisplayName("正常禁用账号落库")
        void shouldDisableAccount() {
            SysUser user = new SysUser();
            user.setId(100L);
            user.setUsername("testuser");
            user.setStatus(1);
            when(sysUserMapper.findById(100L)).thenReturn(user);

            accountService.disableAccount(100L);

            verify(sysUserMapper).updateStatus(100L, 0);
        }

        @Test
        @DisplayName("userId 为 null 时直接跳过")
        void shouldSkipWhenUserIdNull() {
            accountService.disableAccount(null);
            verify(sysUserMapper, never()).findById(anyLong());
            verify(sysUserMapper, never()).updateStatus(anyLong(), anyInt());
        }

        @Test
        @DisplayName("用户不存在时跳过禁用")
        void shouldSkipWhenUserNotFound() {
            when(sysUserMapper.findById(999L)).thenReturn(null);

            // 不应抛异常
            assertDoesNotThrow(() -> accountService.disableAccount(999L));
            verify(sysUserMapper, never()).updateStatus(anyLong(), anyInt());
        }
    }
}
