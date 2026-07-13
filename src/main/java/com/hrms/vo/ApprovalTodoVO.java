package com.hrms.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批待办VO（审批工作台-待办列表）
 */
@Data
public class ApprovalTodoVO {

    /** 审批记录ID */
    private Long approvalId;
    /** 业务类型 */
    private Integer businessType;
    private String businessTypeLabel;
    /** 业务单据ID */
    private Long businessId;
    /** 业务摘要信息 */
    private String summary;
    /** 申请人姓名 */
    private String applicantName;
    /** 申请时间 */
    private LocalDateTime applyTime;
    /** 当前步骤序号 */
    private Integer stepOrder;
    /** 步骤名称 */
    private String stepName;
}
