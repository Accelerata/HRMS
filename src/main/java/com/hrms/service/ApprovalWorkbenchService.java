package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalTransferDTO;
import com.hrms.entity.*;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批工作台 Service（待办/已办/详情/转交）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkbenchService {

    private final ApprovalRecordMapper recordMapper;
    private final ApprovalTemplateMapper templateMapper;
    private final OnboardingApplicationMapper onboardingMapper;
    private final RegularizationApplicationMapper regularizationMapper;
    private final TransferApplicationMapper transferMapper;
    private final ResignationApplicationMapper resignationMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final SupplementaryCardMapper supplementaryCardMapper;
    private final SalaryBatchMapper salaryBatchMapper;
    private final EmployeeMapper employeeMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    /** 我的待办 */
    public List<ApprovalTodoVO> getTodoList() {
        Long userId = BaseContext.getCurrentUserId();
        List<ApprovalRecord> records = recordMapper.selectTodoByApprover(userId);
        List<ApprovalTodoVO> result = new ArrayList<>();

        for (ApprovalRecord r : records) {
            ApprovalTodoVO vo = new ApprovalTodoVO();
            vo.setApprovalId(r.getId());
            vo.setBusinessType(r.getBusinessType());
            vo.setBusinessTypeLabel(BusinessTypeEnum.fromCode(r.getBusinessType()).getLabel());
            vo.setBusinessId(r.getBusinessId());
            vo.setStepOrder(r.getStepOrder());
            vo.setStepName(resolveStepName(r.getBusinessType(), r.getStepOrder()));
            vo.setSummary(buildSummary(r.getBusinessType(), r.getBusinessId()));
            vo.setApplicantName(resolveApplicantName(r.getBusinessType(), r.getBusinessId()));
            vo.setApplyTime(r.getCreateTime());
            vo.setDueTime(r.getDueTime());
            result.add(vo);
        }
        return result;
    }

    /** 我的已办 */
    public List<ApprovalDoneVO> getDoneList() {
        Long userId = BaseContext.getCurrentUserId();
        List<ApprovalRecord> records = recordMapper.selectDoneByApprover(userId);
        List<ApprovalDoneVO> result = new ArrayList<>();

        for (ApprovalRecord r : records) {
            ApprovalDoneVO vo = new ApprovalDoneVO();
            vo.setApprovalId(r.getId());
            vo.setBusinessType(r.getBusinessType());
            vo.setBusinessTypeLabel(BusinessTypeEnum.fromCode(r.getBusinessType()).getLabel());
            vo.setBusinessId(r.getBusinessId());
            vo.setSummary(buildSummary(r.getBusinessType(), r.getBusinessId()));
            vo.setApplicantName(resolveApplicantName(r.getBusinessType(), r.getBusinessId()));
            vo.setAction(r.getAction());
            vo.setActionLabel(switch (r.getAction()) {
                case 1 -> "通过";
                case 2 -> "拒绝";
                case 3 -> "退回";
                default -> "未知";
            });
            vo.setComment(r.getComment());
            vo.setOperateTime(r.getOperateTime());
            result.add(vo);
        }
        return result;
    }

    /**
     * 统一审批详情（8.2.2）：业务数据 + 审批历史 + 当前用户可操作项
     */
    public ApprovalDetailVO getDetail(int businessType, long businessId) {
        ApprovalDetailVO vo = new ApprovalDetailVO();
        vo.setBusinessType(businessType);
        vo.setBusinessTypeLabel(BusinessTypeEnum.fromCode(businessType).getLabel());
        vo.setBusinessId(businessId);
        vo.setSummary(buildSummary(businessType, businessId));
        vo.setApplicantName(resolveApplicantName(businessType, businessId));

        // 业务单据数据（按类型返回完整字段）
        vo.setBusinessData(buildBusinessData(businessType, businessId));

        // 审批历史（含代审留痕、截止时间）
        List<ApprovalRecord> records = recordMapper.selectByBusiness(businessType, businessId);
        List<ApprovalDetailVO.HistoryNode> history = new ArrayList<>();
        Long currentUserId = BaseContext.getCurrentUserId();
        for (ApprovalRecord r : records) {
            ApprovalDetailVO.HistoryNode node = new ApprovalDetailVO.HistoryNode();
            node.setRecordId(r.getId());
            node.setStepOrder(r.getStepOrder());
            node.setStepName(resolveStepName(r.getBusinessType(), r.getStepOrder()));
            node.setApproverName(r.getApproverName());
            node.setOriginalApproverName(r.getOriginalApproverName());
            node.setAssignType(r.getAssignType());
            node.setAction(r.getAction());
            node.setActionLabel(r.getAction() == null ? "待审批" : switch (r.getAction()) {
                case 1 -> "通过";
                case 2 -> "拒绝";
                case 3 -> "退回";
                default -> "未知";
            });
            node.setComment(r.getComment());
            node.setIsPending(r.getIsPending());
            node.setOperateTime(r.getOperateTime());
            node.setDueTime(r.getDueTime());
            history.add(node);

            // 当前用户可执行的审批记录
            if (r.getIsPending() == 1 && r.getApproverId().equals(currentUserId)) {
                vo.setMyRecordId(r.getId());
                vo.setCanOperate(true);
            }
        }
        vo.setHistory(history);
        return vo;
    }

    /**
     * 转交审批任务（8.2.1 操作）
     */
    @Transactional
    public void transfer(Long recordId, ApprovalTransferDTO dto) {
        ApprovalRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw BaseException.notFound("审批记录不存在");
        }
        if (record.getIsPending() != 1) {
            throw BaseException.badRequest("该审批步骤已处理，无法转交");
        }
        Long currentUserId = BaseContext.getCurrentUserId();
        if (!record.getApproverId().equals(currentUserId)) {
            throw BaseException.forbidden("只能转交本人的审批任务");
        }
        if (dto.getTargetUserId().equals(currentUserId)) {
            throw BaseException.badRequest("不能转交给自己");
        }
        SysUser target = sysUserMapper.findById(dto.getTargetUserId());
        if (target == null) {
            throw BaseException.notFound("转交目标用户不存在");
        }

        int updated = recordMapper.reassign(recordId, target.getId(), target.getUsername(),
                record.getApproverId(), record.getApproverName(), 1);
        if (updated == 0) {
            throw BaseException.badRequest("转交失败，该审批步骤已处理");
        }

        // 通知新审批人
        String typeLabel = BusinessTypeEnum.fromCode(record.getBusinessType()).getLabel();
        notificationService.sendApprovalNotify(target.getId(), "审批转交: " + typeLabel,
                record.getApproverName() + " 将一项" + typeLabel + "审批转交给您处理"
                        + (dto.getComment() != null && !dto.getComment().isBlank()
                            ? "，说明：" + dto.getComment() : "。"),
                record.getBusinessType(), record.getBusinessId());

        log.info("审批任务已转交: recordId={}, {} → {}", recordId, record.getApproverName(), target.getUsername());
    }

    // ═══════════════ 私有辅助 ═══════════════

    /** 步骤名称：从审批模板解析（stepOrder 匹配第一条） */
    private String resolveStepName(int businessType, int stepOrder) {
        try {
            List<ApprovalTemplate> templates = templateMapper.selectByBusinessType(businessType);
            return templates.stream()
                    .filter(t -> t.getStepOrder() == stepOrder)
                    .map(ApprovalTemplate::getStepName)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse("第" + stepOrder + "级审批");
        } catch (Exception e) {
            return "第" + stepOrder + "级审批";
        }
    }

    /** 申请人姓名：按业务类型解析 */
    private String resolveApplicantName(int businessType, long businessId) {
        try {
            return switch (businessType) {
                case 1 -> {
                    OnboardingApplication a = onboardingMapper.selectById(businessId);
                    yield a != null ? a.getRealName() : null;
                }
                case 2 -> {
                    RegularizationApplication a = regularizationMapper.selectById(businessId);
                    yield a != null ? employeeName(a.getEmployeeId()) : null;
                }
                case 3 -> {
                    TransferApplication a = transferMapper.selectById(businessId);
                    yield a != null ? employeeName(a.getEmployeeId()) : null;
                }
                case 4 -> {
                    ResignationApplication a = resignationMapper.selectById(businessId);
                    yield a != null ? employeeName(a.getEmployeeId()) : null;
                }
                case 5 -> {
                    SalaryBatch b = salaryBatchMapper.selectById(businessId);
                    yield b != null && b.getSubmitterId() != null ? userName(b.getSubmitterId()) : null;
                }
                case 6 -> {
                    LeaveApplication a = leaveApplicationMapper.selectById(businessId);
                    yield a != null ? employeeName(a.getEmployeeId()) : null;
                }
                case 7 -> {
                    SupplementaryCardApplication a = supplementaryCardMapper.selectById(businessId);
                    yield a != null ? employeeName(a.getEmployeeId()) : null;
                }
                default -> null;
            };
        } catch (Exception e) {
            log.warn("申请人解析失败: businessType={}, businessId={}, {}", businessType, businessId, e.getMessage());
            return null;
        }
    }

    /** 业务单据数据（按类型返回完整字段，供详情页展示） */
    private Object buildBusinessData(int businessType, long businessId) {
        try {
            return switch (businessType) {
                case 1 -> onboardingMapper.selectById(businessId);
                case 2 -> regularizationMapper.selectById(businessId);
                case 3 -> transferMapper.selectById(businessId);
                case 4 -> resignationMapper.selectById(businessId);
                case 5 -> salaryBatchMapper.selectById(businessId);
                case 6 -> leaveApplicationMapper.selectById(businessId);
                case 7 -> supplementaryCardMapper.selectById(businessId);
                default -> null;
            };
        } catch (Exception e) {
            log.warn("业务数据查询失败: businessType={}, businessId={}, {}", businessType, businessId, e.getMessage());
            return null;
        }
    }

    private String employeeName(Long employeeId) {
        if (employeeId == null) return null;
        Employee employee = employeeMapper.selectById(employeeId);
        return employee != null ? employee.getName() : ("员工#" + employeeId);
    }

    private String userName(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        return user != null ? user.getUsername() : ("用户#" + userId);
    }

    /** 根据业务类型和ID构建摘要信息 */
    private String buildSummary(int businessType, long businessId) {
        try {
            return switch (businessType) {
                case 1 -> {
                    OnboardingApplication a = onboardingMapper.selectById(businessId);
                    yield a != null ? "入职申请 - " + a.getRealName() : "入职申请 #" + businessId;
                }
                case 2 -> {
                    RegularizationApplication a = regularizationMapper.selectById(businessId);
                    if (a != null) {
                        yield "转正申请 - " + employeeName(a.getEmployeeId());
                    }
                    yield "转正申请 #" + businessId;
                }
                case 3 -> {
                    TransferApplication a = transferMapper.selectById(businessId);
                    if (a != null) {
                        yield "调岗申请 - " + employeeName(a.getEmployeeId());
                    }
                    yield "调岗申请 #" + businessId;
                }
                case 4 -> {
                    ResignationApplication a = resignationMapper.selectById(businessId);
                    if (a != null) {
                        yield "离职申请 - " + employeeName(a.getEmployeeId());
                    }
                    yield "离职申请 #" + businessId;
                }
                case 5 -> {
                    SalaryBatch b = salaryBatchMapper.selectById(businessId);
                    if (b != null) {
                        yield "薪资批次 - " + b.getYear() + "年" + b.getMonth() + "月（"
                                + (b.getEmployeeCount() != null ? b.getEmployeeCount() : 0) + "人）";
                    }
                    yield "薪资批次 #" + businessId;
                }
                case 6 -> {
                    LeaveApplication a = leaveApplicationMapper.selectById(businessId);
                    if (a != null) {
                        yield "请假申请 - " + employeeName(a.getEmployeeId()) + "（"
                                + leaveTypeLabel(a.getLeaveType()) + " " + a.getDays() + "天）";
                    }
                    yield "请假申请 #" + businessId;
                }
                case 7 -> {
                    SupplementaryCardApplication a = supplementaryCardMapper.selectById(businessId);
                    if (a != null) {
                        yield "补卡申请 - " + employeeName(a.getEmployeeId()) + "（" + a.getAttendanceDate()
                                + (a.getCardType() == 1 ? " 上班卡）" : " 下班卡）");
                    }
                    yield "补卡申请 #" + businessId;
                }
                default -> "审批 #" + businessId;
            };
        } catch (Exception e) {
            return BusinessTypeEnum.fromCode(businessType).getLabel() + " #" + businessId;
        }
    }

    private String leaveTypeLabel(Integer leaveType) {
        if (leaveType == null) return "";
        return switch (leaveType) {
            case 1 -> "年假";
            case 2 -> "调休";
            case 3 -> "事假";
            case 4 -> "病假";
            case 5 -> "婚假";
            case 6 -> "产假";
            case 7 -> "丧假";
            default -> "假期";
        };
    }
}
