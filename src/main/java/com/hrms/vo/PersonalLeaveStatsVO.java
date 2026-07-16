package com.hrms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 个人请假统计 VO
 */
@Data
public class PersonalLeaveStatsVO {

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 统计月份 yyyy-MM */
    private String month;

    /** 分类型已通过请假天数 */
    private List<LeaveStatsByTypeVO> byType;

    /** 当年年假剩余天数 */
    private BigDecimal annualRemainingDays;
}
