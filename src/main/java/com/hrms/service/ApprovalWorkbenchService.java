package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.entity.*;
import com.hrms.enums.BusinessTypeEnum;
import com.hrms.mapper.*;
import com.hrms.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批工作台 Service（待办/已办）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkbenchService {

    private final ApprovalRecordMapper recordMapper;
    private final OnboardingApplicationMapper onboardingMapper;
    private final RegularizationApplicationMapper regularizationMapper;
    private final TransferApplicationMapper transferMapper;
    private final ResignationApplicationMapper resignationMapper;

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
            vo.setSummary(buildSummary(r.getBusinessType(), r.getBusinessId()));
            vo.setApplyTime(r.getCreateTime());
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
                        yield "转正申请 - 员工#" + a.getEmployeeId();
                    }
                    yield "转正申请 #" + businessId;
                }
                case 3 -> {
                    TransferApplication a = transferMapper.selectById(businessId);
                    if (a != null) {
                        yield "调岗申请 - 员工#" + a.getEmployeeId();
                    }
                    yield "调岗申请 #" + businessId;
                }
                case 4 -> {
                    ResignationApplication a = resignationMapper.selectById(businessId);
                    if (a != null) {
                        yield "离职申请 - 员工#" + a.getEmployeeId();
                    }
                    yield "离职申请 #" + businessId;
                }
                default -> "审批 #" + businessId;
            };
        } catch (Exception e) {
            return BusinessTypeEnum.fromCode(businessType).getLabel() + " #" + businessId;
        }
    }
}
