package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.TransferSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalActionEnum;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 调岗申请 Service
 *
 * 特殊逻辑：调岗需要原部门主管 + 新部门主管 双重审批（并行）。
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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 分页查询 */
    public PageResult<TransferVO> page(Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<TransferVO> records = transferMapper.selectPage(status, keyword, offset, size);
        int total = transferMapper.countPage(status, keyword);
        return PageResult.of(total, records);
    }

    /** 获取详情 */
    public TransferVO getDetail(Long id) {
        TransferVO vo = transferMapper.selectVOById(id);
        if (vo == null) {
            throw BaseException.notFound("调岗申请不存在");
        }
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(vo.getStatus()).getLabel());
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(
                BusinessTypeEnum.TRANSFER.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交调岗申请 */
    @Transactional
    public void submit(TransferSaveDTO dto) {
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }

        TransferApplication entity = new TransferApplication();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setFromDeptId(emp.getDeptId());
        entity.setToDeptId(dto.getToDeptId());
        entity.setFromPositionId(emp.getPositionId());
        entity.setToPositionId(dto.getToPositionId());
        entity.setTransferReason(dto.getTransferReason());
        entity.setEffectiveDate(LocalDate.parse(dto.getEffectiveDate(), DATE_FMT));
        entity.setOldManagerApproved(0);
        entity.setNewManagerApproved(0);
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());

        transferMapper.insert(entity);

        // 启动审批 — 并行：原部门主管 + 新部门主管
        stateMachine.startApproval(
                BusinessTypeEnum.TRANSFER.getCode(),
                entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofTransfer(emp.getDeptId(), dto.getToDeptId())
        );

        log.info("调岗申请已提交: id={}, employeeId={}, from={} to={}",
                entity.getId(), dto.getEmployeeId(), emp.getDeptId(), dto.getToDeptId());
    }

    /** 审批调岗（按部门区分审批人） */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        TransferApplication entity = transferMapper.selectById(id);
        if (entity == null) {
            throw BaseException.notFound("调岗申请不存在");
        }
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        // 调岗审批需指定 role: "old"=原部门 / "new"=新部门
        boolean isOldManager = "old".equals(dto.getRole());
        boolean isNewManager = "new".equals(dto.getRole());

        if (!isOldManager && !isNewManager) {
            throw BaseException.badRequest("调岗审批需指定 role 为 old 或 new");
        }

        // 找到对应的审批记录
        List<ApprovalRecord> pendingRecords = stateMachine.getApprovalRecords(
                BusinessTypeEnum.TRANSFER.getCode(), id);
        ApprovalRecord myRecord = pendingRecords.stream()
                .filter(r -> r.getIsPending() == 1
                        && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        // 处理审批
        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        // 更新对应的审批标记
        if (dto.getAction() == ApprovalActionEnum.APPROVE.getCode()) {
            if (isOldManager) {
                entity.setOldManagerApproved(1);
            } else {
                entity.setNewManagerApproved(1);
            }
        } else if (dto.getAction() == ApprovalActionEnum.REJECT.getCode()
                || dto.getAction() == ApprovalActionEnum.RETURN.getCode()) {
            if (isOldManager) {
                entity.setOldManagerApproved(2);
            } else {
                entity.setNewManagerApproved(2);
            }
        }

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            transferMapper.update(entity);
            log.info("调岗申请已拒绝: id={}, role={}", id, dto.getRole());
            return;
        }

        if (result.isApproved()) {
            // 双重审批全部通过
            entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            transferMapper.update(entity);
            executeTransfer(entity);
            log.info("调岗审批全部通过，员工档案已更新: id={}, employeeId={}", id, entity.getEmployeeId());
        } else {
            // 部分通过，更新审批标记
            transferMapper.update(entity);
            log.info("调岗审批部分通过: id={}, role={}", id, dto.getRole());
        }
    }

    /** 执行调岗：更新员工部门/职位 + 写入异动日志 */
    private void executeTransfer(TransferApplication entity) {
        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            // 记录异动前数据
            String beforeData = "{\"deptId\":" + emp.getDeptId() + ",\"positionId\":" + emp.getPositionId() + "}";

            // 更新员工
            emp.setDeptId(entity.getToDeptId());
            if (entity.getToPositionId() != null) {
                emp.setPositionId(entity.getToPositionId());
            }
            employeeMapper.update(emp);

            // 异动后数据
            String afterData = "{\"deptId\":" + emp.getDeptId() + ",\"positionId\":" + emp.getPositionId() + "}";

            // 写入异动日志
            EmployeeTransfer transfer = new EmployeeTransfer();
            transfer.setEmployeeId(entity.getEmployeeId());
            transfer.setTransferType(TransferTypeEnum.TRANSFER.getCode());
            transfer.setBusinessId(entity.getId());
            transfer.setBeforeData(beforeData);
            transfer.setAfterData(afterData);
            transfer.setEffectiveDate(entity.getEffectiveDate());
            transfer.setRemark("调岗审批通过: " +
                    (entity.getFromDeptId() + " → " + entity.getToDeptId()));
            transferLogMapper.insert(transfer);
        }

        log.info("调岗已执行: employeeId={}, fromDept={}, toDept={}",
                entity.getEmployeeId(), entity.getFromDeptId(), entity.getToDeptId());
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
