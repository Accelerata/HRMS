package com.hrms.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 薪资账套模板
 */
@Data
public class SalaryPlan {
    private Long id;
    private String planName;
    private String description;
    /** 状态: 0-停用 1-启用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
