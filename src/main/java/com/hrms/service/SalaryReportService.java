package com.hrms.service;

import com.hrms.common.context.BaseContext;
import com.hrms.common.exception.BaseException;
import com.hrms.entity.*;
import com.hrms.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryReportService {

    private final SalaryRecordMapper salaryRecordMapper;
    private final SalaryBatchMapper salaryBatchMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    /** 近6个月应发/实发趋势 */
    public List<Map<String, Object>> monthlyTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            List<SalaryRecord> records = salaryRecordMapper.selectByDeptAndMonth(null, ym.getYear(), ym.getMonthValue());
            BigDecimal grossTotal = BigDecimal.ZERO;
            BigDecimal netTotal = BigDecimal.ZERO;
            if (records != null) {
                for (SalaryRecord r : records) {
                    if (r.getGrossPay() != null) grossTotal = grossTotal.add(r.getGrossPay());
                    if (r.getNetPay() != null) netTotal = netTotal.add(r.getNetPay());
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("yearMonth", String.format("%d-%02d", ym.getYear(), ym.getMonthValue()));
            m.put("grossTotal", grossTotal);
            m.put("netTotal", netTotal);
            result.add(m);
        }
        return result;
    }

    /** 部门薪资成本分布 */
    public List<Map<String, Object>> deptCostDistribution(int year, int month) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Employee> allEmps = employeeMapper.selectAll();
        Map<Long, List<Employee>> deptEmpMap = allEmps.stream()
                .filter(e -> e.getDeptId() != null)
                .collect(Collectors.groupingBy(Employee::getDeptId));

        for (Map.Entry<Long, List<Employee>> entry : deptEmpMap.entrySet()) {
            Long deptId = entry.getKey();
            BigDecimal totalNet = BigDecimal.ZERO;
            for (Employee emp : entry.getValue()) {
                SalaryRecord record = salaryRecordMapper.selectByEmployeeAndMonth(emp.getId(), year, month);
                if (record != null && record.getNetPay() != null) {
                    totalNet = totalNet.add(record.getNetPay());
                }
            }
            Department dept = departmentMapper.selectById(deptId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deptId", deptId);
            m.put("deptName", dept != null ? dept.getDeptName() : "未知");
            m.put("totalNetPay", totalNet);
            m.put("employeeCount", entry.getValue().size());
            result.add(m);
        }
        return result;
    }

    /** 当月薪资构成占比 */
    public Map<String, BigDecimal> compositionPct(int year, int month) {
        List<SalaryRecord> records = salaryRecordMapper.selectByDeptAndMonth(null, year, month);
        if (records == null || records.isEmpty()) return Map.of();

        BigDecimal totalGross = BigDecimal.ZERO, totalSI = BigDecimal.ZERO;
        BigDecimal totalHF = BigDecimal.ZERO, totalTax = BigDecimal.ZERO, totalNet = BigDecimal.ZERO;
        for (SalaryRecord r : records) {
            if (r.getGrossPay() != null) totalGross = totalGross.add(r.getGrossPay());
            if (r.getSocialInsurancePersonal() != null) totalSI = totalSI.add(r.getSocialInsurancePersonal());
            if (r.getHousingFundPersonal() != null) totalHF = totalHF.add(r.getHousingFundPersonal());
            if (r.getTax() != null) totalTax = totalTax.add(r.getTax());
            if (r.getNetPay() != null) totalNet = totalNet.add(r.getNetPay());
        }

        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("grossPay", totalGross);
        m.put("socialInsurance", totalSI);
        m.put("housingFund", totalHF);
        m.put("tax", totalTax);
        m.put("netPay", totalNet);
        return m;
    }
}
