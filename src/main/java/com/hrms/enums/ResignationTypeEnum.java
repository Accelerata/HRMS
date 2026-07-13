package com.hrms.enums;

import lombok.Getter;

/**
 * 离职类型枚举
 */
@Getter
public enum ResignationTypeEnum {

    VOLUNTARY(1, "主动离职"),
    NEGOTIATED(2, "协商离职"),
    CONTRACT_EXPIRY(3, "合同到期"),
    LAYOFF(4, "裁员");

    private final int code;
    private final String label;

    ResignationTypeEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ResignationTypeEnum fromCode(int code) {
        for (ResignationTypeEnum e : values()) {
            if (e.code == code) return e;
        }
        return VOLUNTARY;
    }
}
