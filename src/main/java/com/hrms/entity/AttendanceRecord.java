package com.hrms.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 打卡记录
 * 记录员工每日的打卡详情
 */
@Data
public class AttendanceRecord {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 考勤组ID */
    private Long groupId;

    /** 打卡日期 */
    private LocalDate attendanceDate;

    /** 上班打卡时间 */
    private LocalTime punchInTime;

    /** 下班打卡时间 */
    private LocalTime punchOutTime;

    /**
     * 上班打卡状态
     * NORMAL / LATE / MISSING_PUNCH / ABSENT_HALF_DAY
     */
    private String punchInStatus;

    /**
     * 下班打卡状态
     * NORMAL / EARLY / MISSING_PUNCH / ABSENT_HALF_DAY
     */
    private String punchOutStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
