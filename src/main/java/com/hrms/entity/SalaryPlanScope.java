package com.hrms.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 薪资账套适用范围
 */
@Data
public class SalaryPlanScope {
    private Long id;
    private Long planId;
    /** DEPT / POSITION / GRADE / DEFAULT */
    private String scopeType;
    private Long targetId;
    /** 职级值，如 P6/T7 */
    private String targetValue;
    /** 优先级（越高越优先） */
    private Integer priority;
    private LocalDate effectiveDate;
    private LocalDateTime createTime;
}
