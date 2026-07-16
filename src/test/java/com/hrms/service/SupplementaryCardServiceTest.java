package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.SupplementaryCardApplyDTO;
import com.hrms.entity.ApprovalRecord;
import com.hrms.entity.AttendanceRecord;
import com.hrms.entity.Employee;
import com.hrms.entity.SupplementaryCardApplication;
import com.hrms.enums.AttendanceStatus;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.AttendanceRecordMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.SupplementaryCardMapper;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SupplementaryCardService 补卡审批测试
 * 验证：缺卡校验、防重复申请、审批通过回写考勤
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SupplementaryCardService 补卡审批")
class SupplementaryCardServiceTest {

    @Mock private SupplementaryCardMapper cardMapper;
    @Mock private AttendanceRecordMapper attendanceRecordMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private ApprovalStateMachineService stateMachine;

    @InjectMocks
    private SupplementaryCardService cardService;

    private Employee employee;
    private AttendanceRecord missingPunchRecord;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1000L);
        BaseContext.setCurrentEmployeeId(100L);

        employee = new Employee();
        employee.setId(100L);
        employee.setDeptId(10L);
        employee.setUserId(1000L);
        when(employeeMapper.selectById(100L)).thenReturn(employee);

        missingPunchRecord = new AttendanceRecord();
        missingPunchRecord.setId(500L);
        missingPunchRecord.setEmployeeId(100L);
        missingPunchRecord.setAttendanceDate(LocalDate.of(2026, 7, 10));
        missingPunchRecord.setPunchInStatus(AttendanceStatus.MISSING_PUNCH.name());
        missingPunchRecord.setPunchOutStatus(AttendanceStatus.NORMAL.name());
        when(attendanceRecordMapper.selectByEmployeeAndDate(100L, LocalDate.of(2026, 7, 10)))
                .thenReturn(missingPunchRecord);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    private SupplementaryCardApplyDTO applyDTO(int cardType) {
        SupplementaryCardApplyDTO dto = new SupplementaryCardApplyDTO();
        dto.setAttendanceDate(LocalDate.of(2026, 7, 10));
        dto.setCardType(cardType);
        dto.setSupplementTime(LocalTime.of(9, 0));
        dto.setReason("忘记打卡");
        return dto;
    }

    @Nested
    @DisplayName("补卡申请发起")
    class Apply {

        @Test
        @DisplayName("缺卡日期正常发起并生成审批待办")
        void apply_success() {
            when(cardMapper.countActiveByEmployeeDateType(100L, LocalDate.of(2026, 7, 10), 1))
                    .thenReturn(0);
            // 模拟 MyBatis useGeneratedKeys 回填主键
            doAnswer(invocation -> {
                invocation.<SupplementaryCardApplication>getArgument(0).setId(1L);
                return 1;
            }).when(cardMapper).insert(any(SupplementaryCardApplication.class));

            cardService.apply(applyDTO(1));

            verify(cardMapper).insert(any(SupplementaryCardApplication.class));
            verify(stateMachine).startApproval(eq(BusinessTypeEnum.CARD.getCode()), eq(1L), any());
        }

        @Test
        @DisplayName("非缺卡卡型拒绝申请")
        void apply_notMissingPunch_throws() {
            // 下班卡状态为 NORMAL
            assertThrows(BaseException.class, () -> cardService.apply(applyDTO(2)));
            verify(cardMapper, never()).insert(any());
        }

        @Test
        @DisplayName("无考勤记录拒绝申请")
        void apply_noRecord_throws() {
            when(attendanceRecordMapper.selectByEmployeeAndDate(100L, LocalDate.of(2026, 7, 10)))
                    .thenReturn(null);
            assertThrows(BaseException.class, () -> cardService.apply(applyDTO(1)));
        }

        @Test
        @DisplayName("重复申请被拒绝")
        void apply_duplicate_throws() {
            when(cardMapper.countActiveByEmployeeDateType(100L, LocalDate.of(2026, 7, 10), 1))
                    .thenReturn(1);
            assertThrows(BaseException.class, () -> cardService.apply(applyDTO(1)));
            verify(cardMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("补卡审批")
    class Approve {

        private SupplementaryCardApplication pendingApplication() {
            SupplementaryCardApplication a = new SupplementaryCardApplication();
            a.setId(1L);
            a.setEmployeeId(100L);
            a.setAttendanceDate(LocalDate.of(2026, 7, 10));
            a.setCardType(1);
            a.setSupplementTime(LocalTime.of(9, 0));
            a.setStatus(1);
            return a;
        }

        @Test
        @DisplayName("审批通过回写考勤记录")
        void approve_pass_writesBack() {
            when(cardMapper.selectById(1L)).thenReturn(pendingApplication());

            ApprovalRecord myRecord = new ApprovalRecord();
            myRecord.setId(50L);
            myRecord.setApproverId(2000L);
            myRecord.setIsPending(1);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.CARD.getCode(), 1L))
                    .thenReturn(List.of(myRecord));
            BaseContext.setCurrentUserId(2000L);

            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setApproved(true);
            when(stateMachine.processApproval(eq(50L), eq(1), any(), eq(2000L))).thenReturn(result);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(1);
            cardService.approve(1L, dto);

            verify(cardMapper).updateStatus(1L, 2);
            verify(attendanceRecordMapper).updatePunchIn(500L, LocalTime.of(9, 0), AttendanceStatus.NORMAL.name());
        }

        @Test
        @DisplayName("审批拒绝不回写考勤")
        void approve_reject_noWriteBack() {
            when(cardMapper.selectById(1L)).thenReturn(pendingApplication());

            ApprovalRecord myRecord = new ApprovalRecord();
            myRecord.setId(50L);
            myRecord.setApproverId(2000L);
            myRecord.setIsPending(1);
            when(stateMachine.getApprovalRecords(BusinessTypeEnum.CARD.getCode(), 1L))
                    .thenReturn(List.of(myRecord));
            BaseContext.setCurrentUserId(2000L);

            ApprovalStateMachineService.ApprovalResult result = new ApprovalStateMachineService.ApprovalResult();
            result.setTerminated(true);
            when(stateMachine.processApproval(eq(50L), eq(2), any(), eq(2000L))).thenReturn(result);

            ApprovalActionDTO dto = new ApprovalActionDTO();
            dto.setAction(2);
            cardService.approve(1L, dto);

            verify(cardMapper).updateStatus(1L, 3);
            verify(attendanceRecordMapper, never()).updatePunchIn(anyLong(), any(), anyString());
        }
    }
}
