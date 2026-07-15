package com.hrms.enums;

import lombok.Getter;

/**
 * 员工状态枚举
 * 0-待入职 1-试用期 2-正式 3-待离职 4-已离职
 */
@Getter
public enum EmployeeStatusEnum {

    PENDING_ENTRY(0, "待入职"),
    PROBATION(1, "试用期"),
    REGULAR(2, "正式"),
    PENDING_RESIGN(3, "待离职"),
    RESIGNED(4, "已离职");

    private final int code;
    private final String label;

    EmployeeStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static EmployeeStatusEnum fromCode(int code) {
        for (EmployeeStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("未知员工状态: " + code);
    }

    /**
     * 校验状态流转是否合法
     *
     * @param current 当前状态
     * @param target  目标状态
     * @return true 表示允许流转
     */
    public static boolean canTransition(int current, int target) {
        EmployeeStatusEnum cur = fromCode(current);
        EmployeeStatusEnum tgt = fromCode(target);
        return switch (cur) {
            case PENDING_ENTRY -> tgt == PROBATION || tgt == RESIGNED; // 待入职→试用期 或 放弃入职
            case PROBATION   -> tgt == REGULAR || tgt == PENDING_RESIGN; // 试用期→正式 或 待离职
            case REGULAR     -> tgt == PENDING_RESIGN; // 正式→待离职
            case PENDING_RESIGN -> tgt == RESIGNED; // 待离职→已离职（定时任务处理）
            case RESIGNED    -> false; // 已离职不可逆
        };
    }
}
