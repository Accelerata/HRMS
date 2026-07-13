package com.hrms.dto;

import lombok.Data;

/**
 * 批量薪资核算请求
 */
@Data
public class SalaryBatchCalcDTO {

    /** 核算年份 */
    private Integer year;

    /** 核算月份 (1-12) */
    private Integer month;

    /** 可选：指定部门ID（为null则全公司核算） */
    private Long deptId;
}
