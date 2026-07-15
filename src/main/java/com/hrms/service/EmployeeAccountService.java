package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.Employee;
import com.hrms.entity.SysRole;
import com.hrms.entity.SysUser;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SysRoleMapper;
import com.hrms.mapper.SysUserMapper;
import com.hrms.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工系统账号 Service
 *
 * 职责：
 * 1. 入职审批通过后创建系统账号（username=手机号、随机初始密码、forceChangePwd=1、绑定角色）
 * 2. 离职/调岗时禁用账号
 * 3. 回填 employee.userId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeAccountService {

    private final SysUserMapper sysUserMapper;
    private final EmployeeMapper employeeMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * 创建系统账号（入职审批通过后调用）
     *
     * @param employeeId 员工ID
     * @param phone      手机号（作为用户名）
     * @param roleCode   初始角色编码（如 ROLE_EMPLOYEE）
     * @return 初始密码（明文，用于展示给 HR/候选人）
     */
    @Transactional
    public String createAccount(Long employeeId, String phone, String roleCode) {
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }
        if (emp.getUserId() != null) {
            log.warn("员工已有关联账号: employeeId={}, userId={}", employeeId, emp.getUserId());
            return null;
        }

        SysUser existingUser = sysUserMapper.findByUsername(phone);
        if (existingUser != null) {
            throw BaseException.badRequest("手机号 " + phone + " 已被注册");
        }

        String rawPwd = PasswordUtil.generateRandomPassword();

        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPassword(PasswordUtil.encode(rawPwd));
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setForceChangePwd(1);
        sysUserMapper.insert(user);

        log.info("系统账号已创建: userId={}, username={}", user.getId(), phone);

        SysRole role = sysRoleMapper.findByRoleCode(roleCode);
        if (role != null) {
            sysRoleMapper.insertUserRole(user.getId(), role.getId());
            log.info("角色绑定完成: userId={}, roleCode={}", user.getId(), roleCode);
        } else {
            log.warn("角色不存在: roleCode={}, 跳过角色绑定", roleCode);
        }

        emp.setUserId(user.getId());
        employeeMapper.update(emp);

        log.info("员工关联账号已完成: employeeId={}, userId={}", employeeId, user.getId());
        return rawPwd;
    }

    /**
     * 禁用系统账号（离职生效时调用）
     *
     * @param userId 系统用户ID
     */
    public void disableAccount(Long userId) {
        if (userId == null) return;
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            log.warn("系统用户不存在: userId={}", userId);
            return;
        }
        sysUserMapper.updateStatus(userId, 0);
        log.info("系统账号已禁用: userId={}, username={}", userId, user.getUsername());
    }
}
