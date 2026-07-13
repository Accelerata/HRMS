package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加班记录
 * 用于计算调休余额
 */
@Data
public class OvertimeRecord {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 加班日期 */
    private LocalDate overtimeDate;

    /** 加班开始时间 */
    private LocalDateTime startTime;

    /** 加班结束时间 */
    private LocalDateTime endTime;

    /**
     * 加班时长（小时）
     * 8小时 = 1天调休
     */
    private BigDecimal hours;

    /**
     * 是否已转为调休：
     * 0-未转换 1-已转换
     */
    private Integer convertedToComp;

    /** 创建时间 */
    private LocalDateTime createTime;
}
