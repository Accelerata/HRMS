package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 请假申请 DTO
 */
@Data
public class LeaveApplyDTO {

    /** 员工ID（可选，默认取当前登录用户关联员工；提供时须与本人一致） */
    private Long employeeId;

    /**
     * 假期类型：
     * 1-年假 2-调休 3-事假 4-病假 5-婚假 6-产假 7-丧假
     */
    @NotNull(message = "假期类型不能为空")
    private Integer leaveType;

    /** 请假开始日期 */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /** 请假结束日期 */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /** 开始时段：0-上午 1-下午（默认上午） */
    private Integer startPeriod;

    /** 结束时段：0-上午 1-下午（默认下午） */
    private Integer endPeriod;

    /**
     * 请假天数（非必填，服务端权威计算后覆盖）
     */
    private BigDecimal days;

    /** 请假原因 */
    @NotBlank(message = "请假原因不能为空")
    private String reason;

    /** 工作交接人ID（可选） */
    private Long handoverTo;

    /** 已上传附件ID列表（可选，绑定 leave_attachment） */
    private List<Long> attachmentIds;
}
