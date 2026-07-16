package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工资条查看审计日志
 */
@Data
public class PayslipViewLog {
    private Long id;
    private Long employeeId;
    private Long recordId;
    /** PASSWORD / SMS / NONE */
    private String verifyMethod;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createTime;
}
