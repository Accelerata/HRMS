package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.DepartmentSaveDTO;
import com.hrms.dto.DeptEmployeeCountDTO;
import com.hrms.entity.Department;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.vo.DeptTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * DepartmentService 单元测试
 * TDD RED 阶段
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService 部门管理服务")
class DepartmentServiceTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private DepartmentService departmentService;

    // ── 模拟数据 ──
    private Department techDept;
    private Department frontendDept;
    private Department backendDept;

    @BeforeEach
    void setUp() {
        techDept = buildDept(1L, 0L, "技术部", "TECH", 1, 1);
        frontendDept = buildDept(2L, 1L, "前端组", "FE", 2, 1);
        backendDept = buildDept(3L, 1L, "后端组", "BE", 2, 1);
    }

    private Department buildDept(Long id, Long parentId, String name, String code, int level, int sort) {
        Department d = new Department();
        d.setId(id);
        d.setParentId(parentId);
        d.setDeptName(name);
        d.setDeptCode(code);
        d.setLevel(level);
        d.setSortOrder(sort);
        d.setStatus(1);
        return d;
    }

    // ═══════════════ 树形查询 ═══════════════

    @Nested
    @DisplayName("部门树查询")
    class TreeQuery {

        @Test
        @DisplayName("应返回完整的部门树结构")
        void shouldBuildFullTree() {
            when(departmentMapper.selectAll())
                    .thenReturn(Arrays.asList(techDept, frontendDept, backendDept));
            when(employeeMapper.countByDeptGroupByStatus())
                    .thenReturn(List.of());

            List<DeptTreeVO> tree = departmentService.getDeptTree();

            assertNotNull(tree);
            assertEquals(1, tree.size(), "根部门数量应为1");
            DeptTreeVO root = tree.get(0);
            assertEquals("技术部", root.getDeptName());
            assertNotNull(root.getChildren());
            assertEquals(2, root.getChildren().size());
        }

        @Test
        @DisplayName("应实时统计各部门在职员工人数")
        void shouldCountEmployeesPerDept() {
            when(departmentMapper.selectAll())
                    .thenReturn(Arrays.asList(techDept, frontendDept));
            when(employeeMapper.countByDeptGroupByStatus())
                    .thenReturn(Arrays.asList(
                            countRow(1L, 1, 3),  // 技术部: 3个试用期
                            countRow(1L, 2, 5),  // 技术部: 5个正式
                            countRow(2L, 1, 1),  // 前端组: 1个试用期
                            countRow(2L, 2, 2)   // 前端组: 2个正式
                    ));

            List<DeptTreeVO> tree = departmentService.getDeptTree();

            DeptTreeVO root = tree.get(0);
            assertEquals(8, root.getEmployeeCount(), "技术部在职 = 3+5");
            assertEquals(3, root.getProbationCount());
            assertEquals(5, root.getRegularCount());

            DeptTreeVO child = root.getChildren().get(0);
            assertEquals(3, child.getEmployeeCount(), "前端组在职 = 1+2");
            assertEquals(1, child.getProbationCount());
            assertEquals(2, child.getRegularCount());
        }

        @Test
        @DisplayName("空数据应返回空列表")
        void shouldReturnEmptyListWhenNoDepartments() {
            when(departmentMapper.selectAll()).thenReturn(List.of());

            List<DeptTreeVO> tree = departmentService.getDeptTree();

            assertNotNull(tree);
            assertTrue(tree.isEmpty());
        }

        private DeptEmployeeCountDTO countRow(Long deptId, int status, int count) {
            DeptEmployeeCountDTO row = new DeptEmployeeCountDTO();
            row.setDeptId(deptId);
            row.setStatus(status);
            row.setCount(count);
            return row;
        }
    }

    // ═══════════════ 创建部门 ═══════════════

    @Nested
    @DisplayName("创建部门")
    class CreateDept {

        @Test
        @DisplayName("正常创建部门成功")
        void shouldCreateSuccessfully() {
            DepartmentSaveDTO dto = new DepartmentSaveDTO();
            dto.setParentId(1L);
            dto.setDeptName("测试组");
            dto.setDeptCode("TEST");
            dto.setSortOrder(1);

            when(departmentMapper.selectById(1L)).thenReturn(techDept); // parent exists
            when(departmentMapper.countByCode(eq("TEST"), isNull())).thenReturn(0);
            when(departmentMapper.countByNameAndParent(eq("测试组"), eq(1L), isNull())).thenReturn(0);
            when(departmentMapper.insert(any(Department.class))).thenReturn(1);

            assertDoesNotThrow(() -> departmentService.create(dto));
        }

        @Test
        @DisplayName("超过5级深度应抛出异常")
        void shouldRejectDepthExceed5() {
            Department level5 = buildDept(9L, 8L, "五级部门", "L5", 5, 1);

            DepartmentSaveDTO dto = new DepartmentSaveDTO();
            dto.setParentId(9L);
            dto.setDeptName("六级部门");
            dto.setDeptCode("L6");

            when(departmentMapper.selectById(9L)).thenReturn(level5);

            BaseException ex = assertThrows(BaseException.class,
                    () -> departmentService.create(dto));
            assertTrue(ex.getMessage().contains("5级") || ex.getMessage().contains("层级"),
                    "应提示超过最大深度: " + ex.getMessage());
        }

        @Test
        @DisplayName("父部门不存在应抛出异常")
        void shouldRejectWhenParentNotFound() {
            DepartmentSaveDTO dto = new DepartmentSaveDTO();
            dto.setParentId(999L);
            dto.setDeptName("孤儿部门");
            dto.setDeptCode("ORPHAN");

            when(departmentMapper.selectById(999L)).thenReturn(null);

            BaseException ex = assertThrows(BaseException.class,
                    () -> departmentService.create(dto));
            assertTrue(ex.getMessage().contains("父部门") || ex.getMessage().contains("不存在"),
                    "应提示父部门不存在: " + ex.getMessage());
        }

        @Test
        @DisplayName("部门编码重复应抛出异常")
        void shouldRejectDuplicateCode() {
            DepartmentSaveDTO dto = new DepartmentSaveDTO();
            dto.setParentId(0L);
            dto.setDeptName("新部门");
            dto.setDeptCode("TECH");

            when(departmentMapper.countByCode("TECH", null)).thenReturn(1);

            BaseException ex = assertThrows(BaseException.class,
                    () -> departmentService.create(dto));
            assertTrue(ex.getMessage().contains("编码") || ex.getMessage().contains("已存在"),
                    "应提示编码重复: " + ex.getMessage());
        }
    }

    // ═══════════════ 更新部门 ═══════════════

    @Nested
    @DisplayName("更新部门")
    class UpdateDept {

        @Test
        @DisplayName("正常更新成功")
        void shouldUpdateSuccessfully() {
            DepartmentSaveDTO dto = new DepartmentSaveDTO();
            dto.setId(2L);
            dto.setParentId(1L);
            dto.setDeptName("前端工程组");
            dto.setDeptCode("FE");
            dto.setSortOrder(2);

            when(departmentMapper.selectById(2L)).thenReturn(frontendDept);
            when(departmentMapper.selectById(1L)).thenReturn(techDept);
            when(departmentMapper.countByCode("FE", 2L)).thenReturn(0);
            when(departmentMapper.countByNameAndParent("前端工程组", 1L, 2L)).thenReturn(0);
            when(departmentMapper.update(any(Department.class))).thenReturn(1);

            assertDoesNotThrow(() -> departmentService.update(dto));
        }

        @Test
        @DisplayName("不能将自己设为子部门")
        void shouldRejectSelfAsParent() {
            DepartmentSaveDTO dto = new DepartmentSaveDTO();
            dto.setId(1L);
            dto.setParentId(1L); // 自己
            dto.setDeptName("技术部");
            dto.setDeptCode("TECH");

            when(departmentMapper.selectById(1L)).thenReturn(techDept);

            BaseException ex = assertThrows(BaseException.class,
                    () -> departmentService.update(dto));
            assertTrue(ex.getMessage().contains("不能") || ex.getMessage().contains("自身"),
                    "应提示不能设置自身为父部门");
        }
    }

    // ═══════════════ 删除部门 ═══════════════

    @Nested
    @DisplayName("删除部门")
    class DeleteDept {

        @Test
        @DisplayName("有空子部门时不能删除")
        void shouldRejectDeleteWhenHasChildren() {
            when(departmentMapper.selectById(1L)).thenReturn(techDept);
            when(departmentMapper.countByParentId(1L)).thenReturn(2);

            BaseException ex = assertThrows(BaseException.class,
                    () -> departmentService.delete(1L));
            assertTrue(ex.getMessage().contains("子部门"),
                    "应提示存在子部门: " + ex.getMessage());
        }

        @Test
        @DisplayName("有在职员工时不能删除")
        void shouldRejectDeleteWhenHasEmployees() {
            when(departmentMapper.selectById(2L)).thenReturn(frontendDept);
            when(departmentMapper.countByParentId(2L)).thenReturn(0);
            when(employeeMapper.selectByDeptId(2L)).thenReturn(List.of(new com.hrms.entity.Employee()));

            BaseException ex = assertThrows(BaseException.class,
                    () -> departmentService.delete(2L));
            assertTrue(ex.getMessage().contains("员工"),
                    "应提示存在员工: " + ex.getMessage());
        }

        @Test
        @DisplayName("无子部门无员工时正常删除")
        void shouldDeleteSuccessfullyWhenEmpty() {
            when(departmentMapper.selectById(2L)).thenReturn(frontendDept);
            when(departmentMapper.countByParentId(2L)).thenReturn(0);
            when(employeeMapper.selectByDeptId(2L)).thenReturn(List.of());
            when(departmentMapper.deleteById(2L)).thenReturn(1);

            assertDoesNotThrow(() -> departmentService.delete(2L));
        }
    }
}
