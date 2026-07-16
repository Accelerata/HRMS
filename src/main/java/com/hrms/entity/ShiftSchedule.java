package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排班制班次配置
 * 定义考勤组下每周各天的上下班时间
 */
@Data
public class ShiftSchedule {

    /** 主键ID */
    private Long id;

    /** 考勤组ID */
    private Long groupId;

    /** 星期几：1=周一 … 7=周日 */
    private Integer dayOfWeek;

    /** 上班时间 */
    private LocalTime startTime;

    /** 下班时间 */
    private LocalTime endTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
