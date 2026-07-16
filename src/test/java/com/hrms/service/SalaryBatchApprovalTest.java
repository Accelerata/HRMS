package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.entity.ApprovalRecord;
import com.hrms.entity.SalaryBatch;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.*;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SalaryBatchService 薪资批次审批测试
 * 验证：批次提交状态机、通过批量确认、拒绝整批退回
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SalaryBatchService 薪资批次审批")
class SalaryBatchApprovalTest {

    @Mock private SalaryCalculationService calculationService;
    @Mock private SalaryAccountMapper salaryAccountMapper;
    @Mock private SalaryRecordMapper salaryRecordMapper;
    @Mock private SalaryBatchMapper salaryBatchMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private AttendanceRecordMapper attendanceRecordMapper;
    @Mock private LeaveApplicationMapper leaveApplicationMapper;
    @Mock private OvertimeRecordMapper overtimeRecordMapper;
    @Mock private SocialInsuranceConfigMapper siConfigMapper;
    @Mock private ApprovalStateMachineService stateMachine;

    @InjectMocks
    private SalaryBatchService batchService;

    private SalaryBatch draftBatch;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1000L); // HR

        draftBatch = new SalaryBatch();
        draftBatch.setId(1L);
        draftBatch.setYear(2026);
        draftBatch.setMonth(6);
        draftBatch.setStatus("DRAFT");
        draftBatch.setEmployeeCount(50);
        draftBatch.setTotalNetPay(new BigDecimal("500000.00"));
        draftBatch.setSubmitterId(1000L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    @Nested
    @DisplayName("批次提交审批")
    class Submit {

        @Test
        @DisplayName("DRAFT 批次提交后 PENDING 并生成财务审批待办")
        void submit_draft_success() {
            when(salaryBatchMapper.selectById(1L)).thenReturn(draftBatch);
            when(salaryRecordMapper.countByBatch(1L)).thenReturn(50);

            batchService.submitBatch(1L);

            verify(salaryBatchMapper).updateStatus(1L, "PENDING");
            verify(salaryBatchMapper).updateSubmitter(1L, 1000L);
            verify(stateMachine).startApproval(eq(BusinessTypeEnum.SALARY.getCode()), eq(1L),
                    argThat(ctx -> Long.valueOf(1000L).equals(ctx.getSubmitterUserId())));
        }

        @Test
        @DisplayName("PENDING 批次重复提交被拒绝")
        void submit_pending_throws() {
            draftBatch.setStatus("PENDING");
            when(salaryBatchMapper.selectById(1L)).thenReturn(draftBatch);

            assertThrows(BaseException.class, () -> batchService.submitBatch(1L));
            verify(stateMachine, never()).startApproval(anyInt(), anyLong(), any());
        }

        @Test
        @DisplayName("空批次提交被拒绝")
        void submit_empty_throws() {
            when(salaryBatchMapper.selectById(1L)).thenReturn(draftBatch);
            when(salaryRecordMapper.countByBatch(1L)).thenReturn(0);

            assertThrows(BaseException.class, () -> batchService.submitBatch(1L));
        }
    }

    @Nested
    @DisplayName("批次审批")
    class Approve {

        private ApprovalRecord financePendingRecord() {
            ApprovalRecord r = new ApprovalRecord();
            r.setId(50L);
            r.setBusinessType(BusinessTypeEnum.SALARY.getCode());
            r.setBusinessId(1L);
            r.setApproverId(4000L); // 财务
            r.setIsPending(1);
            return r;
        }

        @BeforeEach
        void pendingBatch() {
            draftBatch.setStatus("PENDING");
            when(salaryBatchMapper.selectById(1L)).thenReturn(draftBatch);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.SALARY.getCode(), 1L))
                    .thenReturn(List.of(financePendingRecord()));
            BaseContext.setCurrentUserId(4000L); // 财务专员
        }

        @Test
        @DisplayName("财务通过：批次 APPROVED + 记录批量 CONFIRMED")
        void approve_pass_batchConfirmed() {
            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setApproved(true);
            when(stateMachine.processApproval(eq(50L), eq(1), any(), eq(4000L))).thenReturn(result);
            when(salaryRecordMapper.updateStatusByBatch(1L, "DRAFT", "CONFIRMED")).thenReturn(50);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            batchService.approveBatch(1L, dto);

            verify(salaryBatchMapper).updateStatus(1L, "APPROVED");
            verify(salaryRecordMapper).updateStatusByBatch(1L, "DRAFT", "CONFIRMED");
        }

        @Test
        @DisplayName("财务拒绝：批次 REJECTED + 记录回退 DRAFT")
        void approve_reject_batchReverted() {
            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setTerminated(true);
            when(stateMachine.processApproval(eq(50L), eq(2), any(), eq(4000L))).thenReturn(result);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(2);
            dto.setComment("数据有误");
            batchService.approveBatch(1L, dto);

            verify(salaryBatchMapper).updateStatus(1L, "REJECTED");
            verify(salaryRecordMapper).updateStatusByBatch(1L, "CONFIRMED", "DRAFT");
        }

        @Test
        @DisplayName("非 PENDING 批次不可审批")
        void approve_notPending_throws() {
            draftBatch.setStatus("DRAFT");
            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            assertThrows(BaseException.class, () -> batchService.approveBatch(1L, dto));
        }
    }

    @Nested
    @DisplayName("批量核算批次复用")
    class BatchReuse {

        @Test
        @DisplayName("PENDING 批次拒绝重复核算")
        void recalculate_pendingBatch_throws() {
            draftBatch.setStatus("PENDING");
            when(salaryBatchMapper.selectByYearMonth(2026, 6)).thenReturn(draftBatch);

            assertThrows(BaseException.class, () -> batchService.batchCalculate(2026, 6));
        }

        @Test
        @DisplayName("APPROVED 批次拒绝重复核算")
        void recalculate_approvedBatch_throws() {
            draftBatch.setStatus("APPROVED");
            when(salaryBatchMapper.selectByYearMonth(2026, 6)).thenReturn(draftBatch);

            assertThrows(BaseException.class, () -> batchService.batchCalculate(2026, 6));
        }

        @Test
        @DisplayName("DRAFT 批次复用并清理旧记录")
        void recalculate_draftBatch_reuses() {
            when(salaryBatchMapper.selectByYearMonth(2026, 6)).thenReturn(draftBatch);
            when(salaryAccountMapper.selectAllActive()).thenReturn(List.of());
            when(salaryRecordMapper.selectByBatch(1L)).thenReturn(List.of());

            batchService.batchCalculate(2026, 6);

            verify(salaryRecordMapper).deleteByBatch(1L);
            verify(salaryBatchMapper, never()).insert(any());
        }
    }
}
