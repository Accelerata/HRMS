package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.dto.PositionSaveDTO;
import com.hrms.entity.Position;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.PositionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * PositionService 单元测试
 * TDD RED 阶段
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PositionService 职位管理服务")
class PositionServiceTest {

    @Mock
    private PositionMapper positionMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private PositionService positionService;

    private Position p1, p2;

    @BeforeEach
    void setUp() {
        p1 = buildPosition(1L, "初级工程师", "P1", "P", "P1-P2", 3);
        p2 = buildPosition(2L, "高级工程师", "P3", "P", "P3-P4", 6);
    }

    private Position buildPosition(Long id, String name, String code, String seq, String grade, int probation) {
        Position p = new Position();
        p.setId(id);
        p.setPositionName(name);
        p.setPositionCode(code);
        p.setSequence(seq);
        p.setGradeRange(grade);
        p.setDefaultProbationMonths(probation);
        p.setStatus(1);
        return p;
    }

    // ═══════════════ 查询 ═══════════════

    @Nested
    @DisplayName("职位查询")
    class Query {

        @Test
        @DisplayName("应返回所有有效职位")
        void shouldReturnAllActivePositions() {
            when(positionMapper.selectAll()).thenReturn(List.of(p1, p2));

            List<Position> result = positionService.listAll();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("按序列筛选 M/P/S")
        void shouldFilterBySequence() {
            when(positionMapper.selectBySequence("P")).thenReturn(List.of(p1, p2));

            List<Position> result = positionService.listBySequence("P");

            assertEquals(2, result.size());
            result.forEach(p -> assertEquals("P", p.getSequence()));
        }

        @Test
        @DisplayName("非法序列应抛出异常")
        void shouldRejectInvalidSequence() {
            BaseException ex = assertThrows(BaseException.class,
                    () -> positionService.listBySequence("X"));
            assertTrue(ex.getMessage().contains("序列"));
        }
    }

    // ═══════════════ 创建 ═══════════════

    @Nested
    @DisplayName("创建职位")
    class Create {

        @Test
        @DisplayName("正常创建含试用期的职位")
        void shouldCreateWithDefaultProbation() {
            PositionSaveDTO dto = new PositionSaveDTO();
            dto.setPositionName("技术总监");
            dto.setPositionCode("M3");
            dto.setSequence("M");
            dto.setGradeRange("M3-M4");
            dto.setDefaultProbationMonths(6);
            dto.setDescription("管理序列");

            when(positionMapper.countByCode("M3", null)).thenReturn(0);
            when(positionMapper.insert(any(Position.class))).thenReturn(1);

            assertDoesNotThrow(() -> positionService.create(dto));
        }

        @Test
        @DisplayName("序列不是 M/P/S 应抛出异常")
        void shouldRejectInvalidSequence() {
            PositionSaveDTO dto = new PositionSaveDTO();
            dto.setPositionName("测试");
            dto.setPositionCode("T1");
            dto.setSequence("T"); // 非法
            dto.setDefaultProbationMonths(3);

            BaseException ex = assertThrows(BaseException.class,
                    () -> positionService.create(dto));
            assertTrue(ex.getMessage().contains("M/P/S") || ex.getMessage().contains("序列"),
                    "应提示序列必须为M/P/S: " + ex.getMessage());
        }

        @Test
        @DisplayName("试用期为0或负数应抛出异常")
        void shouldRejectInvalidProbationMonths() {
            PositionSaveDTO dto = new PositionSaveDTO();
            dto.setPositionName("测试");
            dto.setPositionCode("P5");
            dto.setSequence("P");
            dto.setDefaultProbationMonths(0);

            BaseException ex = assertThrows(BaseException.class,
                    () -> positionService.create(dto));
            assertTrue(ex.getMessage().contains("试用期"),
                    "应提示试用期不合法: " + ex.getMessage());
        }

        @Test
        @DisplayName("编码重复应抛出异常")
        void shouldRejectDuplicateCode() {
            PositionSaveDTO dto = new PositionSaveDTO();
            dto.setPositionName("重复职位");
            dto.setPositionCode("P1");
            dto.setSequence("P");
            dto.setDefaultProbationMonths(3);

            when(positionMapper.countByCode("P1", null)).thenReturn(1);

            BaseException ex = assertThrows(BaseException.class,
                    () -> positionService.create(dto));
            assertTrue(ex.getMessage().contains("编码") || ex.getMessage().contains("已存在"));
        }
    }

    // ═══════════════ 更新 ═══════════════

    @Nested
    @DisplayName("更新职位")
    class Update {

        @Test
        @DisplayName("正常更新成功")
        void shouldUpdateSuccessfully() {
            PositionSaveDTO dto = new PositionSaveDTO();
            dto.setId(1L);
            dto.setPositionName("中级工程师");
            dto.setPositionCode("P2");
            dto.setSequence("P");
            dto.setGradeRange("P2-P3");
            dto.setDefaultProbationMonths(3);

            when(positionMapper.selectById(1L)).thenReturn(p1);
            when(positionMapper.countByCode("P2", 1L)).thenReturn(0);
            when(positionMapper.update(any(Position.class))).thenReturn(1);

            assertDoesNotThrow(() -> positionService.update(dto));
        }

        @Test
        @DisplayName("职位不存在应抛出异常")
        void shouldRejectWhenNotFound() {
            PositionSaveDTO dto = new PositionSaveDTO();
            dto.setId(999L);
            dto.setPositionName("不存在");
            dto.setPositionCode("X1");
            dto.setSequence("P");
            dto.setDefaultProbationMonths(3);

            when(positionMapper.selectById(999L)).thenReturn(null);

            assertThrows(BaseException.class, () -> positionService.update(dto));
        }
    }

    // ═══════════════ 删除 ═══════════════

    @Nested
    @DisplayName("删除职位")
    class Delete {

        @Test
        @DisplayName("无人使用的职位可删除")
        void shouldDeleteWhenNoEmployees() {
            when(positionMapper.selectById(1L)).thenReturn(p1);
            when(employeeMapper.countByPositionId(1L)).thenReturn(0);
            when(positionMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> positionService.delete(1L));
        }

        @Test
        @DisplayName("有员工使用的职位不能删除")
        void shouldRejectWhenInUse() {
            when(positionMapper.selectById(1L)).thenReturn(p1);
            when(employeeMapper.countByPositionId(1L)).thenReturn(5);

            BaseException ex = assertThrows(BaseException.class,
                    () -> positionService.delete(1L));
            assertTrue(ex.getMessage().contains("使用中") || ex.getMessage().contains("员工"),
                    "应提示存在关联员工: " + ex.getMessage());
        }
    }
}
