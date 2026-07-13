package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 假期余额
 * 记录每种假期类型的剩余额度
 */
@Data
public class LeaveBalance {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /**
     * 假期类型：
     * 1-年假 2-调休 3-事假 4-病假 5-婚假 6-产假 7-丧假
     */
    private Integer leaveType;

    /** 总余额（天，支持0.5天） */
    private BigDecimal totalDays;

    /** 已使用天数 */
    private BigDecimal usedDays;

    /** 剩余天数 */
    private BigDecimal remainingDays;

    /** 年份（年假按年计算） */
    private Integer year;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
