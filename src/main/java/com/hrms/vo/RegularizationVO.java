package com.hrms.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 转正申请 - 详情VO
 */
@Data
public class RegularizationVO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long deptId;
    private String deptName;
    private String positionName;
    private LocalDate hireDate;
    private LocalDate probationEndDate;
    private BigDecimal formalSalary;
    private String probationSummary;
    private String supervisorComment;
    private Integer status;
    private String statusLabel;
    private Long submitterId;
    private String submitterName;
    private List<ApprovalRecordVO> approvalRecords;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
