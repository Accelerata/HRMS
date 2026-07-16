package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调休入账明细
 *
 * 记录每次加班折算入账的调休天数与过期日，是有效期判定的权威来源。
 */
@Data
public class CompLeaveGrant {

    /** 主键ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 加班所属月 yyyy-MM */
    private String overtimeMonth;

    /** 本次折算入账天数 */
    private BigDecimal days;

    /** 已使用天数 */
    private BigDecimal usedDays;

    /** 过期日（加班次月月末） */
    private LocalDate expireDate;

    /** 状态：1-有效 0-已过期清零 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
