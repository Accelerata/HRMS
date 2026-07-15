package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.RegularizationSaveDTO;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RegularizationService 转正三分支测试
 * 验证：通过转正 / 延长试用 / 不通过辞退
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegularizationService 转正三分支")
class RegularizationServiceTest {

    @Mock private RegularizationApplicationMapper regularizationMapper;
    @Mock private ApprovalStateMachineService stateMachine;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeTransferMapper transferMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private RegularizationService regularizationService;

    private Employee employee;
    private RegularizationApplication entity;
    private ApprovalRecord record;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1L);

        employee = new Employee();
        employee.setId(10L);
        employee.setName("李四");
        employee.setDeptId(1L);
        employee.setStatus(EmployeeStatusEnum.PROBATION.getCode());
        employee.setEntryDate(LocalDate.of(2026, 4, 1));
        employee.setRegularDate(LocalDate.of(2026, 7, 1));
        employee.setUserId(50L);

        entity = new RegularizationApplication();
        entity.setId(1L);
        entity.setEmployeeId(10L);
        entity.setFormalSalary(new BigDecimal("12000.00"));
        entity.setProbationSummary("表现优秀");
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());

        record = new ApprovalRecord();
        record.setId(1L);
        record.setBusinessType(BusinessTypeEnum.REGULARIZATION.getCode());
        record.setBusinessId(1L);
        record.setStepOrder(1);
        record.setApproverId(1L);
        record.setIsPending(1);
    }

    // ═══════════════ 提交校验 ═══════════════

    @Nested
    @DisplayName("提交校验")
    class SubmitValidation {

        @Test
        @DisplayName("非试用期员工不能发起转正")
        void shouldRejectNonProbationEmployee() {
            employee.setStatus(EmployeeStatusEnum.REGULAR.getCode());
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            RegularizationSaveDTO dto = new RegularizationSaveDTO();
            dto.setEmployeeId(10L);
            dto.setProbationSummary("表现好");

            BaseException ex = assertThrows(BaseException.class,
                    () -> regularizationService.submit(dto));
            assertTrue(ex.getMessage().contains("试用期"));
        }

        @Test
        @DisplayName("试用期评价为空应拒绝")
        void shouldRejectEmptySummary() {
            when(employeeMapper.selectById(10L)).thenReturn(employee);

            RegularizationSaveDTO dto = new RegularizationSaveDTO();
            dto.setEmployeeId(10L);
            dto.setProbationSummary(""); // 空

            BaseException ex = assertThrows(BaseException.class,
                    () -> regularizationService.submit(dto));
            assertTrue(ex.getMessage().contains("评价"));
        }
    }

    // ═══════════════ 三分支审批 ═══════════════

    @Nested
    @DisplayName("三分支审批结果")
    class ThreeBranchApprove {

        @BeforeEach
        void setupApprove() {
            when(regularizationMapper.selectById(1L)).thenReturn(entity);
            when(stateMachine.getApprovalRecords(anyInt(), eq(1L))).thenReturn(List.of(record));

            ApprovalStateMachineService.ApprovalResult approved = new ApprovalStateMachineService.ApprovalResult();
            approved.setTerminated(false);
            approved.setApproved(true);
            when(stateMachine.processApproval(eq(1L), eq(1), any(), eq(1L)))
                    .thenReturn(approved);
            when(employeeMapper.selectById(10L)).thenReturn(employee);
        }

        @Test
        @DisplayName("通过转正：员工转正式 + 记录异动")
        void shouldPassRegularization() {
            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);  // 通过
            dto.setResultType(1); // 通过转正

            regularizationService.approve(1L, dto);

            // 员工状态
            assertEquals(EmployeeStatusEnum.REGULAR.getCode(), employee.getStatus());
            assertNotNull(employee.getRegularDate());
            verify(employeeMapper).update(employee);

            // 申请单状态
            assertEquals(ApprovalStatusEnum.APPROVED.getCode(), entity.getStatus());
            assertEquals(1, entity.getResultType());

            // 异动日志
            verify(transferMapper).insert(any(EmployeeTransfer.class));

            // 通知
            verify(notificationService).send(eq(50L), contains("转正"), anyString(), anyInt(), anyInt(), anyLong());
        }

        @Test
        @DisplayName("延长试用：更新 regularDate + 保持试用期状态")
        void shouldExtendProbation() {
            LocalDate oldRegularDate = employee.getRegularDate();

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            dto.setResultType(2);      // 延长试用
            dto.setExtendedMonths(2);  // 延长2个月

            regularizationService.approve(1L, dto);

            // 试用期延长
            assertEquals(oldRegularDate.plusMonths(2), employee.getRegularDate());
            // 保持试用期状态
            assertEquals(EmployeeStatusEnum.PROBATION.getCode(), employee.getStatus());

            // 记录延长月数
            assertEquals(2, entity.getExtendedMonths());
            assertEquals(ApprovalStatusEnum.APPROVED.getCode(), entity.getStatus());

            // 警告通知
            verify(notificationService).send(eq(50L), contains("延长"),
                    contains("2"), eq(3), anyInt(), anyLong());
        }

        @Test
        @DisplayName("不通过辞退：员工置待离职 + 记录异动")
        void shouldFailAndSetPendingResign() {
            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            dto.setResultType(3); // 不通过辞退

            regularizationService.approve(1L, dto);

            // 员工置待离职
            assertEquals(EmployeeStatusEnum.PENDING_RESIGN.getCode(), employee.getStatus());
            assertNotNull(employee.getResignDate());
            verify(employeeMapper).update(employee);

            // 记录结果
            assertEquals(3, entity.getResultType());

            // 通知
            verify(notificationService).sendWarning(eq(50L), contains("未通过"), anyString(), anyInt(), anyLong());
        }

        @Test
        @DisplayName("默认 resultType=1 走通过分支")
        void shouldDefaultToPass() {
            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            dto.setResultType(null); // 不指定

            regularizationService.approve(1L, dto);

            assertEquals(EmployeeStatusEnum.REGULAR.getCode(), employee.getStatus());
            assertEquals(1, entity.getResultType());
        }
    }
}
