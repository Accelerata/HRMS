package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.Department;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 部门合并测试 (任务 4.11)
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("DepartmentService 部门合并测试")
class DepartmentMergeTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department techDept;
    private Department devDept;

    private Department deptA; // 技术部
    private Department deptB; // 产品部（同级，非祖先关系）

    @BeforeEach
    void setUp() {
        deptA = new Department();
        deptA.setId(1L);
        deptA.setParentId(0L);
        deptA.setDeptName("技术部");
        deptA.setDeptCode("TECH");
        deptA.setLevel(1);
        deptA.setStatus(1);

        deptB = new Department();
        deptB.setId(2L);
        deptB.setParentId(0L); // 同级根部门
        deptB.setDeptName("产品部");
        deptB.setDeptCode("PROD");
        deptB.setLevel(1);
        deptB.setStatus(1);
    }

    @Test
    @DisplayName("4.7 正常合并（同级部门）→ 员工转移 + 源部门标记已合并")
    void shouldMergeSuccessfully() {
        when(departmentMapper.selectById(1L)).thenReturn(deptA);
        when(departmentMapper.selectById(2L)).thenReturn(deptB);
        when(employeeMapper.updateDeptByDept(eq(1L), eq(2L))).thenReturn(1);

        assertDoesNotThrow(() -> departmentService.mergeDepartments(1L, 2L));

        verify(employeeMapper).updateDeptByDept(1L, 2L);
        verify(departmentMapper).update(deptA);
        assertEquals(2, deptA.getStatus(), "源部门应标记为已合并");
    }

    @Test
    @DisplayName("4.7 源部门不存在 → 抛异常")
    void shouldThrowWhenSourceDeptNotFound() {
        when(departmentMapper.selectById(999L)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> departmentService.mergeDepartments(999L, 1L));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("4.7 源部门是目标部门祖先 → 拒绝合并")
    void shouldRejectWhenSourceIsAncestor() {
        Department parent = new Department();
        parent.setId(1L);
        parent.setParentId(0L);
        parent.setDeptName("父部门");
        parent.setLevel(1);
        parent.setStatus(1);

        Department child = new Department();
        child.setId(3L);
        child.setParentId(1L); // child 的 parent 是 1
        child.setDeptName("子部门");
        child.setLevel(2);
        child.setStatus(1);

        // 把父部门(id=1)合并到子部门(id=3) → 应拒绝
        when(departmentMapper.selectById(1L)).thenReturn(parent);
        when(departmentMapper.selectById(3L)).thenReturn(child);

        BaseException ex = assertThrows(BaseException.class,
                () -> departmentService.mergeDepartments(1L, 3L));
        assertTrue(ex.getMessage().contains("上级部门"));
    }

    @Test
    @DisplayName("4.8 递归CTE查询子部门")
    void shouldQuerySubDeptsRecursively() {
        java.util.List<Long> subIds = new java.util.ArrayList<>(java.util.List.of(2L, 3L, 4L));
        when(departmentMapper.selectSubDeptIdsRecursive(1L))
                .thenReturn(subIds);
        when(employeeMapper.countActiveByDeptIds(anyList())).thenReturn(42);

        int count = departmentService.getEmployeeCountRecursive(1L);
        assertEquals(42, count);
    }
}
