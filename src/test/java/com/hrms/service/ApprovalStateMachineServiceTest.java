package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalActionEnum;
import com.hrms.enums.BusinessTypeEnum;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalStateMachineService 审批引擎测试
 * 验证：顺序门控、条件步骤生成/跳过、审批超时
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApprovalStateMachineService 审批引擎")
class ApprovalStateMachineServiceTest {

    @Mock private ApprovalTemplateMapper templateMapper;
    @Mock private ApprovalRecordMapper recordMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ApprovalStateMachineService stateMachine;

    private Department dept;
    private Employee manager;
    private SysUser hrUser;
    private SysUser financeUser;
    private SysUser managerUser;

    @BeforeEach
    void setUp() {
        dept = new Department();
        dept.setId(1L);
        dept.setManagerId(100L);

        Department newDept = new Department();
        newDept.setId(2L);
        newDept.setManagerId(101L);

        manager = new Employee();
        manager.setId(100L);
        manager.setUserId(200L);

        Employee newDeptManager = new Employee();
        newDeptManager.setId(101L);
        newDeptManager.setUserId(201L);

        managerUser = new SysUser();
        managerUser.setId(200L);
        managerUser.setUsername("manager");

        SysUser newDeptManagerUser = new SysUser();
        newDeptManagerUser.setId(201L);
        newDeptManagerUser.setUsername("new_manager");

        hrUser = new SysUser();
        hrUser.setId(300L);
        hrUser.setUsername("hr_specialist");

        financeUser = new SysUser();
        financeUser.setId(400L);
        financeUser.setUsername("finance");

        when(departmentMapper.selectById(1L)).thenReturn(dept);
        when(departmentMapper.selectById(2L)).thenReturn(newDept);
        when(employeeMapper.selectById(100L)).thenReturn(manager);
        when(employeeMapper.selectById(101L)).thenReturn(newDeptManager);
        when(sysUserMapper.findById(200L)).thenReturn(managerUser);
        when(sysUserMapper.findById(201L)).thenReturn(newDeptManagerUser);
        when(sysUserMapper.findById(300L)).thenReturn(hrUser);
        when(sysUserMapper.findById(400L)).thenReturn(financeUser);
        when(sysUserMapper.findFirstByRoleCode("ROLE_HR")).thenReturn(hrUser);
        when(sysUserMapper.findFirstByRoleCode("ROLE_FINANCE")).thenReturn(financeUser);
    }

    // ═══════════════ 顺序门控 ═══════════════

    @Nested
    @DisplayName("顺序门控")
    class SequentialGating {

        @Test
        @DisplayName("存在更低步骤未审批时拒绝当前审批")
        void shouldRejectWhenLowerStepPending() {
            ApprovalRecord record = new ApprovalRecord();
            record.setId(2L);
            record.setBusinessType(BusinessTypeEnum.ONBOARDING.getCode());
            record.setBusinessId(1L);
            record.setStepOrder(2); // 第2步
            record.setApproverId(300L);
            record.setIsPending(1);

            when(recordMapper.selectById(2L)).thenReturn(record);
            when(recordMapper.countLowerPending(
                    BusinessTypeEnum.ONBOARDING.getCode(), 1L, 2)).thenReturn(1); // 第1步未审

            BaseException ex = assertThrows(BaseException.class,
                    () -> stateMachine.processApproval(2L, 1, "审批通过", 300L));
            assertTrue(ex.getMessage().contains("前置审批") || ex.getMessage().contains("完成"),
                    "应提示先完成前置审批步骤");
        }

        @Test
        @DisplayName("无更低步骤未审批时正常执行")
        void shouldAllowWhenNoLowerStepPending() {
            ApprovalRecord record = new ApprovalRecord();
            record.setId(1L);
            record.setBusinessType(BusinessTypeEnum.ONBOARDING.getCode());
            record.setBusinessId(1L);
            record.setStepOrder(1);
            record.setApproverId(200L);
            record.setIsPending(1);

            when(recordMapper.selectById(1L)).thenReturn(record);
            when(recordMapper.countLowerPending(
                    BusinessTypeEnum.ONBOARDING.getCode(), 1L, 1)).thenReturn(0);

            // 第1步全部通过(无剩余待办)
            when(recordMapper.selectPendingByBusiness(
                    BusinessTypeEnum.ONBOARDING.getCode(), 1L)).thenReturn(List.of());

            var result = stateMachine.processApproval(1L, 1, "通过", 200L);

            assertTrue(result.isApproved());
            verify(recordMapper).updateAction(1L, 1, "通过", 0);
        }

        @Test
        @DisplayName("非本人审批记录应拒绝")
        void shouldRejectNonOwnerApproval() {
            ApprovalRecord record = new ApprovalRecord();
            record.setId(1L);
            record.setBusinessType(1);
            record.setBusinessId(1L);
            record.setApproverId(200L); // 审批人是200
            record.setIsPending(1);

            when(recordMapper.selectById(1L)).thenReturn(record);

            BaseException ex = assertThrows(BaseException.class,
                    () -> stateMachine.processApproval(1L, 1, "通过", 999L)); // 操作人是999
            assertTrue(ex.getMessage().contains("审批人"));
        }

        @Test
        @DisplayName("已处理记录不可重复审批")
        void shouldRejectProcessedRecord() {
            ApprovalRecord record = new ApprovalRecord();
            record.setId(1L);
            record.setApproverId(200L);
            record.setIsPending(0); // 已处理

            when(recordMapper.selectById(1L)).thenReturn(record);

            BaseException ex = assertThrows(BaseException.class,
                    () -> stateMachine.processApproval(1L, 1, "通过", 200L));
            assertTrue(ex.getMessage().contains("已处理"));
        }
    }

    // ═══════════════ 条件步骤 ═══════════════

    @Nested
    @DisplayName("条件审批步骤生成/跳过")
    class ConditionalSteps {

        @Test
        @DisplayName("needHr=false 时跳过 HR 审批步骤")
        void shouldSkipHrStepWhenNotNeeded() {
            ApprovalTemplate deptStep = buildTemplate(1L, 1, 1, "dept_manager", null);
            ApprovalTemplate hrStep = buildTemplate(2L, 1, 2, "hr_specialist", "needHr");

            when(templateMapper.selectByBusinessType(anyInt())).thenReturn(List.of(deptStep, hrStep));
            when(sysUserMapper.findFirstByRoleCode("ROLE_HR")).thenReturn(hrUser);

            ApprovalStateMachineService.ApprovalContext ctx =
                    ApprovalStateMachineService.ApprovalContext.ofDept(1L, false); // needHr=false

            stateMachine.startApproval(BusinessTypeEnum.ONBOARDING.getCode(), 1L, ctx);

            // 只生成部门主管步骤，HR步骤被跳过
            ArgumentCaptor<List<ApprovalRecord>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordMapper).insertBatch(captor.capture());
            List<ApprovalRecord> generated = captor.getValue();
            assertEquals(1, generated.size(), "应只生成1条审批记录");
            assertEquals(200L, generated.get(0).getApproverId());
        }

        @Test
        @DisplayName("needHr=true 时生成 HR 审批步骤")
        void shouldGenerateHrStepWhenNeeded() {
            ApprovalTemplate deptStep = buildTemplate(1L, 1, 1, "dept_manager", null);
            ApprovalTemplate hrStep = buildTemplate(2L, 1, 2, "hr_specialist", "needHr");

            when(templateMapper.selectByBusinessType(anyInt())).thenReturn(List.of(deptStep, hrStep));
            when(sysUserMapper.findFirstByRoleCode("ROLE_HR")).thenReturn(hrUser);

            ApprovalStateMachineService.ApprovalContext ctx =
                    ApprovalStateMachineService.ApprovalContext.ofDept(1L, true); // needHr=true

            stateMachine.startApproval(BusinessTypeEnum.ONBOARDING.getCode(), 1L, ctx);

            ArgumentCaptor<List<ApprovalRecord>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordMapper).insertBatch(captor.capture());
            List<ApprovalRecord> generated = captor.getValue();
            assertEquals(2, generated.size(), "应生成2条审批记录");
        }

        @Test
        @DisplayName("hasSalaryAdjust=true 时生成财务审批步骤")
        void shouldGenerateFinanceStepWhenSalaryAdjusted() {
            ApprovalTemplate step1 = buildTemplate(1L, 3, 1, "old_dept_manager", null);
            ApprovalTemplate step2 = buildTemplate(2L, 3, 2, "new_dept_manager", null);
            ApprovalTemplate step3 = buildTemplate(3L, 3, 3, "hr_specialist", null);
            ApprovalTemplate step4 = buildTemplate(4L, 3, 4, "finance_specialist", "hasSalaryAdjust");

            when(templateMapper.selectByBusinessType(anyInt()))
                    .thenReturn(List.of(step1, step2, step3, step4));
            when(sysUserMapper.findFirstByRoleCode("ROLE_HR")).thenReturn(hrUser);
            when(sysUserMapper.findFirstByRoleCode("ROLE_FINANCE")).thenReturn(financeUser);

            ApprovalStateMachineService.ApprovalContext ctx =
                    ApprovalStateMachineService.ApprovalContext.ofTransfer(1L, 2L, true);

            stateMachine.startApproval(BusinessTypeEnum.TRANSFER.getCode(), 1L, ctx);

            ArgumentCaptor<List<ApprovalRecord>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordMapper).insertBatch(captor.capture());
            List<ApprovalRecord> generated = captor.getValue();
            assertEquals(4, generated.size(), "应生成4条审批记录（含财务步骤）");
        }
    }

    // ═══════════════ 审批超时 ═══════════════

    @Nested
    @DisplayName("审批截止时间与超时")
    class DueTime {

        @Test
        @DisplayName("启动审批时每个步骤应计算48h截止时间")
        void shouldSet48hDueTimeOnStart() {
            ApprovalTemplate deptStep = buildTemplate(1L, 1, 1, "dept_manager", null);
            when(templateMapper.selectByBusinessType(anyInt())).thenReturn(List.of(deptStep));

            ApprovalStateMachineService.ApprovalContext ctx =
                    ApprovalStateMachineService.ApprovalContext.ofDept(1L);

            stateMachine.startApproval(BusinessTypeEnum.ONBOARDING.getCode(), 1L, ctx);

            ArgumentCaptor<List<ApprovalRecord>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordMapper).insertBatch(captor.capture());
            ApprovalRecord generated = captor.getValue().get(0);
            assertNotNull(generated.getDueTime(), "应设置截止时间");
            // 截止时间应在当前时间 + 48h 左右
            LocalDateTime expectedMin = LocalDateTime.now().plusHours(47);
            LocalDateTime expectedMax = LocalDateTime.now().plusHours(49);
            assertTrue(generated.getDueTime().isAfter(expectedMin)
                    && generated.getDueTime().isBefore(expectedMax),
                    "截止时间应在48h左右");
        }

        @Test
        @DisplayName("审批待办通知应发送给审批人")
        void shouldSendNotificationOnStart() {
            ApprovalTemplate deptStep = buildTemplate(1L, 1, 1, "dept_manager", null);
            when(templateMapper.selectByBusinessType(anyInt())).thenReturn(List.of(deptStep));

            ApprovalStateMachineService.ApprovalContext ctx =
                    ApprovalStateMachineService.ApprovalContext.ofDept(1L);

            stateMachine.startApproval(BusinessTypeEnum.ONBOARDING.getCode(), 1L, ctx);

            verify(notificationService).sendApprovalNotify(
                    eq(200L), contains("审批待办"), anyString(), anyInt(), anyLong());
        }
    }

    // ═══════════════ 辅助方法 ═══════════════

    private ApprovalTemplate buildTemplate(Long id, int bizType, int stepOrder,
                                           String approverTarget, String conditionExpr) {
        ApprovalTemplate tpl = new ApprovalTemplate();
        tpl.setId(id);
        tpl.setBusinessType(bizType);
        tpl.setStepOrder(stepOrder);
        tpl.setApproverTarget(approverTarget);
        tpl.setStepName(approverTarget + "_step");
        tpl.setConditionExpr(conditionExpr);
        return tpl;
    }
}
