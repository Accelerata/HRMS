package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 薪资账套
 */
@Data
public class SalaryAccount {

    private Long id;
    private Long employeeId;
    /** 关联薪资账套模板ID */
    private Long planId;
    /** 基本工资 */
    private BigDecimal basicSalary;
    /** 岗位工资 */
    private BigDecimal positionSalary;
    /** 绩效工资 */
    private BigDecimal performanceSalary;
    /** 试用期薪资比例 */
    private BigDecimal probationRatio;
    /** 社保缴纳基数 */
    private BigDecimal socialInsuranceBase;
    /** 公积金缴纳基数 */
    private BigDecimal housingFundBase;
    /** 状态: 1-生效中 0-已失效 */
    private Integer status;
    /** 生效日期（原字段） */
    private LocalDateTime effectiveDate;
    /** 档案生效开始日期 */
    private LocalDate effectiveStartDate;
    /** 档案生效结束日期（NULL=至今有效） */
    private LocalDate effectiveEndDate;
    /** 调薪原因 */
    private String changeReason;
    /** 操作人ID */
    private Long operatorId;
    /** 加密版本号 */
    private Integer encryptionVersion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
