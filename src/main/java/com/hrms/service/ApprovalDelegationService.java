package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalDelegationDTO;
import com.hrms.entity.ApprovalDelegation;
import com.hrms.entity.SysUser;
import com.hrms.mapper.ApprovalDelegationMapper;
import com.hrms.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 审批委托服务（需求 8.3）
 *
 * 规则：
 * 1. 审批人可设置委托（被委托人 + 起止时间），同一委托人不允许时间区间重叠的生效委托
 * 2. 委托生效期间新产生的审批任务自动改派被委托人（引擎 startApproval 钩子）
 * 3. 委托人可随时取消；取消后新任务恢复分配给本人
 * 4. 被委托人审批改派任务时系统留痕「XXX 代 YYY 审批」（引擎 processApproval）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalDelegationService {

    private final ApprovalDelegationMapper delegationMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    /**
     * 设置委托
     */
    @Transactional
    public ApprovalDelegation create(ApprovalDelegationDTO dto) {
        Long delegatorId = BaseContext.getCurrentUserId();

        if (dto.getDelegateId().equals(delegatorId)) {
            throw BaseException.badRequest("不能委托给自己");
        }
        SysUser delegate = sysUserMapper.findById(dto.getDelegateId());
        if (delegate == null) {
            throw BaseException.notFound("被委托人不存在");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw BaseException.badRequest("委托结束时间必须晚于开始时间");
        }
        int overlapping = delegationMapper.countOverlapping(delegatorId, dto.getStartTime(), dto.getEndTime());
        if (overlapping > 0) {
            throw BaseException.badRequest("已存在时间区间重叠的生效委托，请先取消原委托");
        }

        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setDelegatorId(delegatorId);
        delegation.setDelegatorName(BaseContext.getCurrentUsername());
        delegation.setDelegateId(delegate.getId());
        delegation.setDelegateName(delegate.getUsername());
        delegation.setStartTime(dto.getStartTime());
        delegation.setEndTime(dto.getEndTime());
        delegationMapper.insert(delegation);

        // 通知被委托人
        notificationService.sendApprovalNotify(delegate.getId(), "审批委托通知",
                delegation.getDelegatorName() + " 已将 " + dto.getStartTime().toLocalDate()
                        + " 至 " + dto.getEndTime().toLocalDate() + " 期间的审批任务委托给您处理。",
                null, null);

        log.info("审批委托已创建: id={}, delegator={}, delegate={}, {}~{}",
                delegation.getId(), delegation.getDelegatorName(), delegate.getUsername(),
                dto.getStartTime(), dto.getEndTime());
        return delegation;
    }

    /**
     * 取消委托（仅委托人本人）
     */
    @Transactional
    public void cancel(Long id) {
        ApprovalDelegation delegation = delegationMapper.selectById(id);
        if (delegation == null) {
            throw BaseException.notFound("委托记录不存在");
        }
        if (!delegation.getDelegatorId().equals(BaseContext.getCurrentUserId())) {
            throw BaseException.forbidden("只能取消本人的委托");
        }
        if (delegation.getStatus() != 1) {
            throw BaseException.badRequest("该委托已取消");
        }
        delegationMapper.cancel(id);
        log.info("审批委托已取消: id={}, delegator={}", id, delegation.getDelegatorName());
    }

    /**
     * 查询我的委托列表
     */
    public List<ApprovalDelegation> myDelegations() {
        return delegationMapper.selectByDelegator(BaseContext.getCurrentUserId());
    }
}
