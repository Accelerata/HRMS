package com.hrms.enums;

import lombok.Getter;

/**
 * 审批动作枚举
 */
@Getter
public enum ApprovalActionEnum {

    APPROVE(1, "通过"),
    REJECT(2, "拒绝"),
    RETURN(3, "退回");

    private final int code;
    private final String label;

    ApprovalActionEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApprovalActionEnum fromCode(int code) {
        for (ApprovalActionEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("未知审批动作: " + code);
    }
}
