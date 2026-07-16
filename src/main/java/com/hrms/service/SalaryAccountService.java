package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.mapper.SalaryAccountMapper;
import com.hrms.mapper.SalaryChangeHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryAccountService {

    private final SalaryAccountMapper salaryAccountMapper;
    private final SalaryChangeHistoryMapper historyMapper;

    public SalaryAccount getByEmployeeId(Long employeeId) {
        return salaryAccountMapper.selectByEmployeeId(employeeId);
    }

    public List<SalaryAccount> listActive() {
        return salaryAccountMapper.selectAllActive();
    }

    public List<SalaryChangeHistory> listHistory(Long employeeId) {
        return historyMapper.selectByEmployeeId(employeeId);
    }

    @Transactional
    public SalaryAccount create(SalaryAccount account) {
        account.setStatus(1);
        account.setEffectiveStartDate(LocalDate.now());
        account.setOperatorId(BaseContext.getCurrentUserId());
        salaryAccountMapper.insert(account);

        writeHistory(account.getEmployeeId(), account.getId(), "CREATE",
                null, null, account.getChangeReason(), account.getOperatorId());
        log.info("薪资档案创建: employeeId={}, accountId={}", account.getEmployeeId(), account.getId());
        return account;
    }

    @Transactional
    public void adjust(Long accountId, SalaryAccount newAccount) {
        SalaryAccount old = salaryAccountMapper.selectById(accountId);
        if (old == null) throw BaseException.notFound("薪资档案不存在");
        if (old.getStatus() != 1) throw BaseException.badRequest("只能调整生效中的档案");

        // 停用旧档案
        old.setStatus(0);
        old.setEffectiveEndDate(LocalDate.now());
        salaryAccountMapper.update(old);

        // 创建新档案
        newAccount.setEmployeeId(old.getEmployeeId());
        newAccount.setStatus(1);
        newAccount.setEffectiveStartDate(LocalDate.now());
        newAccount.setOperatorId(BaseContext.getCurrentUserId());
        salaryAccountMapper.insert(newAccount);

        writeHistory(old.getEmployeeId(), newAccount.getId(), "ADJUST",
                null, null, newAccount.getChangeReason(), newAccount.getOperatorId());
        log.info("薪资档案调整: employeeId={}, oldAccountId={}, newAccountId={}",
                old.getEmployeeId(), accountId, newAccount.getId());
    }

    @Transactional
    public void deactivate(Long accountId) {
        SalaryAccount account = salaryAccountMapper.selectById(accountId);
        if (account == null) throw BaseException.notFound("薪资档案不存在");
        account.setStatus(0);
        account.setEffectiveEndDate(LocalDate.now());
        account.setOperatorId(BaseContext.getCurrentUserId());
        salaryAccountMapper.update(account);

        writeHistory(account.getEmployeeId(), accountId, "DEACTIVATE",
                null, null, "档案停用", account.getOperatorId());
        log.info("薪资档案停用: employeeId={}, accountId={}", account.getEmployeeId(), accountId);
    }

    private void writeHistory(Long employeeId, Long accountId, String changeType,
                               String oldValue, String newValue, String reason, Long operatorId) {
        SalaryChangeHistory h = new SalaryChangeHistory();
        h.setEmployeeId(employeeId);
        h.setAccountId(accountId);
        h.setChangeType(changeType);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setChangeReason(reason);
        h.setSourceBusiness("MANUAL");
        h.setOperatorId(operatorId);
        historyMapper.insert(h);
    }
}
