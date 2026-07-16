package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志
 */
@Data
public class AuditLog {

    /** 主键ID */
    private Long id;

    /** 操作人ID（sys_user.id） */
    private Long operatorId;

    /** 操作人用户名 */
    private String operatorName;

    /** 操作类型（如 SALARY_VIEW, PAYSLIP_VIEW, EXPORT, CREATE, UPDATE, DELETE） */
    private String operation;

    /** 目标资源类型（如 EMPLOYEE, PAYSLIP, SALARY_BATCH, DEPARTMENT） */
    private String resourceType;

    /** 目标资源ID */
    private String resourceId;

    /** 请求参数摘要（JSON格式，最多500字符） */
    private String requestSummary;

    /** 操作结果：SUCCESS / FAILURE */
    private String result;

    /** 失败时的错误信息 */
    private String errorMessage;

    /** 客户端IP地址 */
    private String clientIp;

    /** 操作时间 */
    private LocalDateTime createTime;
}
