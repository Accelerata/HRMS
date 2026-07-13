package com.hrms.enums;

import lombok.Getter;

/**
 * 考勤组类型枚举
 * FIXED - 固定班（固定上下班时间）
 * FLEXIBLE - 弹性班（弹性上班时间，有弹性阈值）
 */
@Getter
public enum AttendanceGroupType {

    FIXED(1, "固定班"),
    FLEXIBLE(2, "弹性班");

    private final int code;
    private final String label;

    AttendanceGroupType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AttendanceGroupType fromCode(int code) {
        for (AttendanceGroupType e : values()) {
            if (e.code == code) return e;
        }
        return FIXED;
    }
}
