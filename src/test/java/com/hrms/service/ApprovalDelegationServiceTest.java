package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.dto.ApprovalDelegationDTO;
import com.hrms.entity.ApprovalDelegation;
import com.hrms.entity.SysUser;
import com.hrms.mapper.ApprovalDelegationMapper;
import com.hrms.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ApprovalDelegationService 委托审批测试（需求 8.3）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApprovalDelegationService 委托审批")
class ApprovalDelegationServiceTest {

    @Mock private ApprovalDelegationMapper delegationMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ApprovalDelegationService delegationService;

    private SysUser delegate;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentUserId(100L);
        BaseContext.setCurrentUsername("主管A");

        delegate = new SysUser();
        delegate.setId(200L);
        delegate.setUsername("同事B");
        when(sysUserMapper.findById(200L)).thenReturn(delegate);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    private ApprovalDelegationDTO dto(Long delegateId, String start, String end) {
        ApprovalDelegationDTO dto = new ApprovalDelegationDTO();
        dto.setDelegateId(delegateId);
        dto.setStartTime(LocalDateTime.parse(start));
        dto.setEndTime(LocalDateTime.parse(end));
        return dto;
    }

    @Test
    @DisplayName("设置委托成功并通知被委托人")
    void create_success() {
        ApprovalDelegationDTO dto = dto(200L, "2026-07-20T00:00:00", "2026-07-25T23:59:59");
        when(delegationMapper.countOverlapping(eq(100L), any(), any())).thenReturn(0);

        ApprovalDelegation result = delegationService.create(dto);

        verify(delegationMapper).insert(any(ApprovalDelegation.class));
        assertEquals(100L, result.getDelegatorId());
        assertEquals(200L, result.getDelegateId());
        verify(notificationService).sendApprovalNotify(eq(200L), contains("委托"), anyString(), isNull(), isNull());
    }

    @Test
    @DisplayName("委托给自己被拒绝")
    void create_self_throws() {
        assertThrows(BaseException.class,
                () -> delegationService.create(dto(100L, "2026-07-20T00:00:00", "2026-07-25T00:00:00")));
        verify(delegationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("被委托人不存在被拒绝")
    void create_delegateNotFound_throws() {
        when(sysUserMapper.findById(999L)).thenReturn(null);
        assertThrows(BaseException.class,
                () -> delegationService.create(dto(999L, "2026-07-20T00:00:00", "2026-07-25T00:00:00")));
    }

    @Test
    @DisplayName("结束时间早于开始时间被拒绝")
    void create_invalidTimeRange_throws() {
        assertThrows(BaseException.class,
                () -> delegationService.create(dto(200L, "2026-07-25T00:00:00", "2026-07-20T00:00:00")));
    }

    @Test
    @DisplayName("时间区间重叠被拒绝")
    void create_overlapping_throws() {
        when(delegationMapper.countOverlapping(eq(100L), any(), any())).thenReturn(1);
        assertThrows(BaseException.class,
                () -> delegationService.create(dto(200L, "2026-07-24T00:00:00", "2026-07-28T00:00:00")));
        verify(delegationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("取消委托成功")
    void cancel_success() {
        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setId(1L);
        delegation.setDelegatorId(100L);
        delegation.setStatus(1);
        when(delegationMapper.selectById(1L)).thenReturn(delegation);

        delegationService.cancel(1L);

        verify(delegationMapper).cancel(1L);
    }

    @Test
    @DisplayName("非本人委托不可取消")
    void cancel_notOwner_forbidden() {
        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setId(1L);
        delegation.setDelegatorId(999L);
        delegation.setStatus(1);
        when(delegationMapper.selectById(1L)).thenReturn(delegation);

        assertThrows(BaseException.class, () -> delegationService.cancel(1L));
        verify(delegationMapper, never()).cancel(anyLong());
    }

    @Test
    @DisplayName("已取消的委托不可重复取消")
    void cancel_alreadyCanceled_throws() {
        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setId(1L);
        delegation.setDelegatorId(100L);
        delegation.setStatus(0);
        when(delegationMapper.selectById(1L)).thenReturn(delegation);

        assertThrows(BaseException.class, () -> delegationService.cancel(1L));
    }
}
