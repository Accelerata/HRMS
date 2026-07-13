package com.hrms.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批记录VO
 */
@Data
public class ApprovalRecordVO {

    private Long id;
    private Integer businessType;
    private Long businessId;
    private Integer stepOrder;
    private Long approverId;
    private String approverName;
    private Integer action;
    private String actionLabel;
    private String comment;
    private LocalDateTime operateTime;
    private LocalDateTime createTime;
}
