package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.TransferSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.EmployeeStatusEnum;
import com.hrms.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransferService 调岗三级审批测试 + 发起状态校验
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransferService 调岗流程")
class TransferServiceTest {

    @Mock private TransferApplicationMapper transferMapper;
    @Mock private ApprovalStateMachineService stateMachine;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeTransferMapper transferLogMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService;

    private Employee employee;
    private TransferApplication entity;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1L);

        employee = new Employee();
        employee.setId(10L);
        employee.setName("王五");
        employee.setDeptId(1L);
        employee.setPositionId(5L);
        employee.setGrade("P3");
        employee.setReportTo(100L);
        employee.setStatus(EmployeeStatusEnum.REGULAR.getCode());

        entity = new TransferApplication();
        entity.setId(1L);
        entity.setEmployeeId(10L);
        entity.setFromDeptId(1L);
        entity.setToDeptId(2L);
        entity.setFromPositionId(5L);
        entity.setToPositionId(6L);
        entity.setToGrade("P4");
        entity.setEffectiveDate(LocalDate.of(2026, 8, 1));
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());

        // mock insert to set id
        doAnswer(inv -> {
            TransferApplication e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        }).when(transferMapper).insert(any());
    }

    // ═══════════════ 提交校验 ═══════════════

    @Nested
    @DisplayName("提交状态校验")
    class SubmitValidation {

        @Test
        @DisplayName("仅试用期和正式员工可调岗")
        void shouldAllowProbationAndRegular() {
            // 试用期
            employee.setStatus(EmployeeStatusEnum.PROBATION.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            TransferSaveDTO dto = buildDTO();
            dto.setToDeptId(2L);

            transferService.submit(dto);
            verify(transferMapper).insert(any());
        }

        @Test
        @DisplayName("待入职状态不能调岗")
        void shouldRejectPendingEntry() {
            employee.setStatus(EmployeeStatusEnum.PENDING_ENTRY.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            TransferSaveDTO dto = buildDTO();

            BaseException ex = assertThrows(BaseException.class,
                    () -> transferService.submit(dto));
            assertTrue(ex.getMessage().contains("不可调岗"));
        }

        @Test
        @DisplayName("已离职状态不能调岗")
        void shouldRejectResigned() {
            employee.setStatus(EmployeeStatusEnum.RESIGNED.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            TransferSaveDTO dto = buildDTO();

            BaseException ex = assertThrows(BaseException.class,
                    () -> transferService.submit(dto));
            assertTrue(ex.getMessage().contains("不可调岗"));
        }

        @Test
        @DisplayName("新部门必须与原部门不同")
        void shouldRejectSameDept() {
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            TransferSaveDTO dto = buildDTO();
            dto.setToDeptId(1L); // 同部门

            BaseException ex = assertThrows(BaseException.class,
                    () -> transferService.submit(dto));
            assertTrue(ex.getMessage().contains("必须与原部门不同"));
        }
    }

    // ═══════════════ 三级审批 ═══════════════

    @Nested
    @DisplayName("三级顺序审批 + 薪资调整额外审批")
    class ThreeLevelApproval {

        @Test
        @DisplayName("无薪资调整时启动三级审批（原部门→新部门→HR）")
        void shouldStartThreeLevelApprovalWithoutSalary() {
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            TransferSaveDTO dto = buildDTO();
            dto.setSalaryAdjust(null); // 无薪资调整

            transferService.submit(dto);

            ArgumentCaptor<ApprovalStateMachineService.ApprovalContext> ctxCaptor =
                    ArgumentCaptor.forClass(ApprovalStateMachineService.ApprovalContext.class);
            verify(stateMachine).startApproval(
                    eq(BusinessTypeEnum.TRANSFER.getCode()), eq(1L), ctxCaptor.capture());

            ApprovalStateMachineService.ApprovalContext ctx = ctxCaptor.getValue();
            assertFalse(ctx.isHasSalaryAdjust(), "无薪资调整时 hasSalaryAdjust 为 false");
        }

        @Test
        @DisplayName("有薪资调整时启动四级审批（含财务薪资审核）")
        void shouldStartFourLevelApprovalWithSalary() {
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            TransferSaveDTO dto = buildDTO();
            dto.setSalaryAdjust(new BigDecimal("5000.00"));

            transferService.submit(dto);

            ArgumentCaptor<ApprovalStateMachineService.ApprovalContext> ctxCaptor =
                    ArgumentCaptor.forClass(ApprovalStateMachineService.ApprovalContext.class);
            verify(stateMachine).startApproval(
                    eq(BusinessTypeEnum.TRANSFER.getCode()), eq(1L), ctxCaptor.capture());

            ApprovalStateMachineService.ApprovalContext ctx = ctxCaptor.getValue();
            assertTrue(ctx.isHasSalaryAdjust(), "有薪资调整时 hasSalaryAdjust 为 true");
        }

        @Test
        @DisplayName("三级全部通过后执行调岗生效")
        void shouldExecuteTransferWhenAllApproved() {
            when(transferMapper.selectById(1L)).thenReturn(entity);
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ApprovalRecord record = new ApprovalRecord();
            record.setId(1L);
            record.setBusinessType(BusinessTypeEnum.TRANSFER.getCode());
            record.setBusinessId(1L);
            record.setApproverId(1L);
            record.setIsPending(1);
            when(stateMachine.getApprovalRecords(anyInt(), eq(1L))).thenReturn(List.of(record));

            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setTerminated(false);
            result.setApproved(true);
            when(stateMachine.processApproval(eq(1L), eq(1), any(), eq(1L))).thenReturn(result);

            ApprovalActionDTO action = new ApprovalActionDTO();
            action.setAction(1);

            transferService.approve(1L, action);

            // 申请单状态
            assertEquals(ApprovalStatusEnum.APPROVED.getCode(), entity.getStatus());
            verify(transferMapper).update(entity);

            // 员工更新：部门、职位、职级、汇报人
            assertEquals(2L, employee.getDeptId());
            assertEquals(6L, employee.getPositionId());
            assertEquals("P4", employee.getGrade());
            verify(employeeMapper).update(employee);

            // 异动日志（含 before/after）
            verify(transferLogMapper).insert(any(EmployeeTransfer.class));
        }
    }

    // ═══════════════ 辅助方法 ═══════════════

    private TransferSaveDTO buildDTO() {
        TransferSaveDTO dto = new TransferSaveDTO();
        dto.setEmployeeId(10L);
        dto.setToDeptId(2L);
        dto.setToPositionId(6L);
        dto.setToGrade("P4");
        dto.setToReportTo(101L);
        dto.setTransferReason("业务需要");
        dto.setEffectiveDate("2026-08-01");
        return dto;
    }
}
