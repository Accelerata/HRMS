package com.hrms.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批已办VO（审批工作台-已办列表）
 */
@Data
public class ApprovalDoneVO {

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
    /** 我的审批动作 */
    private Integer action;
    private String actionLabel;
    /** 我的审批意见 */
    private String comment;
    /** 审批时间 */
    private LocalDateTime operateTime;
}
