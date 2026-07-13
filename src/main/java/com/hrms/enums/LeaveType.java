package com.hrms.enums;

import lombok.Getter;

/**
 * 假期类型枚举
 */
@Getter
public enum LeaveType {

    ANNUAL(1, "年假"),
    COMPENSATORY(2, "调休"),
    PERSONAL(3, "事假"),
    SICK(4, "病假"),
    MARRIAGE(5, "婚假"),
    MATERNITY(6, "产假"),
    BEREAVEMENT(7, "丧假");

    private final int code;
    private final String label;

    LeaveType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static LeaveType fromCode(int code) {
        for (LeaveType e : values()) {
            if (e.code == code) return e;
        }
        return PERSONAL;
    }
}
