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

    /** 状态: DRAFT-草稿 PENDING-审批中 APPROVED-已批准 REJECTED-已拒绝 */
    private String status;

    /** 核算人数 */
    private Integer employeeCount;

    /** 实发合计（批次内记录实发工资汇总） */
    private BigDecimal totalNetPay;

    /** 提交人ID（HR，sys_user.id） */
    private Long submitterId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
