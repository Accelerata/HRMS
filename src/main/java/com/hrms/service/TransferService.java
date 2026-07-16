package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.TransferSaveDTO;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 调岗申请 Service
 *
 * 审批流：old_dept_manager → new_dept_manager → hr_specialist（三级顺序审批）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferApplicationMapper transferMapper;
    private final ApprovalStateMachineService stateMachine;
    private final EmployeeMapper employeeMapper;
    private final EmployeeTransferMapper transferLogMapper;
    private final DepartmentMapper departmentMapper;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 分页查询 */
    public PageResult<TransferVO> page(Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<TransferVO> records = transferMapper.selectPage(status, keyword, offset, size);
        int total = transferMapper.countPage(status, keyword);
        return PageResult.of(records, total, page, size);
    }

    /** 获取详情 */
    public TransferVO getDetail(Long id) {
        TransferVO vo = transferMapper.selectVOById(id);
        if (vo == null) throw BaseException.notFound("调岗申请不存在");
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(vo.getStatus()).getLabel());
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.TRANSFER.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交调岗申请 */
    @Transactional
    public void submit(TransferSaveDTO dto) {
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) throw BaseException.notFound("员工不存在");

        // 校验在职状态：试用期或正式
        int status = emp.getStatus();
        if (status != EmployeeStatusEnum.PROBATION.getCode() && status != EmployeeStatusEnum.REGULAR.getCode()) {
            throw BaseException.badRequest("该员工当前状态不可调岗");
        }
        // 校验部门必须变更
        if (dto.getToDeptId().equals(emp.getDeptId())) {
            throw BaseException.badRequest("新部门必须与原部门不同");
        }

        TransferApplication entity = new TransferApplication();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setFromDeptId(emp.getDeptId());
        entity.setToDeptId(dto.getToDeptId());
        entity.setFromPositionId(emp.getPositionId());
        entity.setToPositionId(dto.getToPositionId());
        entity.setToGrade(dto.getToGrade());
        entity.setToReportTo(dto.getToReportTo());
        entity.setSalaryAdjust(dto.getSalaryAdjust());
        entity.setTransferReason(dto.getTransferReason());
        entity.setEffectiveDate(LocalDate.parse(dto.getEffectiveDate(), DATE_FMT));
        entity.setOldManagerApproved(0);
        entity.setNewManagerApproved(0);
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());
        transferMapper.insert(entity);

        // 三级顺序审批：原部门 → 新部门 → HR（薪资调整时额外触发财务审批）
        boolean hasSalaryAdjust = dto.getSalaryAdjust() != null
                && dto.getSalaryAdjust().compareTo(java.math.BigDecimal.ZERO) != 0;
        log.info("调岗薪资调整判断: transferId={}, salaryAdjust={}, hasSalaryAdjust={}",
                entity.getId(), dto.getSalaryAdjust(), hasSalaryAdjust);
        stateMachine.startApproval(BusinessTypeEnum.TRANSFER.getCode(), entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofTransfer(emp.getDeptId(), dto.getToDeptId(), hasSalaryAdjust)
                        .withSubmitter(entity.getSubmitterId()));

        log.info("调岗申请已提交: id={}, employeeId={}, from={} to={}",
                entity.getId(), dto.getEmployeeId(), emp.getDeptId(), dto.getToDeptId());
    }

    /** 审批 */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        TransferApplication entity = transferMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("调岗申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> pendingRecords = stateMachine.getApprovalRecords(BusinessTypeEnum.TRANSFER.getCode(), id);
        ApprovalRecord myRecord = pendingRecords.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            transferMapper.update(entity);
            return;
        }

        if (result.isApproved()) {
            // 三级全部通过，HR备案通过后生效
            entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            transferMapper.update(entity);
            executeTransfer(entity);
        } else {
            transferMapper.update(entity);
            log.info("调岗审批部分通过: id={}", id);
        }
    }

    /** 执行调岗 */
    private void executeTransfer(TransferApplication entity) {
        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp == null) return;

        // 记录异动前数据
        String beforeData = String.format("{\"deptId\":%d,\"positionId\":%d,\"grade\":\"%s\",\"reportTo\":%d}",
                emp.getDeptId(), emp.getPositionId(), emp.getGrade(), emp.getReportTo());

        // 更新员工
        emp.setDeptId(entity.getToDeptId());
        if (entity.getToPositionId() != null) emp.setPositionId(entity.getToPositionId());
        if (entity.getToGrade() != null) emp.setGrade(entity.getToGrade());
        if (entity.getToReportTo() != null) emp.setReportTo(entity.getToReportTo());
        employeeMapper.update(emp);

        // 异动后数据
        String afterData = String.format("{\"deptId\":%d,\"positionId\":%d,\"grade\":\"%s\",\"reportTo\":%d}",
                emp.getDeptId(), emp.getPositionId(), emp.getGrade(), emp.getReportTo());

        // 异动日志
        EmployeeTransfer transfer = new EmployeeTransfer();
        transfer.setEmployeeId(entity.getEmployeeId());
        transfer.setTransferType(TransferTypeEnum.TRANSFER.getCode());
        transfer.setBusinessId(entity.getId());
        transfer.setBeforeData(beforeData);
        transfer.setAfterData(afterData);
        transfer.setEffectiveDate(entity.getEffectiveDate());
        transfer.setRemark("调岗审批通过: " + entity.getFromDeptId() + " → " + entity.getToDeptId()
                + (entity.getTransferReason() != null ? ", 原因: " + entity.getTransferReason() : ""));
        transferLogMapper.insert(transfer);

        // 通知员工
        if (emp.getUserId() != null) {
            notificationService.send(emp.getUserId(), "调岗通知",
                    "您的调岗申请已审批通过，请查看新部门信息。", 1,
                    BusinessTypeEnum.TRANSFER.getCode(), entity.getId());
        }

        log.info("调岗已执行: employeeId={}, fromDept={}, toDept={}, grade={}",
                entity.getEmployeeId(), entity.getFromDeptId(), entity.getToDeptId(), entity.getToGrade());
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
