package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色表
 */
@Data
public class SysRole {

    /** 主键ID */
    private Long id;

    /** 角色名称(如：HR专员) */
    private String roleName;

    /** 角色编码(如：ROLE_HR) */
    private String roleCode;

    /** 数据范围：1-全平台, 2-全部员工, 3-本部门及下属, 4-薪资相关, 5-仅本人 */
    private Integer dataScope;

    /** 角色描述 */
    private String description;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
