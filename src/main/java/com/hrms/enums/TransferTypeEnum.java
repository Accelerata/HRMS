package com.hrms.enums;

import lombok.Getter;

/**
 * 异动类型枚举
 */
@Getter
public enum TransferTypeEnum {

    ONBOARDING(1, "入职"),
    REGULARIZATION(2, "转正"),
    TRANSFER(3, "调岗"),
    RESIGNATION(4, "离职");

    private final int code;
    private final String label;

    TransferTypeEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TransferTypeEnum fromCode(int code) {
        for (TransferTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        throw new IllegalArgumentException("未知异动类型: " + code);
    }
}
