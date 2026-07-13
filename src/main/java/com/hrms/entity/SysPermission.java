package com.hrms.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限菜单表
 */
@Data
public class SysPermission {

    /** 主键ID */
    private Long id;

    /** 父级权限ID(0为顶级) */
    private Long parentId;

    /** 菜单或按钮名称 */
    private String menuName;

    /** 权限标识(如: emp:view, emp:salary:view) */
    private String permissionCode;

    /** 类型：1-目录，2-菜单，3-按钮/字段控制 */
    private Integer type;

    /** 前端路由地址 */
    private String path;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
