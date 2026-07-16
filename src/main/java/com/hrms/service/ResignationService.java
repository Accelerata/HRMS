package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.ResignationSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.EmployeeStatusEnum;
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
 * 审批通过后：置待离职 → 定时任务过渡到已离职 → 禁用账号、释放工号、保留脱敏档案
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
    private final EmployeeAccountService employeeAccountService;
    private final NotificationService notificationService;

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
        if (vo == null) throw BaseException.notFound("离职申请不存在");
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(vo.getStatus()).getLabel());
        vo.setResignationTypeLabel(ResignationTypeEnum.fromCode(vo.getResignationType()).getLabel());
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.RESIGNATION.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交离职申请 */
    @Transactional
    public void submit(ResignationSaveDTO dto) {
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) throw BaseException.notFound("员工不存在");

        // 校验在职状态
        int status = emp.getStatus();
        if (status != EmployeeStatusEnum.PROBATION.getCode() && status != EmployeeStatusEnum.REGULAR.getCode()) {
            throw BaseException.badRequest("该员工当前状态不可发起离职");
        }

        // 校验离职日期 ≥ 今天
        LocalDate resignDate = LocalDate.parse(dto.getResignationDate(), DATE_FMT);
        if (resignDate.isBefore(LocalDate.now())) {
            throw BaseException.badRequest("离职日期不能早于今天");
        }

        // 校验交接人必填
        if (dto.getHandoverTo() == null) {
            throw BaseException.badRequest("交接人不能为空");
        }

        ResignationApplication entity = new ResignationApplication();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setResignationType(dto.getResignationType());
        entity.setResignationReason(dto.getResignationReason());
        entity.setResignationDate(resignDate);
        entity.setHandoverInfo(dto.getHandoverInfo());
        entity.setHandoverTo(dto.getHandoverTo());
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());
        resignationMapper.insert(entity);

        stateMachine.startApproval(BusinessTypeEnum.RESIGNATION.getCode(), entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(emp.getDeptId())
                        .withSubmitter(entity.getSubmitterId()));

        log.info("离职申请已提交: id={}, employeeId={}", entity.getId(), dto.getEmployeeId());
    }

    /** 审批 */
    @Transactional
    public void approve(Long id, ApprovalActionDTO dto) {
        ResignationApplication entity = resignationMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("离职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.RESIGNATION.getCode(), id);
        ApprovalRecord myRecord = records.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            resignationMapper.update(entity);
            return;
        }

        if (result.isApproved()) {
            // 全部审批通过 → 置「待离职」并记录 resignDate
            entity.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            resignationMapper.update(entity);

            Employee emp = employeeMapper.selectById(entity.getEmployeeId());
            if (emp != null) {
                emp.setStatus(EmployeeStatusEnum.PENDING_RESIGN.getCode());
                emp.setResignDate(entity.getResignationDate());
                employeeMapper.update(emp);

                // 通知员工
                if (emp.getUserId() != null) {
                    notificationService.send(emp.getUserId(), "离职审批通过",
                            "您的离职申请已审批通过，最后工作日为 " + entity.getResignationDate() + "。",
                            1, BusinessTypeEnum.RESIGNATION.getCode(), id);
                }
            }

            // 异动日志
            writeTransferLog(entity.getEmployeeId(), TransferTypeEnum.RESIGNATION, id, entity.getResignationDate(),
                    String.format("离职审批通过，类型: %s，交接人: %s",
                            ResignationTypeEnum.fromCode(entity.getResignationType()).getLabel(),
                            entity.getHandoverTo()));

            log.info("离职审批通过，员工转为待离职: employeeId={}, resignDate={}",
                    entity.getEmployeeId(), entity.getResignationDate());
        }
    }

    /** 获取脱敏后的离职档案（按角色） */
    public ResignationVO getMaskedDetail(Long id) {
        ResignationVO vo = getDetail(id);
        // 脱敏：非HR/管理员角色隐藏敏感信息
        // TODO: 根据 BaseContext 中角色判断是否脱敏
        return vo;
    }

    private void writeTransferLog(Long employeeId, TransferTypeEnum type, Long businessId, LocalDate effectiveDate, String remark) {
        EmployeeTransfer transfer = new EmployeeTransfer();
        transfer.setEmployeeId(employeeId);
        transfer.setTransferType(type.getCode());
        transfer.setBusinessId(businessId);
        transfer.setEffectiveDate(effectiveDate);
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
