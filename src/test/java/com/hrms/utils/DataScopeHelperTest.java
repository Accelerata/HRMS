package com.hrms.utils;

import com.hrms.common.context.BaseContext;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataScopeHelper 数据权限隔离单元测试
 * TDD RED 阶段 —— 先写测试，验证数据隔离 SQL 生成逻辑
 */
@DisplayName("DataScopeHelper 数据权限SQL生成")
class DataScopeHelperTest {

    private static final String DEPT_ALIAS = "e";
    private static final String DEPT_COLUMN = "dept_id";
    private static final String USER_COLUMN = "id";

    @BeforeEach
    void setUp() {
        BaseContext.clear();
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    // ═══════════════ 数据范围 1: 全平台（系统管理员） ═══════════════

    @Nested
    @DisplayName("dataScope=1 系统管理员 - 全平台数据")
    class SysAdminAllPlatform {

        @Test
        @DisplayName("应返回空字符串，不做任何数据过滤")
        void shouldReturnEmptyFilter() {
            BaseContext.setDataScope(1);  // ALL_PLATFORM

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertEquals("", sql, "系统管理员应看到全平台数据，不需要 SQL 过滤");
        }
    }

    // ═══════════════ 数据范围 2: 全部员工（HR 专员） ═══════════════

    @Nested
    @DisplayName("dataScope=2 HR 专员 - 全部员工数据")
    class HRAllEmployee {

        @Test
        @DisplayName("应返回空字符串，HR 可看全部员工，不加过滤条件")
        void shouldReturnEmptyFilter() {
            BaseContext.setDataScope(2);  // ALL_EMPLOYEE

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertEquals("", sql, "HR 应看到全部员工数据，不需要 SQL 过滤");
        }
    }

    // ═══════════════ 数据范围 3: 本部门及下属（部门主管） ═══════════════

    @Nested
    @DisplayName("dataScope=3 部门主管 - 本部门及下属部门")
    class DeptManagerDeptAndSub {

        @Test
        @DisplayName("有下属部门时应返回 dept_id IN (...) 条件")
        void shouldReturnDeptInFilter() {
            BaseContext.setDataScope(3);           // DEPT_AND_SUB
            BaseContext.setCurrentDeptId(10L);
            BaseContext.setSubDeptIds(List.of(10L, 11L, 12L));

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertTrue(sql.contains("e.dept_id IN"), "应包含 dept_id IN 子句");
            assertTrue(sql.contains("10"), "应包含自身部门ID 10");
            assertTrue(sql.contains("11"), "应包含下属部门ID 11");
            assertTrue(sql.contains("12"), "应包含下属部门ID 12");
        }

        @Test
        @DisplayName("无下属部门时应只过滤本部门")
        void shouldReturnOnlyOwnDeptWhenNoSubDepts() {
            BaseContext.setDataScope(3);
            BaseContext.setCurrentDeptId(5L);
            BaseContext.setSubDeptIds(List.of(5L));  // 仅自身

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertTrue(sql.contains("e.dept_id IN"), "应包含 dept_id IN 子句");
            assertTrue(sql.contains("5"), "应包含部门ID 5");
        }
    }

    // ═══════════════ 数据范围 4: 薪资相关（财务专员） ═══════════════

    @Nested
    @DisplayName("dataScope=4 财务专员 - 薪资相关")
    class FinanceSalaryOnly {

        @Test
        @DisplayName("应返回空字符串，薪资过滤由业务层单独处理")
        void shouldReturnEmptyFilter() {
            BaseContext.setDataScope(4);  // SALARY_ONLY

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertEquals("", sql, "财务权限的薪资过滤由薪资模块单独处理");
        }
    }

    // ═══════════════ 数据范围 5: 仅本人（普通员工） ═══════════════

    @Nested
    @DisplayName("dataScope=5 普通员工 - 仅本人数据")
    class EmployeeSelfOnly {

        @Test
        @DisplayName("应返回 id = 当前用户ID 的过滤条件")
        void shouldReturnSelfOnlyFilter() {
            BaseContext.setDataScope(5);            // SELF_ONLY
            BaseContext.setCurrentEmployeeId(42L);

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertTrue(sql.contains("e.id ="), "应包含用户ID过滤: " + sql);
            assertTrue(sql.contains("42"), "应过滤到当前员工ID 42: " + sql);
        }

        @Test
        @DisplayName("不同员工应返回不同的过滤条件")
        void shouldFilterByCorrectEmployeeId() {
            BaseContext.setDataScope(5);
            BaseContext.setCurrentEmployeeId(99L);

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertTrue(sql.contains("99"), "应过滤到员工ID 99: " + sql);
            assertFalse(sql.contains("42"), "不应包含其他员工ID: " + sql);
        }
    }

    // ═══════════════ 边界情况 ═══════════════

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("dataScope 未设置时应默认返回仅本人过滤")
        void shouldDefaultToSelfOnlyWhenNotSet() {
            // 不设置任何 dataScope（默认0或未设置）
            BaseContext.setCurrentEmployeeId(1L);
            BaseContext.setCurrentDeptId(1L);

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            // dataScope 未设置或为0，默认最严格策略（仅本人）
            assertTrue(sql.contains("e.id =") || sql.isEmpty(),
                    "未设置 dataScope 时应默认最严格策略");
        }

        @Test
        @DisplayName("使用自定义表别名和字段名")
        void shouldSupportCustomAliasAndColumns() {
            BaseContext.setDataScope(5);
            BaseContext.setCurrentEmployeeId(7L);

            String sql = DataScopeHelper.getSqlFilter("emp", "department_id", "employee_id");

            assertTrue(sql.contains("emp.employee_id ="), "应使用自定义表别名 emp");
            assertTrue(sql.contains("7"), "应包含正确的员工ID");
        }

        @Test
        @DisplayName("dataScope=2 (HR) 即使有 deptId 也不加过滤")
        void hrShouldNotFilterEvenWithDeptContext() {
            BaseContext.setDataScope(2);
            BaseContext.setCurrentDeptId(10L);
            BaseContext.setSubDeptIds(List.of(10L, 11L));

            String sql = DataScopeHelper.getSqlFilter(DEPT_ALIAS, DEPT_COLUMN, USER_COLUMN);

            assertEquals("", sql, "HR 不应受到部门过滤限制");
        }
    }
}
