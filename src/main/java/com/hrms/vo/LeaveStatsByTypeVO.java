package com.hrms.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类型请假天数统计 VO
 */
@Data
public class LeaveStatsByTypeVO {

    /** 假期类型：1-年假 2-调休 3-事假 4-病假 5-婚假 6-产假 7-丧假 */
    private Integer leaveType;

    /** 类型名称 */
    private String leaveTypeName;

    /** 请假天数合计 */
    private BigDecimal totalDays;

    /** 请假人次 */
    private Integer count;
}
