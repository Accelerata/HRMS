package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.RegularizationSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.TransferTypeEnum;
import com.hrms.mapper.*;
import com.hrms.result.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        if (vo == null) {
            throw BaseException.notFound("转正申请不存在");
        }
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(vo.getStatus()).getLabel());
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(
                BusinessTypeEnum.REGULARIZATION.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交转正申请 */
    @Transactional
    public void submit(RegularizationSaveDTO dto) {
        // 校验员工存在且为试用期状态
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }
        if (emp.getStatus() != 1) { // 1=试用期
            throw BaseException.badRequest("该员工不是试用期状态，无法发起转正");
        }

        RegularizationApplication entity = new RegularizationApplication();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setFormalSalary(dto.getFormalSalary());
        entity.setProbationSummary(dto.getProbationSummary());
        entity.setSupervisorComment(dto.getSupervisorComment());
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());

        regularizationMapper.insert(entity);

        // 启动审批流程 — 审批人是员工所在部门的主管
        stateMachine.startApproval(
                BusinessTypeEnum.REGULARIZATION.getCode(),
                entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(emp.getDeptId())
        );

        log.info("转正申请已提交: id={}, employeeId={}", entity.getId(), dto.getEmployeeId());
    }

    /** 审批 */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        RegularizationApplication entity = regularizationMapper.selectById(id);
        if (entity == null) {
            throw BaseException.notFound("转正申请不存在");
        }
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(
                BusinessTypeEnum.REGULARIZATION.getCode(), id);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1
                        && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            regularizationMapper.update(entity);
            log.info("转正申请已拒绝: id={}", id);
            return;
        }

        if (result.isApproved()) {
            entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            regularizationMapper.update(entity);
            executeRegularization(entity);
            log.info("转正审批全部通过，员工已转正: id={}, employeeId={}", id, entity.getEmployeeId());
        }
    }

    /** 执行转正：更新员工状态 + 写入异动日志 */
    private void executeRegularization(RegularizationApplication entity) {
        // 更新员工状态为正式
        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            emp.setStatus(2); // 2=正式
            emp.setRegularDate(LocalDate.now());
            employeeMapper.update(emp);
        }

        // 写入异动日志
        EmployeeTransfer transfer = new EmployeeTransfer();
        transfer.setEmployeeId(entity.getEmployeeId());
        transfer.setTransferType(TransferTypeEnum.REGULARIZATION.getCode());
        transfer.setBusinessId(entity.getId());
        transfer.setEffectiveDate(LocalDate.now());
        transfer.setRemark("转正审批通过");
        transferMapper.insert(transfer);

        log.info("转正已执行: employeeId={}, formalSalary={}", entity.getEmployeeId(), entity.getFormalSalary());
    }

    /** 查询试用期即将到期员工 */
    public List<ExpiringEmployeeVO> getExpiringEmployees(int days) {
        List<Employee> employees = employeeMapper.selectExpiringProbation(days);
        List<ExpiringEmployeeVO> result = new ArrayList<>();
        for (Employee emp : employees) {
            ExpiringEmployeeVO vo = new ExpiringEmployeeVO();
            vo.setEmployeeId(emp.getId());
            vo.setEmployeeName(emp.getName());
            vo.setHireDate(emp.getEntryDate());
            vo.setProbationEndDate(emp.getRegularDate());
            if (emp.getRegularDate() != null) {
                vo.setDaysRemaining((int) (emp.getRegularDate().toEpochDay() - LocalDate.now().toEpochDay()));
            }
            Department dept = departmentMapper.selectById(emp.getDeptId());
            if (dept != null) vo.setDeptName(dept.getDeptName());
            result.add(vo);
        }
        return result;
    }

    private ApprovalRecordVO toRecordVO(ApprovalRecord r) {
        ApprovalRecordVO vo = new ApprovalRecordVO();
        vo.setId(r.getId());
        vo.setBusinessType(r.getBusinessType());
        vo.setBusinessId(r.getBusinessId());
        vo.setStepOrder(r.getStepOrder());
        vo.setApproverId(r.getApproverId());
        vo.setApproverName(r.getApproverName());
        if (r.getAction() != null) {
            vo.setAction(r.getAction());
            vo.setActionLabel(switch (r.getAction()) {
                case 1 -> "通过"; case 2 -> "拒绝"; case 3 -> "退回"; default -> "未知";
            });
        }
        vo.setComment(r.getComment());
        vo.setOperateTime(r.getOperateTime());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
