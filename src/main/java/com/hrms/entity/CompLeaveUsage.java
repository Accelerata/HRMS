package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调休占用明细
 *
 * 记录每笔请假申请消耗了哪些调休入账（保证拒绝/取消时按原路径精确回补）
 */
@Data
public class CompLeaveUsage {

    /** 主键ID */
    private Long id;

    /** 关联 leave_application.id */
    private Long applicationId;

    /** 关联 comp_leave_grant.id */
    private Long grantId;

    /** 从该 grant 扣减的天数 */
    private BigDecimal days;

    /** 创建时间 */
    private LocalDateTime createTime;
}
