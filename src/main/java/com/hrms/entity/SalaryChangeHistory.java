package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调薪历史
 */
@Data
public class SalaryChangeHistory {
    private Long id;
    private Long employeeId;
    private Long accountId;
    /** CREATE / ADJUST / DEACTIVATE */
    private String changeType;
    private String fieldName;
    /** 变更前值（密文） */
    private String oldValue;
    /** 变更后值（密文） */
    private String newValue;
    private String changeReason;
    /** ONBOARD / ADJUST / MANUAL */
    private String sourceBusiness;
    private Long operatorId;
    private Integer encryptionVersion;
    private LocalDateTime createTime;
}
