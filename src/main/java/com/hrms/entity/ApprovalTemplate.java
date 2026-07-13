package com.hrms.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批模板表
 */
@Data
public class ApprovalTemplate {

    private Long id;
    /** 业务类型: 1-入职 2-转正 3-调岗 4-离职 5-薪资 */
    private Integer businessType;
    /** 审批步骤序号（同序号=并行审批） */
    private Integer stepOrder;
    /** 审批人指向 */
    private String approverTarget;
    /** 步骤名称 */
    private String stepName;
    /** 创建时间 */
    private LocalDateTime createTime;
}
