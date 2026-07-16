package com.hrms.service;

import com.hrms.dto.EmployeeQueryDTO;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.crypto.EncryptionUtil;
import com.hrms.vo.EmployeeListVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 员工高级搜索测试 (任务 5.6)
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("EmployeeService 高级搜索测试")
class EmployeeAdvancedSearchTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private com.hrms.mapper.DepartmentMapper departmentMapper;

    @Mock
    private EncryptionUtil encryptionUtil;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeMapper, departmentMapper, encryptionUtil);
    }

    @Test
    @DisplayName("5.6 单条件筛选 → 按部门返回")
    void shouldFilterByDeptOnly() {
        EmployeeQueryDTO dto = new EmployeeQueryDTO();
        dto.setDeptIds(List.of(1L));

        EmployeeListVO vo = new EmployeeListVO();
        vo.setId(1L);
        vo.setName("张三");
        vo.setDeptName("技术部");

        when(employeeMapper.selectByConditions(eq(dto), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(vo));
        when(employeeMapper.countByConditions(eq(dto), isNull())).thenReturn(1);

        List<EmployeeListVO> result = employeeService.queryEmployees(dto, 1, 10);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getName());
    }

    @Test
    @DisplayName("5.6 多条件AND组合 → 部门+职位+状态+职级")
    void shouldCombineMultipleConditions() {
        EmployeeQueryDTO dto = new EmployeeQueryDTO();
        dto.setDeptIds(List.of(1L, 2L));
        dto.setPositionIds(List.of(10L));
        dto.setStatuses(List.of(1, 2));
        dto.setGrades(List.of("P5", "P6"));

        when(employeeMapper.selectByConditions(eq(dto), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(employeeMapper.countByConditions(eq(dto), isNull())).thenReturn(0);

        List<EmployeeListVO> result = employeeService.queryEmployees(dto, 1, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("5.6 日期范围筛选 → startDate ~ endDate")
    void shouldFilterByDateRange() {
        EmployeeQueryDTO dto = new EmployeeQueryDTO();
        dto.setStartDate(LocalDate.of(2025, 1, 1));
        dto.setEndDate(LocalDate.of(2025, 12, 31));

        when(employeeMapper.selectByConditions(eq(dto), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(employeeMapper.countByConditions(eq(dto), isNull())).thenReturn(0);

        List<EmployeeListVO> result = employeeService.queryEmployees(dto, 1, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("5.6 空条件 → 返回全部（无筛选）")
    void shouldReturnAllWhenNoConditions() {
        EmployeeQueryDTO dto = new EmployeeQueryDTO();

        when(employeeMapper.selectByConditions(eq(dto), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(employeeMapper.countByConditions(eq(dto), isNull())).thenReturn(0);

        List<EmployeeListVO> result = employeeService.queryEmployees(dto, 1, 10);
        assertNotNull(result);
    }
}
