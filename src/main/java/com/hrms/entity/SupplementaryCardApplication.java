package com.hrms.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 补卡申请
 * 员工针对缺卡日期发起，直接上级单级审批，通过后回写考勤记录
 */
@Data
public class SupplementaryCardApplication {

    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 补卡日期 */
    private LocalDate attendanceDate;

    /** 卡型: 1-上班卡 2-下班卡 */
    private Integer cardType;

    /** 补卡时间 */
    private LocalTime supplementTime;

    /** 补卡事由 */
    private String reason;

    /** 审批状态: 0-草稿 1-审批中 2-已通过 3-已拒绝 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
