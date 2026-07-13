package com.hrms.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调岗申请表
 */
@Data
public class TransferApplication {

    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 原部门ID */
    private Long fromDeptId;
    /** 新部门ID */
    private Long toDeptId;
    /** 原职位ID */
    private Long fromPositionId;
    /** 新职位ID */
    private Long toPositionId;
    /** 调岗原因 */
    private String transferReason;
    /** 生效日期 */
    private LocalDate effectiveDate;
    /** 原部门负责人审批: 0-未审 1-通过 2-拒绝 */
    private Integer oldManagerApproved;
    /** 新部门负责人审批: 0-未审 1-通过 2-拒绝 */
    private Integer newManagerApproved;
    /** 状态: 0-草稿 1-审批中 2-已通过 3-已拒绝 */
    private Integer status;
    /** 提交人ID */
    private Long submitterId;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
