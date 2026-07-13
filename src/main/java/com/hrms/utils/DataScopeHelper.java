package com.hrms.utils;

import com.hrms.common.context.BaseContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限 SQL 过滤工具类
 * 根据 BaseContext 中当前用户的 dataScope 生成对应的 SQL 过滤条件
 *
 * 数据范围说明：
 *   1 - 全平台（系统管理员）：不加过滤
 *   2 - 全部员工（HR 专员）：不加过滤
 *   3 - 本部门及下属（部门主管）：WHERE dept_id IN (...)
 *   4 - 薪资相关（财务专员）：不加通用过滤，由薪资模块单独处理
 *   5 - 仅本人（普通员工）：WHERE id = currentEmployeeId
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
        // 工具类禁止实例化
    }

    /**
     * 根据当前登录用户的数据权限范围，生成 SQL 过滤条件
     *
     * @param deptAlias  部门表别名
     * @param deptColumn 部门字段名
     * @param userColumn 用户/员工 ID 字段名
     * @return SQL WHERE 条件片段（不含 "WHERE" 关键字，以 " AND " 开头或为空字符串）
     */
    public static String getSqlFilter(String deptAlias, String deptColumn, String userColumn) {
        Integer dataScope = BaseContext.getDataScope();

        // 未设置默认为最严格策略
        if (dataScope == null) {
            dataScope = 5; // SELF_ONLY
        }

        switch (dataScope) {
            case 1: // ALL_PLATFORM - 系统管理员
            case 2: // ALL_EMPLOYEE - HR 专员
            case 4: // SALARY_ONLY - 财务专员（由薪资模块单独处理）
                return "";

            case 3: // DEPT_AND_SUB - 部门主管
                return buildDeptAndSubFilter(deptAlias, deptColumn);

            case 5: // SELF_ONLY - 普通员工
                return buildSelfOnlyFilter(deptAlias, userColumn);

            default:
                // 未知 dataScope，默认最严格策略
                return buildSelfOnlyFilter(deptAlias, userColumn);
        }
    }

    /** 构建"本部门及下属"过滤条件 */
    private static String buildDeptAndSubFilter(String deptAlias, String deptColumn) {
        List<Long> deptIds = BaseContext.getSubDeptIds();
        if (deptIds.isEmpty()) {
            // 如果没有下级部门，仅过滤本部门
            Long deptId = BaseContext.getCurrentDeptId();
            if (deptId == null) {
                return "";
            }
            return String.format(" AND %s.%s = %d", deptAlias, deptColumn, deptId);
        }

        String ids = deptIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return String.format(" AND %s.%s IN (%s)", deptAlias, deptColumn, ids);
    }

    /** 构建"仅本人"过滤条件 */
    private static String buildSelfOnlyFilter(String deptAlias, String userColumn) {
        Long employeeId = BaseContext.getCurrentEmployeeId();
        if (employeeId == null) {
            return "";
        }
        return String.format(" AND %s.%s = %d", deptAlias, userColumn, employeeId);
    }
}
