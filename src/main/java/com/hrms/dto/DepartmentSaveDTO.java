package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 部门保存/更新 DTO
 */
@Data
public class DepartmentSaveDTO {

    /** 主键ID（更新时必填） */
    private Long id;

    /** 上级部门ID（0 表示根部门） */
    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    /** 部门名称 */
    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    /** 部门编码 */
    @NotBlank(message = "部门编码不能为空")
    private String deptCode;

    /** 排序号 */
    private Integer sortOrder;

    /** 负责人ID */
    private Long managerId;

    /** 状态：0-禁用，1-正常 */
    private Integer status;
}
