package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资账套
 * 每位员工对应的薪资配置，包含基本工资、社保公积金基数等
 */
@Data
public class SalaryAccount {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 基本工资（元，精确到分） */
    private BigDecimal basicSalary;

    /** 岗位工资（元） */
    private BigDecimal positionSalary;

    /** 绩效工资（元） */
    private BigDecimal performanceSalary;

    /**
     * 试用期薪资比例
     * 例如 0.80 表示试用期发放正式工资的 80%
     */
    private BigDecimal probationRatio;

    /** 社保缴纳基数（元） */
    private BigDecimal socialInsuranceBase;

    /** 公积金缴纳基数（元） */
    private BigDecimal housingFundBase;

    /**
     * 状态：
     * 1-生效中  0-已失效
     */
    private Integer status;

    /** 生效日期 */
    private LocalDateTime effectiveDate;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 加密版本号 */
    private Integer encryptionVersion;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
