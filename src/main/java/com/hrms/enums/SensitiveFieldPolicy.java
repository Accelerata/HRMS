package com.hrms.enums;

import lombok.Getter;

/**
 * 敏感字段过滤策略
 *
 * 根据当前用户的数据权限范围，决定 EmployeeVO 中哪些敏感字段可见。
 * 白名单模式：默认所有敏感字段不可见，仅当策略匹配时显式填充。
 */
@Getter
public enum SensitiveFieldPolicy {

    /** 显示全部字段（系统管理员、HR专员） */
    SHOW_ALL,

    /**
     * 隐藏薪资和银行信息（部门主管）
     * 可见：姓名、工号、手机号、部门、职位、职级等基本信息
     * 不可见：身份证号、薪资信息、银行卡号、紧急联系人
     */
    HIDE_SALARY_BANK,

    /**
     * 仅显示本人敏感字段（普通员工）
     * 当 targetEmployeeId == currentEmployeeId 时等同 SHOW_ALL
     * 当 targetEmployeeId != currentEmployeeId 时仅显示非敏感基本信息
     */
    SELF_ONLY,

    /**
     * 薪资相关（财务专员）
     * 可见：薪资信息汇总
     * 不可见：身份证号、银行卡号、手机号等个人隐私
     */
    SALARY_ONLY;

    /**
     * 根据 DataScopeEnum 映射到过滤策略
     */
    public static SensitiveFieldPolicy fromDataScope(Integer dataScope) {
        if (dataScope == null) return SELF_ONLY; // 默认最严格
        DataScopeEnum scope = DataScopeEnum.fromCode(dataScope);
        return switch (scope) {
            case ALL_PLATFORM, ALL_EMPLOYEE -> SHOW_ALL;
            case DEPT_AND_SUB -> HIDE_SALARY_BANK;
            case SALARY_ONLY -> SALARY_ONLY;
            case SELF_ONLY -> SELF_ONLY;
        };
    }

    /**
     * 根据角色编码映射到过滤策略
     */
    public static SensitiveFieldPolicy fromRoleCode(String roleCode) {
        if (roleCode == null) return SELF_ONLY;
        return switch (roleCode) {
            case "ROLE_ADMIN", "ROLE_HR" -> SHOW_ALL;
            case "ROLE_MANAGER" -> HIDE_SALARY_BANK;
            case "ROLE_FINANCE" -> SALARY_ONLY;
            case "ROLE_EMPLOYEE" -> SELF_ONLY;
            default -> SELF_ONLY;
        };
    }
}
