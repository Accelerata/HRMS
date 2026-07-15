package com.hrms.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知表
 */
@Data
public class Notification {

    /** 主键 */
    private Long id;

    /** 接收人ID (sys_user.id) */
    private Long recipientId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型: 1-系统通知 2-审批通知 3-提醒 4-警告 */
    private Integer type;

    /** 关联业务类型 */
    private Integer businessType;

    /** 关联业务单据ID */
    private Long businessId;

    /** 是否已读: 0-未读 1-已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;
}
