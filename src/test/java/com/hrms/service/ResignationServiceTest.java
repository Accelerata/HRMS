package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.ResignationSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.EmployeeStatusEnum;
import com.hrms.enums.ResignationTypeEnum;
import com.hrms.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ResignationService 离职全链路测试
 * 验证：提交校验 → 审批通过置待离职 → 定时过渡已离职 → 账号禁用
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ResignationService 离职流程")
class ResignationServiceTest {

    @Mock private ResignationApplicationMapper resignationMapper;
    @Mock private ApprovalStateMachineService stateMachine;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeTransferMapper transferMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private EmployeeAccountService employeeAccountService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ResignationService resignationService;

    private Employee employee;
    private ResignationApplication entity;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1L);

        employee = new Employee();
        employee.setId(10L);
        employee.setName("赵六");
        employee.setDeptId(1L);
        employee.setUserId(50L);
        employee.setStatus(EmployeeStatusEnum.REGULAR.getCode());

        entity = new ResignationApplication();
        entity.setId(1L);
        entity.setEmployeeId(10L);
        entity.setResignationType(ResignationTypeEnum.RESIGNATION.getCode());
        entity.setResignationReason("个人原因");
        entity.setResignationDate(LocalDate.now().plusDays(30));
        entity.setHandoverTo(20L);
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());

        // mock insert to set id
        doAnswer(inv -> {
            ResignationApplication e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        }).when(resignationMapper).insert(any());
    }

    // ═══════════════ 提交校验 ═══════════════

    @Nested
    @DisplayName("提交校验")
    class SubmitValidation {

        @Test
        @DisplayName("试用期和正式员工可发起离职")
        void shouldAllowProbationAndRegular() {
            employee.setStatus(EmployeeStatusEnum.PROBATION.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ResignationSaveDTO dto = buildDTO();
            resignationService.submit(dto);
            verify(resignationMapper).insert(any());
        }

        @Test
        @DisplayName("待入职状态不能发起离职")
        void shouldRejectPendingEntry() {
            employee.setStatus(EmployeeStatusEnum.PENDING_ENTRY.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ResignationSaveDTO dto = buildDTO();
            BaseException ex = assertThrows(BaseException.class,
                    () -> resignationService.submit(dto));
            assertTrue(ex.getMessage().contains("不可发起离职"));
        }

        @Test
        @DisplayName("已离职状态不能发起离职")
        void shouldRejectResigned() {
            employee.setStatus(EmployeeStatusEnum.RESIGNED.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ResignationSaveDTO dto = buildDTO();
            BaseException ex = assertThrows(BaseException.class,
                    () -> resignationService.submit(dto));
            assertTrue(ex.getMessage().contains("不可发起离职"));
        }

        @Test
        @DisplayName("离职日期不能早于今天")
        void shouldRejectPastResignDate() {
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ResignationSaveDTO dto = buildDTO();
            dto.setResignationDate(LocalDate.now().minusDays(1).toString());

            BaseException ex = assertThrows(BaseException.class,
                    () -> resignationService.submit(dto));
            assertTrue(ex.getMessage().contains("不能早于今天"));
        }

        @Test
        @DisplayName("交接人必填")
        void shouldRejectMissingHandover() {
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ResignationSaveDTO dto = buildDTO();
            dto.setHandoverTo(null);

            BaseException ex = assertThrows(BaseException.class,
                    () -> resignationService.submit(dto));
            assertTrue(ex.getMessage().contains("交接人"));
        }
    }

    // ═══════════════ 审批 ═══════════════

    @Nested
    @DisplayName("审批通过 → 待离职")
    class ApproveToPendingResign {

        @Test
        @DisplayName("审批通过后置待离职而非已离职")
        void shouldSetPendingResignNotResigned() {
            when(resignationMapper.selectById(1L)).thenReturn(entity);
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            ApprovalRecord record = buildRecord(1L, 1L);
            when(stateMachine.getApprovalRecords(anyInt(), eq(1L))).thenReturn(List.of(record));

            ApprovalStateMachineService.ApprovalResult approved = new ApprovalStateMachineService.ApprovalResult();
            approved.setTerminated(false);
            approved.setApproved(true);
            when(stateMachine.processApproval(eq(1L), eq(1), any(), eq(1L))).thenReturn(approved);

            ApprovalActionDTO action = new ApprovalActionDTO();
            action.setAction(1);

            resignationService.approve(1L, action);

            // 员工置「待离职(3)」而非「已离职(4)」
            assertEquals(EmployeeStatusEnum.PENDING_RESIGN.getCode(), employee.getStatus());
            assertNotEquals(EmployeeStatusEnum.RESIGNED.getCode(), employee.getStatus(),
                    "应在定时任务中转换，而非审批时直接置已离职");
            assertNotNull(employee.getResignDate());
            verify(employeeMapper).update(employee);

            // 申请单已通过
            assertEquals(ApprovalStatusEnum.APPROVED.getCode(), entity.getStatus());

            // 账号尚未禁用（定时任务执行时才禁用）
            verify(employeeAccountService, never()).disableAccount(anyLong());
        }

        @Test
        @DisplayName("审批拒绝后状态回退")
        void shouldSetRejectedOnTermination() {
            when(resignationMapper.selectById(1L)).thenReturn(entity);

            ApprovalRecord record = buildRecord(1L, 1L);
            when(stateMachine.getApprovalRecords(anyInt(), eq(1L))).thenReturn(List.of(record));

            ApprovalStateMachineService.ApprovalResult terminated = new ApprovalStateMachineService.ApprovalResult();
            terminated.setTerminated(true);
            terminated.setApproved(false);
            when(stateMachine.processApproval(eq(1L), eq(2), any(), eq(1L))).thenReturn(terminated);

            ApprovalActionDTO action = new ApprovalActionDTO();
            action.setAction(2);

            resignationService.approve(1L, action);

            assertEquals(ApprovalStatusEnum.REJECTED.getCode(), entity.getStatus());
            // 员工状态不变
            verify(employeeMapper, never()).update(any(Employee.class));
        }
    }

    // ═══════════════ 辅助方法 ═══════════════

    private ApprovalRecord buildRecord(Long id, Long approverId) {
        ApprovalRecord r = new ApprovalRecord();
        r.setId(id);
        r.setBusinessType(BusinessTypeEnum.RESIGNATION.getCode());
        r.setBusinessId(1L);
        r.setApproverId(approverId);
        r.setIsPending(1);
        return r;
    }

    private ResignationSaveDTO buildDTO() {
        ResignationSaveDTO dto = new ResignationSaveDTO();
        dto.setEmployeeId(10L);
        dto.setResignationType(ResignationTypeEnum.RESIGNATION.getCode());
        dto.setResignationReason("个人原因");
        dto.setResignationDate(LocalDate.now().plusDays(30).toString());
        dto.setHandoverTo(20L);
        return dto;
    }
}
