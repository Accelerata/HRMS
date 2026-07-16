package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /**
     * 请假天数（支持0.5天）
     * 上午半天=0.5, 下午半天=0.5, 全天=1.0
     */
    @NotNull(message = "请假天数不能为空")
    private BigDecimal days;

    /** 请假原因 */
    @NotBlank(message = "请假原因不能为空")
    private String reason;

    /** 工作交接人ID（可选） */
    private Long handoverTo;
}
