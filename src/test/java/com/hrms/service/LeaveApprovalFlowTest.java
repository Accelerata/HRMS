package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.LeaveApplyDTO;
import com.hrms.entity.*;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.LeaveType;
import com.hrms.mapper.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * LeaveService 请假审批流测试
 * 验证：6.3.4 分级规则矩阵、余额提交占用、拒绝/取消回补、HR备案
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaveService 请假审批流")
class LeaveApprovalFlowTest {

    @Mock private LeaveBalanceMapper leaveBalanceMapper;
    @Mock private LeaveApplicationMapper leaveApplicationMapper;
    @Mock private OvertimeRecordMapper overtimeRecordMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private ApprovalRecordMapper approvalRecordMapper;
    @Mock private ApprovalStateMachineService stateMachine;
    @Mock private NotificationService notificationService;
    @Mock private LeaveDayCalculator leaveDayCalculator;
    @Mock private LeaveAttachmentMapper leaveAttachmentMapper;
    @Mock private CompLeaveGrantMapper compLeaveGrantMapper;
    @Mock private CompLeaveUsageMapper compLeaveUsageMapper;

    @InjectMocks
    private LeaveService leaveService;

    private Employee employee;
    private LeaveBalance annualBalance;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1000L);
        BaseContext.setCurrentEmployeeId(100L);

        employee = new Employee();
        employee.setId(100L);
        employee.setName("张三");
        employee.setDeptId(10L);
        employee.setUserId(1000L);
        when(employeeMapper.selectById(100L)).thenReturn(employee);

        annualBalance = new LeaveBalance();
        annualBalance.setEmployeeId(100L);
        annualBalance.setLeaveType(LeaveType.ANNUAL.getCode());
        annualBalance.setTotalDays(new BigDecimal("5"));
        annualBalance.setUsedDays(BigDecimal.ZERO);
        annualBalance.setRemainingDays(new BigDecimal("5"));
        annualBalance.setYear(2026);
        when(leaveBalanceMapper.selectByEmployeeTypeAndYear(eq(100L), eq(LeaveType.ANNUAL.getCode()), anyInt()))
                .thenReturn(annualBalance);

        LeaveBalance compBalance = new LeaveBalance();
        compBalance.setEmployeeId(100L);
        compBalance.setLeaveType(LeaveType.COMPENSATORY.getCode());
        compBalance.setTotalDays(new BigDecimal("10"));
        compBalance.setUsedDays(BigDecimal.ZERO);
        compBalance.setRemainingDays(new BigDecimal("10"));
        compBalance.setYear(2026);
        when(leaveBalanceMapper.selectByEmployeeTypeAndYear(eq(100L), eq(LeaveType.COMPENSATORY.getCode()), anyInt()))
                .thenReturn(compBalance);

        // 新增依赖 mock
        when(leaveDayCalculator.calculate(any(), anyInt(), any(), anyInt())).thenReturn(new BigDecimal("2"));
        // 附件校验 mock：返回非空列表以通过病假/婚假/产假的条件必填校验
        LeaveAttachment dummyAttachment = new LeaveAttachment();
        dummyAttachment.setId(1L);
        dummyAttachment.setApplicationId(1L);
        dummyAttachment.setFileName("test.pdf");
        dummyAttachment.setUploadBy(100L);
        when(leaveAttachmentMapper.selectByApplicationId(anyLong())).thenReturn(List.of(dummyAttachment));
        when(leaveApplicationMapper.insert(any())).thenReturn(1);
        // 调休 FIFO mock
        CompLeaveGrant grant = new CompLeaveGrant();
        grant.setId(1L);
        grant.setEmployeeId(100L);
        grant.setDays(new BigDecimal("10"));
        grant.setUsedDays(BigDecimal.ZERO);
        grant.setExpireDate(LocalDate.of(2026, 12, 31));
        grant.setStatus(1);
        when(compLeaveGrantMapper.selectValidByEmployee(eq(100L))).thenReturn(List.of(grant));
        when(compLeaveUsageMapper.insert(any())).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    private LeaveApplication draftOf(int leaveType, String days) {
        LeaveApplication a = new LeaveApplication();
        a.setId(1L);
        a.setEmployeeId(100L);
        a.setLeaveType(leaveType);
        a.setStartDate(LocalDate.of(2026, 8, 3));
        a.setEndDate(LocalDate.of(2026, 8, 5));
        a.setDays(new BigDecimal(days));
        a.setReason("测试");
        a.setStatus(0);
        return a;
    }

    // ═══════════════ 6.3.4 分级规则矩阵 ═══════════════

    @Nested
    @DisplayName("分级审批规则矩阵")
    class ApprovalLevelMatrix {

        @ParameterizedTest(name = "leaveType={0}, days={1} → 需部门负责人二审={2}")
        @CsvSource({
                "1, 2,   false",  // 年假 ≤3天
                "1, 3,   false",  // 年假 =3天（边界）
                "1, 3.5, true",   // 年假 >3天
                "2, 3,   false",  // 调休 ≤3天
                "2, 5,   true",   // 调休 >3天
                "3, 1,   false",  // 事假 ≤1天（边界）
                "3, 1.5, true",   // 事假 >1天
                "4, 0.5, false",  // 病假 ≤1天
                "4, 2,   true",   // 病假 >1天
                "5, 3,   false",  // 婚假恒一级
                "6, 98,  false",  // 产假恒一级
                "7, 3,   false"   // 丧假恒一级
        })
        void submit_generatesCorrectApprovalLevels(int leaveType, String days, boolean expectDeptManager) {
            LeaveApplication draft = draftOf(leaveType, days);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(draft);

            leaveService.submit(1L);

            ArgumentCaptor<ApprovalStateMachineService.ApprovalContext> captor =
                    ArgumentCaptor.forClass(ApprovalStateMachineService.ApprovalContext.class);
            verify(stateMachine).startApproval(eq(BusinessTypeEnum.LEAVE.getCode()), eq(1L), captor.capture());
            assertEquals(expectDeptManager, captor.getValue().isLeaveNeedDeptManager(),
                    "leaveType=" + leaveType + ", days=" + days);
            assertEquals(100L, captor.getValue().getEmployeeId());
            assertEquals(1000L, captor.getValue().getSubmitterUserId());
        }
    }

    // ═══════════════ 提交占用余额 ═══════════════

    @Nested
    @DisplayName("提交与余额占用")
    class SubmitOccupation {

        @Test
        @DisplayName("草稿创建不扣减余额")
        void apply_doesNotDeductBalance() {
            LeaveApplyDTO dto = new LeaveApplyDTO();
            dto.setLeaveType(1);
            dto.setStartDate(LocalDate.of(2026, 8, 3));
            dto.setEndDate(LocalDate.of(2026, 8, 4));
            dto.setDays(new BigDecimal("2"));
            dto.setReason("年假");

            leaveService.apply(dto);

            verify(leaveBalanceMapper, never()).update(any());
            verify(leaveApplicationMapper).insert(any(LeaveApplication.class));
        }

        @Test
        @DisplayName("提交时占用余额")
        void submit_occupiesBalance() {
            when(leaveApplicationMapper.selectById(1L)).thenReturn(draftOf(1, "2"));

            leaveService.submit(1L);

            ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
            verify(leaveBalanceMapper).update(captor.capture());
            assertEquals(new BigDecimal("2"), captor.getValue().getUsedDays());
            assertEquals(new BigDecimal("3"), captor.getValue().getRemainingDays());
            verify(leaveApplicationMapper).updateStatus(1L, 1);
        }

        @Test
        @DisplayName("余额不足提交失败")
        void submit_insufficientBalance_throws() {
            annualBalance.setRemainingDays(new BigDecimal("1"));
            when(leaveApplicationMapper.selectById(1L)).thenReturn(draftOf(1, "3"));

            assertThrows(BaseException.class, () -> leaveService.submit(1L));
            verify(stateMachine, never()).startApproval(anyInt(), anyLong(), any());
        }

        @Test
        @DisplayName("他人申请不可提交")
        void submit_notOwner_forbidden() {
            LeaveApplication draft = draftOf(1, "2");
            draft.setEmployeeId(999L);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(draft);

            assertThrows(BaseException.class, () -> leaveService.submit(1L));
        }
    }

    // ═══════════════ 审批通过与拒绝 ═══════════════

    @Nested
    @DisplayName("审批通过/拒绝")
    class Approval {

        private ApprovalRecord myPendingRecord() {
            ApprovalRecord r = new ApprovalRecord();
            r.setId(50L);
            r.setBusinessType(BusinessTypeEnum.LEAVE.getCode());
            r.setBusinessId(1L);
            r.setApproverId(1000L);
            r.setIsPending(1);
            return r;
        }

        @Test
        @DisplayName("审批全部通过置已通过")
        void approve_allPass_setsApproved() {
            LeaveApplication pending = draftOf(1, "2");
            pending.setStatus(1);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(pending);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), 1L))
                    .thenReturn(List.of(myPendingRecord()));

            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setApproved(true);
            when(stateMachine.processApproval(eq(50L), eq(1), any(), eq(1000L))).thenReturn(result);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            leaveService.approve(1L, dto);

            verify(leaveApplicationMapper).updateStatus(1L, 2);
            verify(leaveBalanceMapper, never()).update(any()); // 通过不回补
        }

        @Test
        @DisplayName("审批拒绝回补余额")
        void approve_rejected_restoresBalance() {
            LeaveApplication pending = draftOf(1, "2");
            pending.setStatus(1);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(pending);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), 1L))
                    .thenReturn(List.of(myPendingRecord()));

            // 模拟提交时已占用 2 天
            annualBalance.setUsedDays(new BigDecimal("2"));
            annualBalance.setRemainingDays(new BigDecimal("3"));

            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setTerminated(true);
            when(stateMachine.processApproval(eq(50L), eq(2), any(), eq(1000L))).thenReturn(result);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(2);
            dto.setComment("不同意");
            leaveService.approve(1L, dto);

            verify(leaveApplicationMapper).updateStatus(1L, 3);
            ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
            verify(leaveBalanceMapper).update(captor.capture());
            assertEquals(BigDecimal.ZERO, captor.getValue().getUsedDays());
            assertEquals(new BigDecimal("5"), captor.getValue().getRemainingDays());
        }

        @Test
        @DisplayName("婚假通过后向HR备案")
        void approve_marriage_notifiesHr() {
            LeaveApplication pending = draftOf(5, "3");
            pending.setStatus(1);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(pending);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), 1L))
                    .thenReturn(List.of(myPendingRecord()));

            SysUser hr = new SysUser();
            hr.setId(2000L);
            when(sysUserMapper.findFirstByRoleCode("ROLE_HR")).thenReturn(hr);

            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setApproved(true);
            when(stateMachine.processApproval(eq(50L), eq(1), any(), eq(1000L))).thenReturn(result);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            leaveService.approve(1L, dto);

            verify(notificationService).sendApprovalNotify(eq(2000L), contains("备案"), anyString(),
                    eq(BusinessTypeEnum.LEAVE.getCode()), eq(1L));
        }
    }

    // ═══════════════ 取消申请 ═══════════════

    @Nested
    @DisplayName("取消申请")
    class Cancel {

        @Test
        @DisplayName("审批中取消回补余额并关闭待办")
        void cancel_pending_restoresAndCloses() {
            LeaveApplication pending = draftOf(1, "2");
            pending.setStatus(1);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(pending);

            ApprovalRecord unprocessed = new ApprovalRecord();
            unprocessed.setIsPending(1);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), 1L))
                    .thenReturn(List.of(unprocessed));

            annualBalance.setUsedDays(new BigDecimal("2"));
            annualBalance.setRemainingDays(new BigDecimal("3"));

            leaveService.cancel(1L);

            verify(leaveApplicationMapper).updateStatus(1L, 4);
            verify(leaveBalanceMapper).update(any(LeaveBalance.class));
            verify(approvalRecordMapper).closePendingByBusiness(BusinessTypeEnum.LEAVE.getCode(), 1L);
        }

        @Test
        @DisplayName("已有审批动作不可取消")
        void cancel_processed_throws() {
            LeaveApplication pending = draftOf(1, "2");
            pending.setStatus(1);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(pending);

            ApprovalRecord processed = new ApprovalRecord();
            processed.setIsPending(0);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.LEAVE.getCode(), 1L))
                    .thenReturn(List.of(processed));

            assertThrows(BaseException.class, () -> leaveService.cancel(1L));
        }

        @Test
        @DisplayName("他人申请不可取消")
        void cancel_notOwner_forbidden() {
            LeaveApplication pending = draftOf(1, "2");
            pending.setStatus(1);
            pending.setEmployeeId(999L);
            when(leaveApplicationMapper.selectById(1L)).thenReturn(pending);

            assertThrows(BaseException.class, () -> leaveService.cancel(1L));
        }
    }
}
