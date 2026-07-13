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

    /** 调岗原因 */
    private String transferReason;

    @NotBlank(message = "生效日期不能为空")
    private String effectiveDate;
}
