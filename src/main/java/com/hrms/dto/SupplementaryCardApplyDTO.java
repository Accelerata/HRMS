package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 补卡申请 DTO
 */
@Data
public class SupplementaryCardApplyDTO {

    /** 补卡日期 */
    @NotNull(message = "补卡日期不能为空")
    private LocalDate attendanceDate;

    /** 卡型: 1-上班卡 2-下班卡 */
    @NotNull(message = "卡型不能为空")
    private Integer cardType;

    /** 补卡时间 */
    @NotNull(message = "补卡时间不能为空")
    private LocalTime supplementTime;

    /** 补卡事由 */
    @NotBlank(message = "补卡事由不能为空")
    private String reason;
}
