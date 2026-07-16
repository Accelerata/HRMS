package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤组
 * 定义一套考勤规则（固定班/弹性班、上下班时间、弹性阈值）
 */
@Data
public class AttendanceGroup {

    /** 主键ID */
    private Long id;

    /** 考勤组名称 */
    private String groupName;

    /**
     * 考勤组类型：
     * 1-固定班 (FIXED), 2-弹性班 (FLEXIBLE), 3-排班制 (SHIFT)
     */
    private Integer groupType;

    /** 规定上班时间 (如 09:00) */
    private LocalTime startTime;

    /** 规定下班时间 (如 18:00) */
    private LocalTime endTime;

    /**
     * 弹性阈值（分钟）
     * 固定班：允许迟到宽限分钟数
     * 弹性班：弹性时间窗口
     */
    private Integer flexThreshold;

    /**
     * 半天旷工阈值（分钟）
     * 迟到/早退超过此时间视为旷工半天，默认 120 分钟
     */
    private Integer absentHalfDayThreshold;

    // ── 新增字段（需求6.1.1）──

    /** 适用部门ID（按部门关联） */
    private Long deptId;

    /** 适用职位ID（按职位关联） */
    private Long positionId;

    /** 适用员工ID列表（JSON数组，按个人指定） */
    private String employeeIds;

    /** 午休开始时间（如 12:00） */
    private LocalTime lunchBreakStart;

    /** 午休结束时间（如 13:00） */
    private LocalTime lunchBreakEnd;

    /** 迟到阈值（分钟，默认15） */
    private Integer lateThresholdMinutes;

    /** 早退阈值（分钟，默认15） */
    private Integer earlyThresholdMinutes;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
