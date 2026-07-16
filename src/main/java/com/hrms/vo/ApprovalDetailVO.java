package com.hrms.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批详情VO（审批工作台-统一详情页，需求 8.2.2）
 */
@Data
public class ApprovalDetailVO {

    /** 业务类型 */
    private Integer businessType;
    private String businessTypeLabel;
    /** 业务单据ID */
    private Long businessId;
    /** 业务摘要 */
    private String summary;
    /** 申请人姓名 */
    private String applicantName;
    /** 业务单据完整数据（按类型返回对应实体） */
    private Object businessData;
    /** 审批历史（各节点审批人/动作/意见/时间） */
    private List<HistoryNode> history;
    /** 当前用户可处理的审批记录ID（无则为空） */
    private Long myRecordId;
    /** 当前用户是否可执行审批操作（通过/拒绝/转交） */
    private boolean canOperate;

    /**
     * 审批历史节点
     */
    @Data
    public static class HistoryNode {
        /** 审批记录ID */
        private Long recordId;
        /** 步骤序号 */
        private Integer stepOrder;
        /** 步骤名称 */
        private String stepName;
        /** 审批人姓名（实际处理人） */
        private String approverName;
        /** 原审批人姓名（转交/委托改派前） */
        private String originalApproverName;
        /** 分配方式: 0-正常 1-转交 2-委托 */
        private Integer assignType;
        /** 审批动作 */
        private Integer action;
        private String actionLabel;
        /** 审批意见（含「XXX 代 YYY 审批」留痕） */
        private String comment;
        /** 是否待审批 */
        private Integer isPending;
        /** 审批操作时间 */
        private LocalDateTime operateTime;
        /** 审批截止时间 */
        private LocalDateTime dueTime;
    }
}
