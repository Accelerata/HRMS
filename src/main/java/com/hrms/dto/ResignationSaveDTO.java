package com.hrms.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 离职申请 - 保存DTO
 */
@Data
public class ResignationSaveDTO {

    private Long id;

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @NotNull(message = "离职类型不能为空")
    private Integer resignationType;

    /** 离职原因 */
    private String resignationReason;

    @NotBlank(message = "最后工作日不能为空")
    private String resignationDate;

    /** 交接事项 */
    private String handoverInfo;

    /** 接手人ID */
    private Long handoverTo;
}
