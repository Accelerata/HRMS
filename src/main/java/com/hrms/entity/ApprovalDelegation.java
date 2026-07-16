package com.hrms.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批委托表
 * 审批人设置委托后，委托期间新产生的审批任务自动改派被委托人
 */
@Data
public class ApprovalDelegation {

    private Long id;
    /** 委托人ID（原审批人） */
    private Long delegatorId;
    /** 委托人姓名 */
    private String delegatorName;
    /** 被委托人ID */
    private Long delegateId;
    /** 被委托人姓名 */
    private String delegateName;
    /** 委托开始时间 */
    private LocalDateTime startTime;
    /** 委托结束时间 */
    private LocalDateTime endTime;
    /** 状态: 0-已取消 1-生效中 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
