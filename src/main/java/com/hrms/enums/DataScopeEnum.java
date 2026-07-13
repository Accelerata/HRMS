package com.hrms.enums;

import lombok.Getter;

/**
 * 数据权限范围枚举
 * 对应 sys_role.data_scope 字段
 */
@Getter
public enum DataScopeEnum {

    /** 全平台数据（系统管理员） */
    ALL_PLATFORM(1, "全平台"),

    /** 全部员工数据（HR 专员） */
    ALL_EMPLOYEE(2, "全部员工"),

    /** 本部门及下属部门（部门主管） */
    DEPT_AND_SUB(3, "本部门及下属"),

    /** 薪资相关（财务专员） */
    SALARY_ONLY(4, "薪资相关"),

    /** 仅本人数据（普通员工） */
    SELF_ONLY(5, "仅本人");

    private final int code;
    private final String description;

    DataScopeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /** 根据 code 获取枚举 */
    public static DataScopeEnum fromCode(int code) {
        for (DataScopeEnum scope : values()) {
            if (scope.code == code) {
                return scope;
            }
        }
        return SELF_ONLY; // 默认最严格
    }
}
