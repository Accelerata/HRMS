package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请记录
 */
@Data
public class LeaveApplication {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /**
     * 假期类型：
     * 1-年假 2-调休 3-事假 4-病假 5-婚假 6-产假 7-丧假
     */
    private Integer leaveType;

    /** 请假开始日期 */
    private LocalDate startDate;

    /** 请假结束日期 */
    private LocalDate endDate;

    /**
     * 请假天数（支持0.5天）
     * 上午半天=0.5, 下午半天=0.5, 全天=1.0
     */
    private BigDecimal days;

    /** 请假原因 */
    private String reason;

    /**
     * 审批状态：
     * 0-草稿 1-审批中 2-已通过 3-已拒绝
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
