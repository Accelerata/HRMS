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
     * 1-固定班 (FIXED), 2-弹性班 (FLEXIBLE)
     */
    private Integer groupType;

    /** 规定上班时间 (如 09:00) */
    private LocalTime startTime;

    /** 规定下班时间 (如 18:00) */
    private LocalTime endTime;

    /**
     * 弹性阈值（分钟）
     * 固定班：允许迟到宽限分钟数
     * 弹性班：弹性时间窗口，如 60 表示最晚 10:00 打卡不算迟到
     */
    private Integer flexThreshold;

    /**
     * 半天旷工阈值（分钟）
     * 迟到/早退超过此时间视为旷工半天，默认 120 分钟
     */
    private Integer absentHalfDayThreshold;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
