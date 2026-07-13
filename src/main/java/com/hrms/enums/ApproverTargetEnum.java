package com.hrms.enums;

import lombok.Getter;

/**
 * 审批人指向枚举
 */
@Getter
public enum ApproverTargetEnum {

    DEPT_MANAGER("dept_manager", "部门主管"),
    HR_SPECIALIST("hr_specialist", "HR专员"),
    OLD_DEPT_MANAGER("old_dept_manager", "原部门主管"),
    NEW_DEPT_MANAGER("new_dept_manager", "新部门主管");

    private final String code;
    private final String label;

    ApproverTargetEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
