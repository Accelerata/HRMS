package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.mapper.SalaryPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryPlanService {

    private final SalaryPlanMapper salaryPlanMapper;

    public List<SalaryPlan> listAll() {
        return salaryPlanMapper.selectAll();
    }

    public List<SalaryPlan> listEnabled() {
        return salaryPlanMapper.selectEnabled();
    }

    public SalaryPlan getById(Long id) {
        SalaryPlan plan = salaryPlanMapper.selectById(id);
        if (plan == null) throw BaseException.notFound("账套不存在");
        return plan;
    }

    @Transactional
    public SalaryPlan create(SalaryPlan plan) {
        plan.setStatus(1);
        salaryPlanMapper.insert(plan);
        log.info("薪资账套创建: id={}, name={}", plan.getId(), plan.getPlanName());
        return plan;
    }

    @Transactional
    public void update(SalaryPlan plan) {
        salaryPlanMapper.update(plan);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        salaryPlanMapper.updateStatus(id, status);
    }

    // ── 工资项目 ──

    public List<SalaryPlanItem> listItems(Long planId) {
        return salaryPlanMapper.selectItemsByPlanId(planId);
    }

    @Transactional
    public void addItem(SalaryPlanItem item) {
        salaryPlanMapper.insertItem(item);
    }

    @Transactional
    public void updateItem(SalaryPlanItem item) {
        salaryPlanMapper.updateItem(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        salaryPlanMapper.deleteItem(id);
    }

    // ── 适用范围 ──

    public List<SalaryPlanScope> listScopes(Long planId) {
        return salaryPlanMapper.selectScopesByPlanId(planId);
    }

    @Transactional
    public void addScope(SalaryPlanScope scope) {
        salaryPlanMapper.insertScope(scope);
    }

    @Transactional
    public void deleteScope(Long id) {
        salaryPlanMapper.deleteScope(id);
    }

    /** 按员工信息匹配有效账套 */
    public SalaryPlan matchPlan(Long deptId, Long positionId, String grade) {
        return salaryPlanMapper.matchPlan(deptId, positionId, grade);
    }
}
