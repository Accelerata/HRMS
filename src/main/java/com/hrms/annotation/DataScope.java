package com.hrms.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标记在 Mapper 方法上，由 DataScopeAspect 自动拼接数据隔离 SQL
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /** 部门表别名（SQL 中使用的别名） */
    String deptAlias() default "d";

    /** 部门关联字段 */
    String deptColumn() default "dept_id";

    /** 用户/员工 ID 字段 */
    String userColumn() default "id";
}
