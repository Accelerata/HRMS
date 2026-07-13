package com.hrms.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 转正申请 - 保存DTO
 */
@Data
public class RegularizationSaveDTO {

    private Long id;

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    /** 转正后薪资 */
    private BigDecimal formalSalary;

    /** 试用期工作小结 */
    private String probationSummary;

    /** 直属上级评语 */
    private String supervisorComment;
}
