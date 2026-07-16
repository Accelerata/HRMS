package com.hrms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 职位保存/更新 DTO
 */
@Data
public class PositionSaveDTO {

    /** 主键ID（更新时必填） */
    private Long id;

    /** 职位名称 */
    @NotBlank(message = "职位名称不能为空")
    private String positionName;

    /** 职位编码 */
    @NotBlank(message = "职位编码不能为空")
    private String positionCode;

    /** 职位序列：M/P/S */
    @NotBlank(message = "职位序列不能为空")
    private String sequence;

    /** 职级范围 */
    private String gradeRange;

    /** 默认试用期（月） */
    @NotNull(message = "默认试用期不能为空")
    private Integer defaultProbationMonths;

    /** 所属部门ID（为空表示全公司通用） */
    private Long deptId;

    /** 职位描述 */
    private String description;

    /** 是否标准职位：1-标准职位 0-非标准职位（非标准职位入职需HR二审） */
    private Integer isStandard;

    /** 状态：0-禁用，1-正常 */
    private Integer status;
}
