package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资批次
 * HR 批量核算生成，提交后经财务专员审批，通过则批次内记录批量确认
 */
@Data
public class SalaryBatch {

    private Long id;

    /** 核算年份 */
    private Integer year;

    /** 核算月份 (1-12) */
    private Integer month;

    /** 状态: DRAFT/CALCULATING/PENDING_CONFIRM/PENDING_APPROVAL/APPROVED/PAID/ARCHIVED/REJECTED */
    private String status;

    /** 核算人数 */
    private Integer employeeCount;

    /** 实发合计 */
    private BigDecimal totalNetPay;

    /** 提交人ID */
    private Long submitterId;

    /** 考勤是否已锁定: 0-未锁 1-已锁定 */
    private Integer attendanceLocked;

    /** 考勤锁定时间 */
    private LocalDateTime lockTime;

    /** 阻断原因 */
    private String blockingReason;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
