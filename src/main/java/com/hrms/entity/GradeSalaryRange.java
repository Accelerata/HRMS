package com.hrms.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 职级薪资范围表
 * 用于入职二审条件判断：薪资是否在对应职级的合理范围内
 */
@Data
public class GradeSalaryRange {

    /** 主键ID */
    private Long id;

    /** 职级编码（如 P1、P3、M5） */
    private String gradeCode;

    /** 薪资下限（含） */
    private BigDecimal minSalary;

    /** 薪资上限（含） */
    private BigDecimal maxSalary;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
