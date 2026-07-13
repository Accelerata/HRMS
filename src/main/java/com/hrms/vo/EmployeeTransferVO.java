package com.hrms.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 异动日志VO
 */
@Data
public class EmployeeTransferVO {

    private Long id;
    private Long employeeId;
    private Integer transferType;
    private String transferTypeLabel;
    private Long businessId;
    /** 异动前数据（JSON字符串，前端自行反序列化） */
    private String beforeData;
    /** 异动后数据（JSON字符串） */
    private String afterData;
    private LocalDate effectiveDate;
    private String remark;
    private LocalDateTime createTime;
}
