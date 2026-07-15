package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.crypto.EncryptionUtil;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.OnboardingSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.EmployeeStatusEnum;
import com.hrms.enums.TransferTypeEnum;
import com.hrms.mapper.*;
import com.hrms.utils.EmployeeNoGenerator;
import com.hrms.result.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 入职申请 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OnboardingApplicationMapper onboardingMapper;
    private final ApprovalStateMachineService stateMachine;
    private final EmployeeMapper employeeMapper;
    private final EmployeeTransferMapper transferMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final GradeSalaryRangeMapper gradeSalaryRangeMapper;
    private final EncryptionUtil encryptionUtil;
    private final EmployeeNoGenerator employeeNoGenerator;
    private final EmployeeAccountService employeeAccountService;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 分页查询 */
    public PageResult<OnboardingVO> page(Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<OnboardingVO> records = onboardingMapper.selectPage(status, keyword, offset, size);
        int total = onboardingMapper.countPage(status, keyword);
        return PageResult.of(records, total, page, size);
    }

    /** 获取详情 */
    public OnboardingVO getDetail(Long id) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        OnboardingVO vo = buildVO(entity);
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.ONBOARDING.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交入职申请 */
    @Transactional
    public void submit(OnboardingSaveDTO dto) {
        OnboardingApplication entity = buildEntity(dto, ApprovalStatusEnum.PENDING);
        onboardingMapper.insert(entity);
        startApproval(entity);
        log.info("入职申请已提交: id={}, name={}", entity.getId(), dto.getRealName());
    }

    /** 保存草稿 */
    @Transactional
    public void saveDraft(OnboardingSaveDTO dto) {
        OnboardingApplication entity = buildEntity(dto, ApprovalStatusEnum.DRAFT);
        onboardingMapper.insert(entity);
        log.info("入职草稿已保存: id={}", entity.getId());
    }

    /** 更新草稿 */
    @Transactional
    public void update(OnboardingSaveDTO dto) {
        OnboardingApplication entity = onboardingMapper.selectById(dto.getId());
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.DRAFT.getCode()
                && entity.getStatus() != ApprovalStatusEnum.REJECTED.getCode()) {
            throw BaseException.badRequest("仅草稿或已拒绝状态可编辑");
        }
        fillEntity(entity, dto);
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        onboardingMapper.update(entity);
        startApproval(entity);
        log.info("入职申请已重新提交: id={}", entity.getId());
    }

    /** 删除草稿 */
    @Transactional
    public void deleteDraft(Long id) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.DRAFT.getCode()) {
            throw BaseException.badRequest("仅草稿状态可删除");
        }
        onboardingMapper.deleteById(id);
        log.info("入职草稿已删除: id={}", id);
    }

    /** 审批中撤回（仅第一级未被审批时可撤回） */
    @Transactional
    public void withdraw(Long id) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("仅审批中状态可撤回");
        }
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(BusinessTypeEnum.ONBOARDING.getCode(), id);
        boolean anyProcessed = records.stream().anyMatch(r -> r.getIsPending() != 1);
        if (anyProcessed) {
            throw BaseException.badRequest("已有审批人处理，无法撤回");
        }
        entity.setStatus(ApprovalStatusEnum.DRAFT.getCode());
        onboardingMapper.update(entity);
        log.info("入职申请已撤回: id={}", id);
    }

    /** 审批 */
    @Transactional
    public OnboardingResultVO approve(Long id, ApprovalActionDTO dto) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        List<ApprovalRecord> pendingRecords = stateMachine.getApprovalRecords(BusinessTypeEnum.ONBOARDING.getCode(), id);
        ApprovalRecord myRecord = pendingRecords.stream()
                .filter(r -> r.getIsPending() == 1 && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            onboardingMapper.update(entity);
            return null;
        }

        if (result.isApproved()) {
            entity.setStatus(ApprovalStatusEnum.PENDING_ENTRY.getCode());
            onboardingMapper.update(entity);
            return executeOnboarding(entity);
        }

        return null;
    }

    /** 确认到岗 */
    @Transactional
    public void confirmArrival(Long id) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING_ENTRY.getCode()) {
            throw BaseException.badRequest("当前状态不是待入职，无法确认到岗");
        }

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp == null) throw BaseException.notFound("关联员工不存在");

        emp.setStatus(EmployeeStatusEnum.PROBATION.getCode());
        employeeMapper.update(emp);
        entity.setStatus(ApprovalStatusEnum.ONBOARDED.getCode());
        onboardingMapper.update(entity);

        // 写入异动日志
        writeTransferLog(emp.getId(), TransferTypeEnum.ONBOARDING, entity.getId(), entity.getEntryDate(),
                "确认到岗，员工转为试用期");

        // 通知
        if (emp.getUserId() != null) {
            notificationService.send(emp.getUserId(), "欢迎入职", "欢迎加入公司！请登录系统并修改初始密码。",
                    1, BusinessTypeEnum.ONBOARDING.getCode(), id);
        }

        log.info("确认到岗完成: onboardingId={}, employeeId={}", id, emp.getId());
    }

    /** 修改入职日期（待入职状态） */
    @Transactional
    public void updateEntryDate(Long id, String entryDateStr) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING_ENTRY.getCode()) {
            throw BaseException.badRequest("仅待入职状态可修改入职日期");
        }
        LocalDate newDate = LocalDate.parse(entryDateStr, DATE_FMT);
        entity.setEntryDate(newDate);
        onboardingMapper.update(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            emp.setEntryDate(newDate);
            emp.setRegularDate(newDate.plusMonths(entity.getProbationMonths() != null ? entity.getProbationMonths() : 3));
            employeeMapper.update(emp);
        }
        log.info("入职日期已修改: id={}, newDate={}", id, newDate);
    }

    /** 标记放弃入职 */
    @Transactional
    public void markAbandon(Long id) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) throw BaseException.notFound("入职申请不存在");
        if (entity.getStatus() != ApprovalStatusEnum.PENDING_ENTRY.getCode()) {
            throw BaseException.badRequest("仅待入职状态可标记放弃");
        }
        if (entity.getEmployeeId() != null) {
            Employee emp = employeeMapper.selectById(entity.getEmployeeId());
            if (emp != null) {
                emp.setStatus(EmployeeStatusEnum.RESIGNED.getCode());
                employeeMapper.update(emp);
                if (emp.getUserId() != null) {
                    employeeAccountService.disableAccount(emp.getUserId());
                }
            }
        }
        entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
        onboardingMapper.update(entity);
        log.info("入职申请已标记放弃: id={}", id);
    }

    // ═══════════════ 内部方法 ═══════════════

    private OnboardingResultVO executeOnboarding(OnboardingApplication entity) {
        // 1. 生成工号
        String employeeNo = employeeNoGenerator.generate(entity.getTargetDeptId());

        // 2. 创建员工档案（待入职状态）
        Employee emp = new Employee();
        emp.setEmployeeNo(employeeNo);
        emp.setName(entity.getRealName());
        emp.setPhone(entity.getPhone());
        emp.setPhoneHash(encryptionUtil.computeHash(entity.getPhone()));
        emp.setEmail(entity.getEmail());
        emp.setIdCard(entity.getIdCard());
        emp.setIdCardHash(encryptionUtil.computeHash(entity.getIdCard()));
        emp.setBirthday(extractBirthdayFromIdCard(entity.getIdCard()));
        emp.setGender(entity.getGender());
        emp.setRegisteredAddress(entity.getRegisteredAddress());
        emp.setCurrentAddress(entity.getCurrentAddress());
        emp.setDeptId(entity.getTargetDeptId());
        emp.setPositionId(entity.getTargetPositionId());
        emp.setGrade(entity.getGrade());
        emp.setReportTo(entity.getReportTo());
        emp.setWorkLocation(entity.getWorkLocation());
        emp.setEntryType(entity.getEntryType() != null ? entity.getEntryType() : 1);
        emp.setSalaryAccountId(entity.getSalaryAccountId());
        emp.setBaseSalary(entity.getOfferSalary());
        emp.setBankAccount(entity.getBankAccount());
        emp.setBankAccountHash(encryptionUtil.computeHash(entity.getBankAccount()));
        emp.setBankName(entity.getBankName());
        emp.setStatus(EmployeeStatusEnum.PENDING_ENTRY.getCode());
        emp.setEntryDate(entity.getEntryDate());
        emp.setRegularDate(entity.getEntryDate().plusMonths(
                entity.getProbationMonths() != null ? entity.getProbationMonths() : 3));
        employeeMapper.insert(emp);

        // 3. 创建系统账号
        String rawPwd = null;
        try {
            rawPwd = employeeAccountService.createAccount(emp.getId(), entity.getPhone(), "ROLE_EMPLOYEE");
        } catch (Exception e) {
            log.error("创建账号失败: phone={}", entity.getPhone(), e);
        }

        // 4. 回填关联
        entity.setEmployeeId(emp.getId());
        onboardingMapper.update(entity);

        // 5. 异动日志
        writeTransferLog(emp.getId(), TransferTypeEnum.ONBOARDING, entity.getId(), entity.getEntryDate(),
                "入职审批通过，待确认到岗，工号: " + employeeNo);

        // 6. 通知：欢迎候选人 + 通知HR/部门负责人
        if (emp.getUserId() != null) {
            notificationService.send(emp.getUserId(), "入职审批通过",
                    "您的入职申请已审批通过，请在入职日期当天联系HR确认到岗。",
                    1, BusinessTypeEnum.ONBOARDING.getCode(), entity.getId());
        }
        // 通知部门主管
        Department dept = departmentMapper.selectById(entity.getTargetDeptId());
        if (dept != null && dept.getManagerId() != null) {
            Employee manager = employeeMapper.selectById(dept.getManagerId());
            if (manager != null && manager.getUserId() != null) {
                notificationService.send(manager.getUserId(), "新员工待入职",
                        "候选人 " + entity.getRealName() + " 已审批通过，工号: " + employeeNo + "，等待确认到岗。",
                        1, BusinessTypeEnum.ONBOARDING.getCode(), entity.getId());
            }
        }

        OnboardingResultVO vo = new OnboardingResultVO();
        vo.setEmployeeNo(employeeNo);
        vo.setUsername(entity.getPhone());
        vo.setInitialPassword(rawPwd);
        return vo;
    }

    private void startApproval(OnboardingApplication entity) {
        boolean needHr = determineNeedHr(entity);
        log.info("入职二审条件判断: onboardingId={}, positionId={}, grade={}, salary={}, needHr={}",
                entity.getId(), entity.getTargetPositionId(), entity.getGrade(), entity.getOfferSalary(), needHr);
        stateMachine.startApproval(BusinessTypeEnum.ONBOARDING.getCode(), entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(entity.getTargetDeptId(), needHr));
    }

    /**
     * 判断入职是否需要 HR 二审
     * 判定逻辑：标准职位 + 薪资在对应职级范围内 → 不需要 HR 审批（仅部门主管审批即可）
     * 任一条件不满足 → 需要 HR 二审
     */
    private boolean determineNeedHr(OnboardingApplication entity) {
        // 1. 查询职位，判断是否标准职位
        Position position = positionMapper.selectById(entity.getTargetPositionId());
        boolean isStandard = position != null && position.getIsStandard() != null && position.getIsStandard() == 1;

        // 2. 查询职级薪资范围，判断薪资是否在范围内
        boolean salaryInRange = false;
        if (entity.getGrade() != null && entity.getOfferSalary() != null) {
            GradeSalaryRange range = gradeSalaryRangeMapper.selectByGradeCode(entity.getGrade());
            if (range != null) {
                salaryInRange = entity.getOfferSalary().compareTo(range.getMinSalary()) >= 0
                        && entity.getOfferSalary().compareTo(range.getMaxSalary()) <= 0;
            } else {
                // 未配置薪资范围时，视为在范围内（不因此触发 HR 审批）
                salaryInRange = true;
                log.debug("职级 {} 未配置薪资范围，默认视为在范围内", entity.getGrade());
            }
        }

        // 标准职位 + 薪资在范围内 → 不需要 HR 审批（needHr = false）
        return !(isStandard && salaryInRange);
    }

    private OnboardingApplication buildEntity(OnboardingSaveDTO dto, ApprovalStatusEnum status) {
        OnboardingApplication entity = new OnboardingApplication();
        fillEntity(entity, dto);
        entity.setStatus(status.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());
        return entity;
    }

    private void fillEntity(OnboardingApplication entity, OnboardingSaveDTO dto) {
        entity.setRealName(dto.getRealName());
        entity.setPhone(dto.getPhone());
        entity.setPhoneHash(encryptionUtil.computeHash(dto.getPhone()));
        entity.setEmail(dto.getEmail());
        entity.setIdCard(dto.getIdCard());
        entity.setIdCardHash(encryptionUtil.computeHash(dto.getIdCard()));
        entity.setTargetDeptId(dto.getTargetDeptId());
        entity.setTargetPositionId(dto.getTargetPositionId());
        entity.setOfferSalary(dto.getOfferSalary());
        entity.setProbationMonths(dto.getProbationMonths() != null ? dto.getProbationMonths() : 3);
        entity.setEntryDate(LocalDate.parse(dto.getEntryDate(), DATE_FMT));
        entity.setGender(dto.getGender());
        entity.setRegisteredAddress(dto.getRegisteredAddress());
        entity.setCurrentAddress(dto.getCurrentAddress());
        entity.setGrade(dto.getGrade());
        entity.setReportTo(dto.getReportTo());
        entity.setWorkLocation(dto.getWorkLocation());
        entity.setEntryType(dto.getEntryType() != null ? dto.getEntryType() : 1);
        entity.setEmploymentType(dto.getEmploymentType() != null ? dto.getEmploymentType() : 1);
        entity.setProbationSalaryRatio(dto.getProbationSalaryRatio() != null ? dto.getProbationSalaryRatio() : new BigDecimal("0.80"));
        entity.setSalaryAccountId(dto.getSalaryAccountId());
        entity.setBankAccount(dto.getBankAccount());
        entity.setBankAccountHash(encryptionUtil.computeHash(dto.getBankAccount()));
        entity.setBankName(dto.getBankName());
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

    private LocalDate extractBirthdayFromIdCard(String idCard) {
        if (idCard == null || idCard.length() < 14) return null;
        try {
            return LocalDate.parse(idCard.substring(6, 14), DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) { return null; }
    }

    private OnboardingVO buildVO(OnboardingApplication entity) {
        OnboardingVO vo = new OnboardingVO();
        vo.setId(entity.getId()); vo.setEmployeeId(entity.getEmployeeId());
        vo.setRealName(entity.getRealName()); vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail()); vo.setIdCard(entity.getIdCard());
        vo.setTargetDeptId(entity.getTargetDeptId()); vo.setTargetPositionId(entity.getTargetPositionId());
        vo.setOfferSalary(entity.getOfferSalary()); vo.setProbationMonths(entity.getProbationMonths());
        vo.setEntryDate(entity.getEntryDate()); vo.setStatus(entity.getStatus());
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(entity.getStatus()).getLabel());
        vo.setSubmitterId(entity.getSubmitterId());
        vo.setCreateTime(entity.getCreateTime()); vo.setUpdateTime(entity.getUpdateTime());
        Department dept = departmentMapper.selectById(entity.getTargetDeptId());
        if (dept != null) vo.setTargetDeptName(dept.getDeptName());
        return vo;
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
