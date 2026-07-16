package com.hrms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 部门请假率 VO
 */
@Data
public class DeptLeaveRateVO {

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 统计月份 yyyy-MM */
    private String month;

    /** 部门在职人数 */
    private Integer activeEmployeeCount;

    /** 当月工作日数 */
    private Integer workdayCount;

    /** 部门当月已通过请假天数合计 */
    private BigDecimal totalLeaveDays;

    /** 请假率 = totalLeaveDays / (activeEmployeeCount * workdayCount) */
    private BigDecimal leaveRate;

    /** 分类型请假天数 */
    private List<LeaveStatsByTypeVO> byType;
}
