package com.hrms.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 员工列表 VO — 仅包含列表展示所需字段
 */
@Data
public class EmployeeListVO {

    /** 主键ID */
    private Long id;

    /** 员工工号 */
    private String employeeNo;

    /** 姓名 */
    private String name;

    /** 部门名称（JOIN 查询） */
    private String deptName;

    /** 职位名称（JOIN 查询） */
    private String positionName;

    /** 职级（如 P3、M5） */
    private String grade;

    /** 状态: 0-待入职 1-试用期 2-正式 3-待离职 4-已离职 */
    private Integer status;

    /** 状态文字 */
    private String statusLabel;

    /** 入职日期 */
    private LocalDate entryDate;
}
