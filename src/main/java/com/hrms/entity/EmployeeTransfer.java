package com.hrms.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工异动日志表
 */
@Data
public class EmployeeTransfer {

    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 异动类型: 1-入职 2-转正 3-调岗 4-离职 */
    private Integer transferType;
    /** 关联业务单据ID */
    private Long businessId;
    /** 异动前数据(JSON) */
    private String beforeData;
    /** 异动后数据(JSON) */
    private String afterData;
    /** 生效日期 */
    private LocalDate effectiveDate;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
}
