package com.hrms.vo;

import lombok.Data;
import java.time.LocalDate;

/**
 * 试用期即将到期员工VO（转正预警）
 */
@Data
public class ExpiringEmployeeVO {

    private Long employeeId;
    private String employeeName;
    private String deptName;
    private LocalDate hireDate;
    private LocalDate probationEndDate;
    /** 剩余天数 */
    private Integer daysRemaining;
}
