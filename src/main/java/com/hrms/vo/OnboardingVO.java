package com.hrms.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入职申请 - 详情VO
 */
@Data
public class OnboardingVO {

    private Long id;
    private Long employeeId;
    private String realName;
    private String phone;
    private String email;
    private String idCard;
    private Long targetDeptId;
    private String targetDeptName;
    private Long targetPositionId;
    private String targetPositionName;
    private BigDecimal offerSalary;
    private Integer probationMonths;
    private LocalDate entryDate;
    private Integer status;
    private String statusLabel;
    private Long submitterId;
    private String submitterName;
    private List<ApprovalRecordVO> approvalRecords;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
