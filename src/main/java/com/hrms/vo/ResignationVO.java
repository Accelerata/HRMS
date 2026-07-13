package com.hrms.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 离职申请 - 详情VO
 */
@Data
public class ResignationVO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long deptId;
    private String deptName;
    private Integer resignationType;
    private String resignationTypeLabel;
    private String resignationReason;
    private LocalDate resignationDate;
    private String handoverInfo;
    private Long handoverTo;
    private String handoverToName;
    private Integer status;
    private String statusLabel;
    private Long submitterId;
    private String submitterName;
    private List<ApprovalRecordVO> approvalRecords;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
