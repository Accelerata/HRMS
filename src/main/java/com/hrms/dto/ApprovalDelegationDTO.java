package com.hrms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批委托设置 DTO
 */
@Data
public class ApprovalDelegationDTO {

    /** 被委托人ID */
    @NotNull(message = "被委托人不能为空")
    private Long delegateId;

    /** 委托开始时间 */
    @NotNull(message = "委托开始时间不能为空")
    private LocalDateTime startTime;

    /** 委托结束时间 */
    @NotNull(message = "委托结束时间不能为空")
    private LocalDateTime endTime;
}
