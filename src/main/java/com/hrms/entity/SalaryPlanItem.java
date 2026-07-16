package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 薪资账套工资项目
 */
@Data
public class SalaryPlanItem {
    private Long id;
    private Long planId;
    private String itemName;
    private String itemCode;
    /** INCOME / DEDUCTION */
    private String itemType;
    /** FIXED / VARIABLE / ATTENDANCE / SOCIAL / FUND / TAX */
    private String category;
    private String calcRule;
    private Integer sortOrder;
    private Integer isEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
