package com.hrms.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批记录表
 */
@Data
public class ApprovalRecord {

    private Long id;
    /** 业务类型 */
    private Integer businessType;
    /** 业务单据ID */
    private Long businessId;
    /** 审批步骤序号 */
    private Integer stepOrder;
    /** 审批人ID */
    private Long approverId;
    /** 审批人姓名 */
    private String approverName;
    /** 审批动作: 1-通过 2-拒绝 3-退回 */
    private Integer action;
    /** 审批意见 */
    private String comment;
    /** 是否待审批: 1-待审 0-已处理 */
    private Integer isPending;
    /** 审批操作时间 */
    private LocalDateTime operateTime;
    /** 审批截止时间（每级48h，超时催办） */
    private LocalDateTime dueTime;
    /** 创建时间 */
    private LocalDateTime createTime;
}
