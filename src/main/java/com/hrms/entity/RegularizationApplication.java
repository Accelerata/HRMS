package com.hrms.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转正申请表
 */
@Data
public class RegularizationApplication {

    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 转正后薪资 */
    private BigDecimal formalSalary;
    /** 试用期工作小结 */
    private String probationSummary;
    /** 直属上级评语 */
    private String supervisorComment;
    /** 状态: 0-草稿 1-审批中 2-已通过 3-已拒绝 */
    private Integer status;
    /** 提交人ID */
    private Long submitterId;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
