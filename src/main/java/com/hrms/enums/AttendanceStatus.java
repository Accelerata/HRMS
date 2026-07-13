package com.hrms.enums;

import lombok.Getter;

/**
 * 考勤打卡状态枚举
 * NORMAL - 正常
 * LATE - 迟到
 * EARLY - 早退
 * ABSENT_HALF_DAY - 旷工半天
 * MISSING_PUNCH - 缺卡
 */
@Getter
public enum AttendanceStatus {

    NORMAL("正常", "#52c41a"),
    LATE("迟到", "#faad14"),
    EARLY("早退", "#faad14"),
    ABSENT_HALF_DAY("旷工半天", "#ff4d4f"),
    MISSING_PUNCH("缺卡", "#ff4d4f");

    private final String label;
    private final String color;

    AttendanceStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }
}
