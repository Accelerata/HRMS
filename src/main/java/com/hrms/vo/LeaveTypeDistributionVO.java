package com.hrms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 请假类型分布 VO（用于饼图/环形图）
 */
@Data
public class LeaveTypeDistributionVO {

    /** 假期类型 */
    private Integer leaveType;

    /** 类型名称 */
    private String leaveTypeName;

    /** 请假天数 */
    private BigDecimal totalDays;

    /** 人次 */
    private Integer count;
}
