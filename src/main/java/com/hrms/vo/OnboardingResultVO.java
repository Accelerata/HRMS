package com.hrms.vo;

import lombok.Data;

/**
 * 入职审批通过结果VO
 */
@Data
public class OnboardingResultVO {

    private Long employeeId;
    private String employeeNo;
    private String username;
    private String initialPassword;
}
