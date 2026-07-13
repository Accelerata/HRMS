package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工保存/更新 DTO
 */
@Data
public class EmployeeSaveDTO {

    /** 主键ID（更新时必填） */
    private Long id;

    // ── 必填 — 基础信息 ──

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    // ── 可填 — 个人信息 ──

    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;

    /** 生日（不填则从身份证号自动提取） */
    private LocalDate birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    // ── 必填 — 工作信息 ──

    @NotNull(message = "所属部门不能为空")
    private Long deptId;

    @NotNull(message = "职位不能为空")
    private Long positionId;

    /** 职级（如 P3、M5） */
    private String grade;

    /** 直接汇报人ID */
    private Long reportTo;

    /** 工作地点 */
    private String workLocation;

    /** 入职类型: 1-社招 2-校招 3-内推 4-调动 */
    @NotNull(message = "入职类型不能为空")
    private Integer entryType;

    /** 入职日期 */
    @NotNull(message = "入职日期不能为空")
    private LocalDate entryDate;

    // ── 薪资信息（HR/财务可见） ──

    /** 薪资账套ID */
    private Long salaryAccountId;

    /** 基本工资（精确到分） */
    private BigDecimal baseSalary;

    /** 银行账号 */
    private String bankAccount;

    /** 开户行 */
    private String bankName;

    // ── 状态 ──

    /** 员工状态: 0-待入职 1-试用期 2-正式 3-待离职 4-已离职（默认1-试用期） */
    private Integer status;
}
