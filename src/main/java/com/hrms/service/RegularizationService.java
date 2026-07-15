package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.RegularizationSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.EmployeeStatusEnum;
import com.hrms.enums.TransferTypeEnum;
import com.hrms.mapper.*;
import com.hrms.result.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 转正申请 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegularizationService {

    private final RegularizationApplicationMapper regularizationMapper;
    private final ApprovalStateMachineService stateMachine;
    private final EmployeeMapper employeeMapper;
    private final EmployeeTransferMapper transferMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    /** 分页查询 */
    public PageResult<RegularizationVO> page(Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<RegularizationVO> records = regularizationMapper.selectPage(status, keyword, offset, size);
        int total = regularizationMapper.countPage(status, keyword);
        return PageResult.of(records, total, page, size);
    }

    /** 获取详情 */
    public RegularizationVO getDetail(Long id) {
        RegularizationVO vo = regularizationMapper.selectVOById(id);
        if (vo == null) throw BaseException.notFound("转正申请不存在");
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(vo.getStatus()).getLabel());
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.REGULARIZATION.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交转正申请 */
    @Transactional
    public void submit(RegularizationSaveDTO dto) {
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) throw BaseException.notFound("员工不存在");
        if (emp.getStatus() != EmployeeStatusEnum.PROBATION.getCode()) {
            throw BaseException.badRequest("该员工不是试用期状态，无法发起转正");
        }
        if (dto.getProbationSummary() == null || dto.getProbationSummary().isBlank()) {
            throw BaseException.badRequest("试用期表现评价不能为空");
        }

        RegularizationApplication entity = new RegularizationApplication();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setFormalSalary(dto.getFormalSalary());
        entity.setProbationSummary(dto.getProbationSummary());
        entity.setSupervisorComment(dto.getSupervisorComment());
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());
        regularizationMapper.insert(entity);

        stateMachine.startApproval(BusinessTypeEnum.REGULARIZATION.getCode(), entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(emp.getDeptId()));

        log.info("转正申请已提交: id={}, employeeId={}", entity.getId(), dto.getEmployeeId());
    }

    /** 审批（三分支：通过/延长试用/不通过） */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        RegularizationApplication entity = regularizationMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("转正申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.REGULARIZATION.getCode(), id);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            regularizationMapper.update(entity);
            return;
        }

        if (result.isApproved()) {
            // 仅在最终一级 HR 审批时处理三分支
            int resultType = dto.getResultType() != null ? dto.getResultType() : 1;
            entity.setResultType(resultType);

            switch (resultType) {
                case 1 -> executePass(entity);       // 通过
                case 2 -> executeExtend(entity, dto.getExtendedMonths()); // 延长试用
                case 3 -> executeFail(entity);       // 不通过辞退
                default -> executePass(entity);
            }
        }
    }

    private void executePass(RegularizationApplication entity) {
        entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
        regularizationMapper.update(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            emp.setStatus(EmployeeStatusEnum.REGULAR.getCode());
            emp.setRegularDate(LocalDate.now());
            employeeMapper.update(emp);
        }

        writeTransferLog(entity.getEmployeeId(), TransferTypeEnum.REGULARIZATION, entity.getId(), LocalDate.now(),
                "转正审批通过");

        if (emp != null && emp.getUserId() != null) {
            notificationService.send(emp.getUserId(), "转正通知",
                    "恭喜您已通过转正审批，正式成为公司一员！", 1,
                    BusinessTypeEnum.REGULARIZATION.getCode(), entity.getId());
        }
        log.info("转正通过: employeeId={}", entity.getEmployeeId());
    }

    private void executeExtend(RegularizationApplication entity, Integer extendedMonths) {
        int months = extendedMonths != null && extendedMonths > 0 ? extendedMonths : 1;
        entity.setExtendedMonths(months);
        entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
        regularizationMapper.update(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            emp.setRegularDate(emp.getRegularDate().plusMonths(months));
            employeeMapper.update(emp);
        }

        writeTransferLog(entity.getEmployeeId(), TransferTypeEnum.REGULARIZATION, entity.getId(), LocalDate.now(),
                "转正审批: 延长试用 " + months + " 个月");

        if (emp != null && emp.getUserId() != null) {
            notificationService.send(emp.getUserId(), "试用期延长通知",
                    "您的试用期已延长 " + months + " 个月，请继续努力。", 3,
                    BusinessTypeEnum.REGULARIZATION.getCode(), entity.getId());
        }
        log.info("转正延长试用: employeeId={}, months={}", entity.getEmployeeId(), months);
    }

    private void executeFail(RegularizationApplication entity) {
        entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
        regularizationMapper.update(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            emp.setStatus(EmployeeStatusEnum.PENDING_RESIGN.getCode());
            emp.setResignDate(LocalDate.now());
            employeeMapper.update(emp);
        }

        writeTransferLog(entity.getEmployeeId(), TransferTypeEnum.REGULARIZATION, entity.getId(), LocalDate.now(),
                "转正不通过，员工转为待离职");

        // 通知员工
        if (emp != null && emp.getUserId() != null) {
            notificationService.sendWarning(emp.getUserId(), "转正未通过",
                    "很遗憾，您的转正申请未通过审批。", BusinessTypeEnum.REGULARIZATION.getCode(), entity.getId());
        }
        log.info("转正不通过: employeeId={}", entity.getEmployeeId());
    }

    /** 查询试用期即将到期员工 */
    public List<ExpiringEmployeeVO> getExpiringEmployees(int days) {
        List<Employee> employees = employeeMapper.selectExpiringProbation(days);
        List<ExpiringEmployeeVO> result = new ArrayList<>();
        for (Employee emp : employees) {
            ExpiringEmployeeVO vo = new ExpiringEmployeeVO();
            vo.setEmployeeId(emp.getId()); vo.setEmployeeName(emp.getName());
            vo.setHireDate(emp.getEntryDate()); vo.setProbationEndDate(emp.getRegularDate());
            if (emp.getRegularDate() != null) {
                vo.setDaysRemaining((int) (emp.getRegularDate().toEpochDay() - LocalDate.now().toEpochDay()));
            }
            Department dept = departmentMapper.selectById(emp.getDeptId());
            if (dept != null) vo.setDeptName(dept.getDeptName());
            result.add(vo);
        }
        return result;
    }

    private void writeTransferLog(Long employeeId, TransferTypeEnum type, Long businessId, LocalDate effectiveDate, String remark) {
        EmployeeTransfer transfer = new EmployeeTransfer();
        transfer.setEmployeeId(employeeId); transfer.setTransferType(type.getCode());
        transfer.setBusinessId(businessId); transfer.setEffectiveDate(effectiveDate);
        transfer.setRemark(remark);
        transferMapper.insert(transfer);
    }

    private ApprovalRecordVO toRecordVO(ApprovalRecord r) {
        ApprovalRecordVO vo = new ApprovalRecordVO();
        vo.setId(r.getId()); vo.setBusinessType(r.getBusinessType());
        vo.setBusinessId(r.getBusinessId()); vo.setStepOrder(r.getStepOrder());
        vo.setApproverId(r.getApproverId()); vo.setApproverName(r.getApproverName());
        if (r.getAction() != null) {
            vo.setAction(r.getAction());
            vo.setActionLabel(switch (r.getAction()) { case 1 -> "通过"; case 2 -> "拒绝"; case 3 -> "退回"; default -> "未知"; });
        }
        vo.setComment(r.getComment()); vo.setOperateTime(r.getOperateTime()); vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
