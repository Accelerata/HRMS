package com.hrms.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工合同表
 * 支持合同续签历史记录
 */
@Data
public class EmployeeContract {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 合同类型: 1-固定期限 2-无固定期限 3-劳务合同 */
    private Integer contractType;

    /** 合同开始日期 */
    private LocalDate contractStartDate;

    /** 合同到期日（无固定期限为空） */
    private LocalDate contractEndDate;

    /** 试用期待遇比例（80-100） */
    private Integer probationSalaryRatio;

    /** 状态: 0-已终止 1-生效中 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
