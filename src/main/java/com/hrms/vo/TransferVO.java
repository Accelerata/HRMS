package com.hrms.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 调岗申请 - 详情VO
 */
@Data
public class TransferVO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long fromDeptId;
    private String fromDeptName;
    private Long toDeptId;
    private String toDeptName;
    private Long fromPositionId;
    private String fromPositionName;
    private Long toPositionId;
    private String toPositionName;
    private String transferReason;
    private LocalDate effectiveDate;
    private Integer oldManagerApproved;
    private Integer newManagerApproved;
    private Integer status;
    private String statusLabel;
    private Long submitterId;
    private String submitterName;
    private List<ApprovalRecordVO> approvalRecords;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
