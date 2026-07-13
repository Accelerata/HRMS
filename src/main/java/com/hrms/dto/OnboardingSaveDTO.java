package com.hrms.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 入职申请 - 保存DTO
 */
@Data
public class OnboardingSaveDTO {

    /** 更新时传ID */
    private Long id;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String idCard;

    @NotNull(message = "目标部门不能为空")
    private Long targetDeptId;

    @NotNull(message = "目标职位不能为空")
    private Long targetPositionId;

    @NotNull(message = "入职薪酬不能为空")
    private BigDecimal offerSalary;

    /** 试用期月数，默认3 */
    private Integer probationMonths;

    @NotNull(message = "入职日期不能为空")
    private String entryDate;

    // ── 员工扩展信息（入职时写入 employee 表） ──

    /** 性别: 0-未知 1-男 2-女 */
    private Integer gender;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    /** 职级（如 P3、M5） */
    private String grade;

    /** 直接汇报人ID */
    private Long reportTo;

    /** 工作地点 */
    private String workLocation;

    /** 入职类型: 1-社招 2-校招 3-内推 4-调动 */
    private Integer entryType;

    /** 薪资账套ID */
    private Long salaryAccountId;

    /** 银行账号 */
    private String bankAccount;

    /** 开户行 */
    private String bankName;
}
