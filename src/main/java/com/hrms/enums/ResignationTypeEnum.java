package com.hrms.enums;

import lombok.Getter;

/**
 * 离职类型枚举
 * 辞职/辞退/合同到期不续签/其他
 */
@Getter
public enum ResignationTypeEnum {

    RESIGNATION(1, "辞职"),
    DISMISSAL(2, "辞退"),
    CONTRACT_EXPIRY(3, "合同到期不续签"),
    OTHER(4, "其他");

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
        return OTHER;
    }
}
