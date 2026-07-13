package com.hrms.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 离职申请表
 */
@Data
public class ResignationApplication {

    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 离职类型: 1-主动 2-协商 3-合同到期 4-裁员 */
    private Integer resignationType;
    /** 离职原因 */
    private String resignationReason;
    /** 最后工作日 */
    private LocalDate resignationDate;
    /** 交接事项 */
    private String handoverInfo;
    /** 接手人ID */
    private Long handoverTo;
    /** 状态: 0-草稿 1-审批中 2-已通过 3-已拒绝 */
    private Integer status;
    /** 提交人ID */
    private Long submitterId;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
