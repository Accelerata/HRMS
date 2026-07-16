package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 薪资调整项
 */
@Data
public class SalaryAdjustment {
    private Long id;
    private Long batchId;
    private Long employeeId;
    private Long recordId;
    /** INCOME / DEDUCTION */
    private String adjustType;
    /** 金额（加密存储） */
    private String amount;
    private String reason;
    private Long operatorId;
    private LocalDateTime createTime;
}
