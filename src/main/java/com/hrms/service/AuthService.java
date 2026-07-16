package com.hrms.service;

import com.hrms.common.constant.JwtClaimsConstant;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.LoginDTO;
import com.hrms.entity.Employee;
import com.hrms.entity.SysRole;
import com.hrms.entity.SysUser;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SysPermissionMapper;
import com.hrms.mapper.SysRoleMapper;
import com.hrms.mapper.SysUserMapper;
import com.hrms.utils.JwtUtil;
import com.hrms.utils.PasswordUtil;
import com.hrms.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final EmployeeMapper employeeMapper;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.admin-secret-key}")
    private String secretKey;

    @Value("${jwt.admin-ttl}")
    private long ttlMillis;

    /**
     * 用户登录
     * @param dto 登录请求
     * @return 登录结果（含 JWT Token）
     */
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        SysUser user = sysUserMapper.findByUsername(dto.getUsername());
        if (user == null) {
            log.warn("登录失败: 用户名不存在 - {}", dto.getUsername());
            throw new BaseException(401, "用户名或密码错误");
        }

        // 2. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("登录失败: 账号已禁用 - {}", dto.getUsername());
            throw new BaseException(401, "账号已被禁用，请联系管理员");
        }

        // 3. 校验密码
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            log.warn("登录失败: 密码错误 - {}", dto.getUsername());
            throw new BaseException(401, "用户名或密码错误");
        }

        // 3.5 校验密码是否过期（超过90天未更换）
        if (user.getPwdUpdateTime() != null
                && user.getPwdUpdateTime().isBefore(java.time.LocalDateTime.now().minusDays(90))) {
            log.warn("登录失败: 密码已过期 - {}", dto.getUsername());
            throw new BaseException(426, "密码已过期，请修改密码后重新登录");
        }

        // 4. 查询角色
        SysRole role = sysRoleMapper.findByUserId(user.getId());
        String roleCode = role != null ? role.getRoleCode() : "ROLE_EMPLOYEE";
        String roleName = role != null ? role.getRoleName() : "普通员工";
        Integer dataScope = role != null ? role.getDataScope() : 5;

        // 4.5 校验是否需要强制改密
        boolean needChangePwd = user.getForceChangePwd() != null && user.getForceChangePwd() == 1;

        // 5. 查询权限码
        List<String> permissions = sysPermissionMapper.findPermissionCodesByUserId(user.getId());
        // 系统管理员默认拥有所有权限
        if (dataScope == 1 && permissions.isEmpty()) {
            permissions = List.of("*"); // 通配符表示所有权限
        }

        // 6. 关联员工档案
        Employee employee = employeeMapper.selectByUserId(user.getId());
        Long employeeId = employee != null ? employee.getId() : null;
        Long deptId = employee != null ? employee.getDeptId() : null;

        // 7. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.EMPLOYEE_ID, employeeId);
        claims.put(JwtClaimsConstant.ROLE_CODE, roleCode);
        claims.put(JwtClaimsConstant.DATA_SCOPE, dataScope);
        claims.put(JwtClaimsConstant.DEPT_ID, deptId);
        claims.put(JwtClaimsConstant.PERMISSIONS, String.join(",", permissions));

        String token = JwtUtil.createJWT(secretKey, ttlMillis, claims);

        log.info("登录成功: username={}, role={}, employeeId={}, permissions={}",
                user.getUsername(), roleCode, employeeId, permissions);

        // 7.5 创建会话（Redis记录活跃时间）
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set("session:active:" + user.getId(),
                        System.currentTimeMillis(), 30, java.util.concurrent.TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.warn("创建会话失败(Redis不可用): userId={}", user.getId());
        }

        // 7. 返回结果
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .employeeId(employeeId)
                .roleCode(roleCode)
                .roleName(roleName)
                .dataScope(dataScope)
                .permissions(permissions)
                .needChangePwd(needChangePwd)
                .build();
    }

    /**
     * 修改密码
     *
     * @param userId      当前用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw new BaseException(401, "未登录");
        }
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw BaseException.notFound("用户不存在");
        }
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            throw BaseException.badRequest("旧密码错误");
        }
        if (!PasswordUtil.isStrong(newPassword)) {
            throw BaseException.badRequest("新密码不符合要求：至少8位，包含大小写字母和数字");
        }
        sysUserMapper.updatePassword(userId, PasswordUtil.encode(newPassword));
        log.info("密码修改成功: userId={}", userId);
    }
}
