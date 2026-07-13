package com.hrms.enums;

import lombok.Getter;

/**
 * 审批业务类型枚举
 */
@Getter
public enum BusinessTypeEnum {

    ONBOARDING(1, "入职"),
    REGULARIZATION(2, "转正"),
    TRANSFER(3, "调岗"),
    RESIGNATION(4, "离职"),
    SALARY(5, "薪资");

    private final int code;
    private final String label;

    BusinessTypeEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static BusinessTypeEnum fromCode(int code) {
        for (BusinessTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("未知业务类型: " + code);
    }
}
