package com.hrms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批任务转交 DTO
 */
@Data
public class ApprovalTransferDTO {

    /** 目标审批人ID（转交给谁） */
    @NotNull(message = "转交目标人不能为空")
    private Long targetUserId;

    /** 转交说明（可选） */
    private String comment;
}
