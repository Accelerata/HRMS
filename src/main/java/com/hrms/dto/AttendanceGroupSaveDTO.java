package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * 考勤组保存 DTO
 */
@Data
public class AttendanceGroupSaveDTO {

    /** 更新时传ID */
    private Long id;

    /** 考勤组名称 */
    @NotBlank(message = "考勤组名称不能为空")
    private String groupName;

    /**
     * 考勤组类型：
     * 1-固定班 2-弹性班
     */
    @NotNull(message = "考勤组类型不能为空")
    private Integer groupType;

    /** 规定上班时间 */
    @NotNull(message = "上班时间不能为空")
    private LocalTime startTime;

    /** 规定下班时间 */
    @NotNull(message = "下班时间不能为空")
    private LocalTime endTime;

    /** 弹性阈值（分钟） */
    private Integer flexThreshold;

    /** 半天旷工阈值（分钟），默认120 */
    private Integer absentHalfDayThreshold;
}
