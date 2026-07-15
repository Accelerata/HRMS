package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.crypto.EncryptionUtil;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.OnboardingSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.EmployeeStatusEnum;
import com.hrms.mapper.*;
import com.hrms.utils.EmployeeNoGenerator;
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
 * OnboardingService 入职全链路测试
 * 验证：提交→审批→待入职→确认到岗→已入职 完整流程
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OnboardingService 入职全链路")
class OnboardingServiceTest {

    @Mock private OnboardingApplicationMapper onboardingMapper;
    @Mock private ApprovalStateMachineService stateMachine;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeTransferMapper transferMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private GradeSalaryRangeMapper gradeSalaryRangeMapper;
    @Mock private EncryptionUtil encryptionUtil;
    @Mock private EmployeeNoGenerator employeeNoGenerator;
    @Mock private EmployeeAccountService employeeAccountService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private OnboardingService onboardingService;

    private OnboardingSaveDTO dto;
    private OnboardingApplication entity;
    private Department dept;
    private Position position;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(1L); // HR用户

        dto = new OnboardingSaveDTO();
        dto.setRealName("张三");
        dto.setPhone("13800138001");
        dto.setEmail("zhangsan@test.com");
        dto.setIdCard("440101199508080012");
        dto.setTargetDeptId(10L);
        dto.setTargetPositionId(20L);
        dto.setOfferSalary(new BigDecimal("15000.00"));
        dto.setProbationMonths(3);
        dto.setEntryDate("2026-08-01");
        dto.setGender(1);
        dto.setGrade("P3");
        dto.setEmploymentType(1);

        entity = new OnboardingApplication();
        entity.setId(1L);
        entity.setRealName("张三");
        entity.setPhone("13800138001");
        entity.setTargetDeptId(10L);
        entity.setTargetPositionId(20L);
        entity.setOfferSalary(new BigDecimal("15000.00"));
        entity.setProbationMonths(3);
        entity.setEntryDate(LocalDate.of(2026, 8, 1));
        entity.setGrade("P3");
        entity.setEmploymentType(1);
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());

        dept = new Department();
        dept.setId(10L);
        dept.setDeptName("技术部");
        dept.setDeptCode("TECH");
        dept.setManagerId(5L);

        position = new Position();
        position.setId(20L);
        position.setPositionName("高级工程师");
        position.setIsStandard(1);

        // 通用 mocking
        when(encryptionUtil.computeHash(anyString())).thenReturn("mock_hash");
        when(employeeNoGenerator.generate(10L)).thenReturn("2026TECH001");
        when(departmentMapper.selectById(10L)).thenReturn(dept);
        when(positionMapper.selectById(20L)).thenReturn(position);
        when(gradeSalaryRangeMapper.selectByGradeCode("P3")).thenReturn(null); // 未配置→默认在范围内
    }

    // ═══════════════ 提交 ═══════════════

    @Nested
    @DisplayName("提交入职申请")
    class Submit {

        @Test
        @DisplayName("提交成功：创建申请单 + 启动审批")
        void shouldSubmitAndStartApproval() {
            doAnswer(inv -> {
                OnboardingApplication e = inv.getArgument(0);
                e.setId(1L);
                return 1;
            }).when(onboardingMapper).insert(any());

            onboardingService.submit(dto);

            ArgumentCaptor<OnboardingApplication> captor = ArgumentCaptor.forClass(OnboardingApplication.class);
            verify(onboardingMapper).insert(captor.capture());
            OnboardingApplication saved = captor.getValue();
            assertEquals("张三", saved.getRealName());
            assertEquals(ApprovalStatusEnum.PENDING.getCode(), saved.getStatus());
            assertEquals("P3", saved.getGrade());

            verify(stateMachine).startApproval(
                    eq(BusinessTypeEnum.ONBOARDING.getCode()), eq(1L),
                    any(ApprovalStateMachineService.ApprovalContext.class));
        }
    }

    // ═══════════════ 审批 ═══════════════

    @Nested
    @DisplayName("审批入职申请")
    class Approve {

        @Test
        @DisplayName("审批拒绝：申请单置 REJECTED")
        void shouldSetRejectedOnTermination() {
            entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
            when(onboardingMapper.selectById(1L)).thenReturn(entity);

            ApprovalRecord record = buildRecord(1L, 1L);
            when(stateMachine.getApprovalRecords(anyInt(), eq(1L))).thenReturn(List.of(record));

            ApprovalStateMachineService.ApprovalResult terminated = new ApprovalStateMachineService.ApprovalResult();
            terminated.setTerminated(true);
            terminated.setApproved(false);
            when(stateMachine.processApproval(eq(1L), eq(2), anyString(), eq(1L)))
                    .thenReturn(terminated);

            ApprovalActionDTO action = new ApprovalActionDTO();
            action.setAction(2); // 拒绝
            action.setComment("不合适");

            var result = onboardingService.approve(1L, action);

            assertNull(result, "拒绝后应返回null");
            assertEquals(ApprovalStatusEnum.REJECTED.getCode(), entity.getStatus());
            verify(onboardingMapper).update(entity);
        }

        @Test
        @DisplayName("审批全部通过：转为待入职 + 生成工号账号")
        void shouldExecuteOnboardingWhenAllApproved() {
            entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
            when(onboardingMapper.selectById(1L)).thenReturn(entity);

            ApprovalRecord record = buildRecord(1L, 1L);
            when(stateMachine.getApprovalRecords(anyInt(), eq(1L))).thenReturn(List.of(record));

            ApprovalStateMachineService.ApprovalResult approved = new ApprovalStateMachineService.ApprovalResult();
            approved.setTerminated(false);
            approved.setApproved(true);
            when(stateMachine.processApproval(eq(1L), eq(1), any(), eq(1L)))
                    .thenReturn(approved);

            // mock employee insert
            doAnswer(inv -> {
                Employee e = inv.getArgument(0);
                e.setId(100L);
                return 1;
            }).when(employeeMapper).insert(any(Employee.class));

            when(employeeAccountService.createAccount(eq(100L), eq("13800138001"), eq("ROLE_EMPLOYEE")))
                    .thenReturn("Abc12345!");

            ApprovalActionDTO action = new ApprovalActionDTO();
            action.setAction(1); // 通过

            var result = onboardingService.approve(1L, action);

            // 验证结果
            assertNotNull(result);
            assertEquals("2026TECH001", result.getEmployeeNo());
            assertEquals("13800138001", result.getUsername());
            assertEquals("Abc12345!", result.getInitialPassword());

            // 验证状态
            assertEquals(ApprovalStatusEnum.PENDING_ENTRY.getCode(), entity.getStatus());
            assertEquals(100L, entity.getEmployeeId());

            // 验证员工创建
            ArgumentCaptor<Employee> empCaptor = ArgumentCaptor.forClass(Employee.class);
            verify(employeeMapper).insert(empCaptor.capture());
            Employee created = empCaptor.getValue();
            assertEquals("2026TECH001", created.getEmployeeNo());
            assertEquals(EmployeeStatusEnum.PENDING_ENTRY.getCode(), created.getStatus());
            assertEquals("张三", created.getName());

            // 验证账号创建
            verify(employeeAccountService).createAccount(100L, "13800138001", "ROLE_EMPLOYEE");

            // 验证异动日志
            verify(transferMapper).insert(any(EmployeeTransfer.class));
        }
    }

    // ═══════════════ 确认到岗 ═══════════════

    @Nested
    @DisplayName("确认到岗")
    class ConfirmArrival {

        @Test
        @DisplayName("确认到岗：员工转试用期 + 申请转已入职")
        void shouldConfirmArrivalSuccessfully() {
            entity.setStatus(ApprovalStatusEnum.PENDING_ENTRY.getCode());
            entity.setEmployeeId(100L);
            when(onboardingMapper.selectById(1L)).thenReturn(entity);

            Employee emp = new Employee();
            emp.setId(100L);
            emp.setName("张三");
            emp.setStatus(EmployeeStatusEnum.PENDING_ENTRY.getCode());
            emp.setUserId(200L);
            when(employeeMapper.selectById(100L)).thenReturn(emp);

            onboardingService.confirmArrival(1L);

            // 员工状态转试用期
            assertEquals(EmployeeStatusEnum.PROBATION.getCode(), emp.getStatus());
            verify(employeeMapper).update(emp);

            // 申请单转已入职
            assertEquals(ApprovalStatusEnum.ONBOARDED.getCode(), entity.getStatus());
            verify(onboardingMapper).update(entity);

            // 异动日志
            verify(transferMapper).insert(any(EmployeeTransfer.class));

            // 欢迎通知
            verify(notificationService).send(eq(200L), eq("欢迎入职"),
                    contains("欢迎加入"), anyInt(), anyInt(), anyLong());
        }

        @Test
        @DisplayName("非待入职状态确认到岗应拒绝")
        void shouldRejectWhenNotPendingEntry() {
            entity.setStatus(ApprovalStatusEnum.PENDING.getCode()); // 审批中，非待入职
            when(onboardingMapper.selectById(1L)).thenReturn(entity);

            var ex = assertThrows(com.hrms.common.exception.BaseException.class,
                    () -> onboardingService.confirmArrival(1L));
            assertTrue(ex.getMessage().contains("待入职"));
        }
    }

    // ═══════════════ 辅助方法 ═══════════════

    private ApprovalRecord buildRecord(Long id, Long approverId) {
        ApprovalRecord r = new ApprovalRecord();
        r.setId(id);
        r.setBusinessType(BusinessTypeEnum.ONBOARDING.getCode());
        r.setBusinessId(1L);
        r.setStepOrder(1);
        r.setApproverId(approverId);
        r.setIsPending(1);
        return r;
    }

    private void mockInsertWithId() {
        doAnswer(inv -> {
            OnboardingApplication e = inv.getArgument(0);
            e.setId(1L);
            return 1;
        }).when(onboardingMapper).insert(any());
    }
}
