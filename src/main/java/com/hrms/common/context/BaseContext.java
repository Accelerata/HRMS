package com.hrms.common.context;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 基于 ThreadLocal 的请求上下文
 * 用于在单次请求链路中传递当前登录用户信息
 */
public class BaseContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_EMPLOYEE_ID = new ThreadLocal<>();

    // ── 阶段二新增：角色与数据权限 ──
    private static final ThreadLocal<String> CURRENT_ROLE_CODE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> DATA_SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<Long>> SUB_DEPT_IDS = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> PERMISSIONS = new ThreadLocal<>();

    /** 设置当前用户ID */
    public static void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /** 获取当前用户ID */
    public static Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 设置当前用户名 */
    public static void setCurrentUsername(String username) {
        CURRENT_USERNAME.set(username);
    }

    /** 获取当前用户名 */
    public static String getCurrentUsername() {
        return CURRENT_USERNAME.get();
    }

    /** 设置当前员工ID */
    public static void setCurrentEmployeeId(Long employeeId) {
        CURRENT_EMPLOYEE_ID.set(employeeId);
    }

    /** 获取当前员工ID */
    public static Long getCurrentEmployeeId() {
        return CURRENT_EMPLOYEE_ID.get();
    }

    // ── 阶段二新增：角色与数据权限 ──

    /** 设置当前角色编码 */
    public static void setCurrentRoleCode(String roleCode) {
        CURRENT_ROLE_CODE.set(roleCode);
    }

    /** 获取当前角色编码 */
    public static String getCurrentRoleCode() {
        return CURRENT_ROLE_CODE.get();
    }

    /** 设置数据权限范围 */
    public static void setDataScope(Integer dataScope) {
        DATA_SCOPE.set(dataScope);
    }

    /** 获取数据权限范围 */
    public static Integer getDataScope() {
        return DATA_SCOPE.get();
    }

    /** 设置当前用户所属部门ID */
    public static void setCurrentDeptId(Long deptId) {
        CURRENT_DEPT_ID.set(deptId);
    }

    /** 获取当前用户所属部门ID */
    public static Long getCurrentDeptId() {
        return CURRENT_DEPT_ID.get();
    }

    /** 设置当前用户可见的部门ID列表（含自身和下属） */
    public static void setSubDeptIds(List<Long> deptIds) {
        SUB_DEPT_IDS.set(deptIds);
    }

    /** 获取当前用户可见的部门ID列表 */
    public static List<Long> getSubDeptIds() {
        List<Long> ids = SUB_DEPT_IDS.get();
        return ids != null ? ids : Collections.emptyList();
    }

    /** 设置当前用户权限码集合 */
    public static void setPermissions(Set<String> permissions) {
        PERMISSIONS.set(permissions);
    }

    /** 获取当前用户权限码集合 */
    public static Set<String> getPermissions() {
        Set<String> p = PERMISSIONS.get();
        return p != null ? p : Collections.emptySet();
    }

    /** 请求结束后清理，防止内存泄漏 */
    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_USERNAME.remove();
        CURRENT_EMPLOYEE_ID.remove();
        CURRENT_ROLE_CODE.remove();
        DATA_SCOPE.remove();
        CURRENT_DEPT_ID.remove();
        SUB_DEPT_IDS.remove();
        PERMISSIONS.remove();
    }

}
