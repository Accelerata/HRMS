package com.hrms.annotation;

import java.lang.annotation.*;

/**
 * 操作审计注解
 * 标注在 Controller 方法上，自动记录操作审计日志。
 *
 * 仅记录写操作（创建/修改/删除）和敏感读操作（薪资查看、工资条查看、批量导出）。
 * 不记录普通查询操作。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** 操作类型（如 SALARY_VIEW, PAYSLIP_VIEW, EXPORT, CREATE, UPDATE, DELETE） */
    String operation();

    /** 目标资源类型（如 EMPLOYEE, PAYSLIP, SALARY_BATCH, DEPARTMENT） */
    String resourceType();

    /** 目标资源ID的SpEL表达式（如 #id, #dto.employeeId），可选 */
    String resourceId() default "";
}
