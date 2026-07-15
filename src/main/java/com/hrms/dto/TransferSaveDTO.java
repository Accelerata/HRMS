package com.hrms.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 调岗申请 - 保存DTO
 */
@Data
public class TransferSaveDTO {

    private Long id;

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @NotNull(message = "新部门不能为空")
    private Long toDeptId;

    /** 新职位ID */
    private Long toPositionId;

    /** 目标职级 */
    private String toGrade;

    /** 新汇报人ID */
    private Long toReportTo;

    /** 调岗薪资调整金额 */
    private java.math.BigDecimal salaryAdjust;

    /** 调岗原因 */
    private String transferReason;

    @NotBlank(message = "生效日期不能为空")
    private String effectiveDate;
}
