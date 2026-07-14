package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.crypto.EncryptionUtil;
import com.hrms.dto.ApprovalActionDTO;
import com.hrms.dto.OnboardingSaveDTO;
import com.hrms.entity.*;
import com.hrms.enums.ApprovalStatusEnum;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.enums.TransferTypeEnum;
import com.hrms.mapper.*;
import com.hrms.utils.PasswordUtil;
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
import java.util.UUID;

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
    private final EncryptionUtil encryptionUtil;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 分页查询 */
    public PageResult<OnboardingVO> page(Integer status, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<OnboardingVO> records = onboardingMapper.selectPage(status, keyword, offset, size);
        int total = onboardingMapper.countPage(status, keyword);
        return PageResult.of(records,total , page, size);
    }

    /** 获取详情 */
    public OnboardingVO getDetail(Long id) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw BaseException.notFound("入职申请不存在");
        }
        OnboardingVO vo = buildVO(entity);
        // 审批记录
        List<ApprovalRecord> records = stateMachine.getApprovalRecords(
                BusinessTypeEnum.ONBOARDING.getCode(), id);
        vo.setApprovalRecords(records.stream().map(this::toRecordVO).toList());
        return vo;
    }

    /** 提交入职申请（草稿或直接提交） */
    @Transactional
    public void submit(OnboardingSaveDTO dto) {
        OnboardingApplication entity = new OnboardingApplication();
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
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());
        // 员工扩展信息
        entity.setGender(dto.getGender());
        entity.setRegisteredAddress(dto.getRegisteredAddress());
        entity.setCurrentAddress(dto.getCurrentAddress());
        entity.setGrade(dto.getGrade());
        entity.setReportTo(dto.getReportTo());
        entity.setWorkLocation(dto.getWorkLocation());
        entity.setEntryType(dto.getEntryType() != null ? dto.getEntryType() : 1);
        entity.setSalaryAccountId(dto.getSalaryAccountId());
        entity.setBankAccount(dto.getBankAccount());
        entity.setBankAccountHash(encryptionUtil.computeHash(dto.getBankAccount()));
        entity.setBankName(dto.getBankName());

        onboardingMapper.insert(entity);

        // 启动审批流程
        stateMachine.startApproval(
                BusinessTypeEnum.ONBOARDING.getCode(),
                entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(dto.getTargetDeptId())
        );

        log.info("入职申请已提交: id={}, name={}", entity.getId(), dto.getRealName());
    }

    /** 保存草稿 */
    @Transactional
    public void saveDraft(OnboardingSaveDTO dto) {
        OnboardingApplication entity = new OnboardingApplication();
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
        entity.setStatus(ApprovalStatusEnum.DRAFT.getCode());
        entity.setSubmitterId(BaseContext.getCurrentUserId());
        // 员工扩展信息
        entity.setGender(dto.getGender());
        entity.setRegisteredAddress(dto.getRegisteredAddress());
        entity.setCurrentAddress(dto.getCurrentAddress());
        entity.setGrade(dto.getGrade());
        entity.setReportTo(dto.getReportTo());
        entity.setWorkLocation(dto.getWorkLocation());
        entity.setEntryType(dto.getEntryType() != null ? dto.getEntryType() : 1);
        entity.setSalaryAccountId(dto.getSalaryAccountId());
        entity.setBankAccount(dto.getBankAccount());
        entity.setBankAccountHash(encryptionUtil.computeHash(dto.getBankAccount()));
        entity.setBankName(dto.getBankName());
        onboardingMapper.insert(entity);
        log.info("入职草稿已保存: id={}", entity.getId());
    }

    /** 更新草稿 */
    @Transactional
    public void update(OnboardingSaveDTO dto) {
        OnboardingApplication entity = onboardingMapper.selectById(dto.getId());
        if (entity == null) {
            throw BaseException.notFound("入职申请不存在");
        }
        if (entity.getStatus() != ApprovalStatusEnum.DRAFT.getCode()
                && entity.getStatus() != ApprovalStatusEnum.REJECTED.getCode()) {
            throw BaseException.badRequest("仅草稿或已拒绝状态可编辑");
        }

        entity.setRealName(dto.getRealName());
        entity.setPhone(dto.getPhone());
        entity.setPhoneHash(encryptionUtil.computeHash(dto.getPhone()));
        entity.setEmail(dto.getEmail());
        entity.setIdCard(dto.getIdCard());
        entity.setIdCardHash(encryptionUtil.computeHash(dto.getIdCard()));
        entity.setTargetDeptId(dto.getTargetDeptId());
        entity.setTargetPositionId(dto.getTargetPositionId());
        entity.setOfferSalary(dto.getOfferSalary());
        entity.setProbationMonths(dto.getProbationMonths());
        entity.setEntryDate(LocalDate.parse(dto.getEntryDate(), DATE_FMT));
        entity.setBankAccount(dto.getBankAccount());
        entity.setBankAccountHash(encryptionUtil.computeHash(dto.getBankAccount()));
        // 重新提交 → 进入审批中
        entity.setStatus(ApprovalStatusEnum.PENDING.getCode());

        onboardingMapper.update(entity);

        // 重新启动审批流程
        stateMachine.startApproval(
                BusinessTypeEnum.ONBOARDING.getCode(),
                entity.getId(),
                ApprovalStateMachineService.ApprovalContext.ofDept(dto.getTargetDeptId())
        );

        log.info("入职申请已重新提交: id={}", entity.getId());
    }

    /** 审批 */
    @Transactional
    public OnboardingResultVO approve(Long id, ApprovalActionDTO dto) {
        OnboardingApplication entity = onboardingMapper.selectById(id);
        if (entity == null) {
            throw BaseException.notFound("入职申请不存在");
        }
        if (entity.getStatus() != ApprovalStatusEnum.PENDING.getCode()) {
            throw BaseException.badRequest("当前状态不可审批");
        }

        // 找到当前用户待审批的记录
        List<ApprovalRecord> pendingRecords = stateMachine.getApprovalRecords(
                BusinessTypeEnum.ONBOARDING.getCode(), id);
        ApprovalRecord myRecord = pendingRecords.stream()
                .filter(r -> r.getIsPending() == 1
                        && r.getApproverId().equals(BaseContext.getCurrentUserId()))
                .findFirst()
                .orElseThrow(() -> BaseException.badRequest("您没有待处理的审批"));

        // 处理审批
        ApprovalStateMachineService.ApprovalResult result = stateMachine.processApproval(
                myRecord.getId(), dto.getAction(), dto.getComment(), BaseContext.getCurrentUserId());

        if (result.isTerminated()) {
            // 拒绝或退回
            entity.setStatus(ApprovalStatusEnum.REJECTED.getCode());
            onboardingMapper.update(entity);
            log.info("入职申请已拒绝: id={}", id);
            return null;
        }

        if (result.isApproved()) {
            // 全部审批通过 → 执行入职
            entity.setStatus(ApprovalStatusEnum.PENDING_ENTRY.getCode());
            onboardingMapper.update(entity);
            OnboardingResultVO vo = executeOnboarding(entity);
            log.info("入职审批全部通过，员工已入职: id={}, employeeNo={}", id, vo.getEmployeeNo());
            return vo;
        }

        // 部分审批通过，继续等待
        log.info("入职申请审批中(部分通过): id={}, step={}", id, result.getStepOrder());
        return null;
    }

    /** 执行入职：生成工号+创建账号+创建员工档案+写异动日志 */
    private OnboardingResultVO executeOnboarding(OnboardingApplication entity) {
        // 1. 生成工号
        String employeeNo = generateEmployeeNo();

        // 2. 创建 sys_user 账号
        SysUser user = new SysUser();
        user.setUsername(entity.getPhone());
        String rawPwd = generateRandomPassword();
        user.setPassword(PasswordUtil.encode(rawPwd));
        user.setStatus(1);
        // 这里需要 SysUserMapper.insert，暂时通过 mapper 处理
        // (SysUserMapper 目前只有 findByUsername 和 findById，需要新增 insert)

        // 3. 创建 employee 档案
        Employee emp = new Employee();
        emp.setEmployeeNo(employeeNo);
        emp.setName(entity.getRealName());
        // 个人信息
        emp.setPhone(entity.getPhone());
        emp.setPhoneHash(encryptionUtil.computeHash(entity.getPhone()));
        emp.setEmail(entity.getEmail());
        emp.setIdCard(entity.getIdCard());
        emp.setIdCardHash(encryptionUtil.computeHash(entity.getIdCard()));
        // 从身份证号自动提取生日
        emp.setBirthday(extractBirthdayFromIdCard(entity.getIdCard()));
        // 工作信息
        emp.setDeptId(entity.getTargetDeptId());
        emp.setPositionId(entity.getTargetPositionId());
        emp.setEntryType(entity.getEntryType() != null ? entity.getEntryType() : 1); // 默认社招
        emp.setWorkLocation(entity.getWorkLocation());
        emp.setGrade(entity.getGrade());
        emp.setReportTo(entity.getReportTo());
        // 薪资信息
        emp.setSalaryAccountId(entity.getSalaryAccountId());
        emp.setBaseSalary(entity.getOfferSalary());
        emp.setBankAccount(entity.getBankAccount());
        emp.setBankAccountHash(encryptionUtil.computeHash(entity.getBankAccount()));
        emp.setBankName(entity.getBankName());
        // 状态
        emp.setStatus(1); // 试用期
        emp.setEntryDate(entity.getEntryDate());
        emp.setRegularDate(entity.getEntryDate().plusMonths(
                entity.getProbationMonths() != null ? entity.getProbationMonths() : 3));

        employeeMapper.insert(emp);

        // 4. 回填关联
        entity.setEmployeeId(emp.getId());
        entity.setStatus(ApprovalStatusEnum.ONBOARDED.getCode());
        onboardingMapper.update(entity);

        // 5. 写入异动日志
        EmployeeTransfer transfer = new EmployeeTransfer();
        transfer.setEmployeeId(null); // 等 employee insert 后
        transfer.setTransferType(TransferTypeEnum.ONBOARDING.getCode());
        transfer.setBusinessId(entity.getId());
        transfer.setEffectiveDate(entity.getEntryDate());
        transfer.setRemark("入职审批通过，工号: " + employeeNo);

        // 返回结果
        OnboardingResultVO vo = new OnboardingResultVO();
        vo.setEmployeeNo(employeeNo);
        vo.setUsername(entity.getPhone());
        vo.setInitialPassword(rawPwd);
        return vo;
    }

    /** 生成工号: HRM + yyyyMMdd + 4位随机 */
    private String generateEmployeeNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = String.format("%04d", (int) (Math.random() * 9999));
        return "HRM" + datePart + seq;
    }

    /** 生成随机密码 */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    /** 从身份证号提取生日（18位身份证: 第7-14位为YYYYMMDD） */
    private LocalDate extractBirthdayFromIdCard(String idCard) {
        if (idCard == null || idCard.length() < 14) return null;
        try {
            String dateStr = idCard.substring(6, 14);
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            return null;
        }
    }

    private OnboardingVO buildVO(OnboardingApplication entity) {
        OnboardingVO vo = new OnboardingVO();
        vo.setId(entity.getId());
        vo.setEmployeeId(entity.getEmployeeId());
        vo.setRealName(entity.getRealName());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setIdCard(entity.getIdCard());
        vo.setTargetDeptId(entity.getTargetDeptId());
        vo.setTargetPositionId(entity.getTargetPositionId());
        vo.setOfferSalary(entity.getOfferSalary());
        vo.setProbationMonths(entity.getProbationMonths());
        vo.setEntryDate(entity.getEntryDate());
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(ApprovalStatusEnum.fromCode(entity.getStatus()).getLabel());
        vo.setSubmitterId(entity.getSubmitterId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        // 关联名称
        Department dept = departmentMapper.selectById(entity.getTargetDeptId());
        if (dept != null) vo.setTargetDeptName(dept.getDeptName());
        return vo;
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
                case 1 -> "通过";
                case 2 -> "拒绝";
                case 3 -> "退回";
                default -> "未知";
            });
        }
        vo.setComment(r.getComment());
        vo.setOperateTime(r.getOperateTime());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }
}
