package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.enums.SensitiveFieldPolicy;
import com.hrms.vo.EmployeeVO;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字段级权限过滤测试 (任务 1.7)
 *
 * 验证：
 * - Manager 查下属 → 敏感字段为空
 * - Employee 查自己 → 完整返回
 * - Employee 查他人 → 仅基本信息
 * - HR 查任意 → 全部可见
 */
@DisplayName("字段级权限过滤测试")
class FieldPermissionTest {

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        // 创建一个不依赖 Spring 的轻量 Service 用于测试 toVO/applyFieldFilter
        employeeService = new EmployeeService(null, null, null);
    }

    private EmployeeVO buildFullVO() {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(1L);
        vo.setEmployeeNo("202601001");
        vo.setName("张三");
        vo.setGender(1);
        vo.setPhone("13800138000");
        vo.setEmail("zhangsan@hrms.com");
        vo.setIdCard("440101199508080012");
        vo.setBankAccount("6228480012345678912");
        vo.setBankName("工商银行");
        vo.setBaseSalary(new BigDecimal("15000"));
        vo.setSalaryAccountId(10L);
        vo.setDeptId(1L);
        vo.setDeptName("技术部");
        vo.setPositionId(5L);
        vo.setGrade("P6");
        vo.setStatus(2);
        return vo;
    }

    @Test
    @DisplayName("1.7 HR(数据范围1) → 全部字段可见")
    void shouldShowAllForHr() {
        BaseContext.setDataScope(1); // ALL_PLATFORM → SHOW_ALL
        EmployeeVO vo = buildFullVO();
        employeeService.applyFieldFilter(vo, 1L);

        assertNotNull(vo.getPhone());
        assertNotNull(vo.getIdCard());
        assertNotNull(vo.getBankAccount());
        assertNotNull(vo.getBaseSalary());
    }

    @Test
    @DisplayName("1.7 部门主管(数据范围3=DEPT_AND_SUB) → 身份证/薪资/银行卡隐藏")
    void shouldHideSensitiveForManager() {
        BaseContext.setDataScope(3); // DEPT_AND_SUB → HIDE_SALARY_BANK
        EmployeeVO vo = buildFullVO();
        employeeService.applyFieldFilter(vo, 2L);

        assertNull(vo.getIdCard(), "主管不应看到身份证");
        assertNull(vo.getSalaryAccountId(), "主管不应看到薪资账套");
        assertNull(vo.getBaseSalary(), "主管不应看到基本工资");
        assertNull(vo.getBankAccount(), "主管不应看到银行卡");
        assertNotNull(vo.getName(), "主管应看到姓名");
        assertNotNull(vo.getDeptName(), "主管应看到部门");
    }

    @Test
    @DisplayName("1.7 员工查自己(dataScope=5, currentEmployeeId=1) → 完整返回")
    void shouldShowFullForSelf() {
        BaseContext.setDataScope(5); // SELF_ONLY
        BaseContext.setCurrentEmployeeId(1L);
        EmployeeVO vo = buildFullVO();
        employeeService.applyFieldFilter(vo, 1L);

        assertNotNull(vo.getPhone());
        assertNotNull(vo.getIdCard());
        assertNotNull(vo.getBankAccount());
        assertNotNull(vo.getBaseSalary());
    }

    @Test
    @DisplayName("1.7 员工查他人(dataScope=5, currentEmployeeId=1, target=2) → 仅基本信息")
    void shouldOnlyShowBasicForOtherEmployee() {
        BaseContext.setDataScope(5); // SELF_ONLY
        BaseContext.setCurrentEmployeeId(1L);
        EmployeeVO vo = buildFullVO();
        employeeService.applyFieldFilter(vo, 2L); // 查员工ID=2，非本人

        assertEquals("张三", vo.getName(), "应保留姓名");
        assertEquals("技术部", vo.getDeptName(), "应保留部门");
        assertNull(vo.getPhone(), "不应看到手机号");
        assertNull(vo.getIdCard(), "不应看到身份证");
        assertNull(vo.getBankAccount(), "不应看到银行卡");
        assertNull(vo.getBaseSalary(), "不应看到薪资");
    }

    @Test
    @DisplayName("1.7 无上下文 → 不过滤")
    void shouldNotFilterWhenNoContext() {
        BaseContext.clear();
        EmployeeVO vo = buildFullVO();
        employeeService.applyFieldFilter(vo, 1L);

        assertNotNull(vo.getPhone());
        assertNotNull(vo.getIdCard());
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }
}
