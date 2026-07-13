package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.ResignationSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.ResignationTypeEnum;
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
 * 离职申请 Service
 *
 * 审批通过后：禁用账号、更新员工状态、释放考勤组、保留脱敏档案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResignationService {

    private final ResignationApplicationMapper resignationMapper;
    private final ApprovalStateMachineService stateMachine;
    private final EmployeeMapper employeeMapper;
    private final EmployeeTransferMapper transferMapper;
    private final SysUserMapper sysUserMapper;
    private final DepartmentMapper departmentMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 分页查询 */
    public PageResult<ResignationVO> page(Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<ResignationVO> records = resignationMapper.selectPage(status, keyword, offset, size);
        int total = resignationMapper.countPage(status, keyword);
        return PageResult.of(records, total, page, size);
    }

    /** 获取详情 */
    public ResignationVO getDetail(Long id) {
        ResignationVO vo = resignationMapper.selectVOById(id);
        if (vo == null) {
            throw BaseException.notFound("离职申请不存在");
        }
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(vo.getStatus()).getLabel());
        vo.setResignationTypeLabel(ResignationTypeEnum.fromCode(vo.getResignationType()).getLabel());
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(
                BusinessTypeEnum.RESIGNATION.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交离职申请 */
    @Transactional
    public void submit(ResignationSaveDTO dto) {
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw BaseException.notFound("员工不存在");
        }
        if (emp.getStatus() == 3 || emp.getStatus() == 4) { // 3=待离职 4=已离职
            throw BaseException.badRequest("该员工已离职");
        }

        ResignationApplication entity = new ResignationApplication();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setResignationType(dto.getResignationType());
        entity.setResignationReason(dto.getResignationReason());
        entity.setResignationDate(LocalDate.parse(dto.getResignationDate(), DATE_FMT));
        entity.setHandoverInfo(dto.getHandoverInfo());
        entity.setHandoverTo(dto.getHandoverTo());
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());

        resignationMapper.insert(entity);

        // 启动审批流程
        stateMachine.startApproval(
                BusinessTypeEnum.RESIGNATION.getCode(),
                entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(emp.getDeptId())
        );

        log.info("离职申请已提交: id={}, employeeId={}", entity.getId(), dto.getEmployeeId());
    }

    /** 审批 */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        ResignationApplication entity = resignationMapper.selectById(id);
        if (entity == null) {
            throw BaseException.notFound("离职申请不存在");
        }
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(
                BusinessTypeEnum.RESIGNATION.getCode(), id);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1
                        && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            resignationMapper.update(entity);
            log.info("离职申请已拒绝: id={}", id);
            return;
        }

        if (result.isApproved()) {
            entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            resignationMapper.update(entity);
            executeResignation(entity);
            log.info("离职审批全部通过，账号已禁用: id={}, employeeId={}", id, entity.getEmployeeId());
        }
    }

    /** 执行离职：禁用账号 + 更新员工状态 + 写异动日志 */
    private void executeResignation(ResignationApplication entity) {
        // 1. 禁用系统账号
        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null && emp.getUserId() != null) {
            SysUser user = sysUserMapper.findById(emp.getUserId());
            if (user != null) {
                user.setStatus(0); // 禁用账号
            }
        }

        // 2. 更新员工状态为已离职(status=4) + 待离职(status=3)由提交时设置
        if (emp != null) {
            emp.setStatus(4); // 4=已离职
            emp.setResignDate(entity.getResignationDate());
            employeeMapper.update(emp);
        }

        // 3. 写入异动日志（含交接信息）
        EmployeeTransfer transfer = new EmployeeTransfer();
        transfer.setEmployeeId(entity.getEmployeeId());
        transfer.setTransferType(TransferTypeEnum.RESIGNATION.getCode());
        transfer.setBusinessId(entity.getId());
        transfer.setEffectiveDate(entity.getResignationDate());
        transfer.setRemark(String.format("离职审批通过，类型: %s，接手人ID: %s",
                ResignationTypeEnum.fromCode(entity.getResignationType()).getLabel(),
                entity.getHandoverTo()));
        transferMapper.insert(transfer);

        // 4. TODO: 释放考勤组（Phase 4 考勤模块实现）
        // 5. TODO: 标记停止次月薪资核算（Phase 5 薪资模块实现）

        log.info("离职已执行: employeeId={}, resignDate={}", entity.getEmployeeId(), entity.getResignationDate());
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
