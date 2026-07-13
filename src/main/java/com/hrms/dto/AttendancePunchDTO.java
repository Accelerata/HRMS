package com.hrms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * 打卡请求 DTO
 */
@Data
public class AttendancePunchDTO {

    /** 员工ID */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /** 考勤组ID */
    @NotNull(message = "考勤组ID不能为空")
    private Long groupId;

    /** 打卡时间 */
    @NotNull(message = "打卡时间不能为空")
    private LocalTime punchTime;
}
