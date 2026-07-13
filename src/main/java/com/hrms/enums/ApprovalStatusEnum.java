package com.hrms.enums;

import lombok.Getter;

/**
 * 审批状态枚举
 * 0-草稿 1-审批中 2-已通过 3-已拒绝 4-待入职 5-已入职
 */
@Getter
public enum ApprovalStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    PENDING_ENTRY(4, "待入职"),
    ONBOARDED(5, "已入职");

    private final int code;
    private final String label;

    ApprovalStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApprovalStatusEnum fromCode(int code) {
        for (ApprovalStatusEnum e : values()) {
            if (e.code == code) return e;
        }
        return DRAFT;
    }
}
