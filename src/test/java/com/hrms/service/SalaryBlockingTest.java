package com.hrms.service;

import com.hrms.common.constant.SalaryWarningConstants;
import com.hrms.dto.SalaryCalcDTO;
import com.hrms.vo.SalaryCalcResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 薪资红色阻断检测测试 (任务 8.6)
 */
@DisplayName("SalaryCalculationService 阻断检测测试")
class SalaryBlockingTest {

    private final SalaryCalculationService service = new SalaryCalculationService();

    // ── 8.3 阻断检测 ──

    @Test
    @DisplayName("8.3 实发工资<=0 → 红色阻断")
    void shouldBlockWhenNetPayZeroOrNegative() {
        SalaryCalcDTO input = SalaryCalcDTO.builder()
                .employeeId(1L).year(2026).month(7)
                .basicSalary(BigDecimal.valueOf(5000))
                .positionSalary(BigDecimal.ZERO)
                .build();

        // 实发工资=0
        List<String> blockings = service.detectBlockings(input, BigDecimal.ZERO, BigDecimal.ZERO);
        assertNotNull(blockings);
        assertTrue(blockings.contains(SalaryWarningConstants.BLOCK_NET_PAY_ZERO_OR_NEGATIVE));

        // 实发工资<0
        blockings = service.detectBlockings(input, BigDecimal.ZERO, BigDecimal.valueOf(-100));
        assertTrue(blockings.contains(SalaryWarningConstants.BLOCK_NET_PAY_ZERO_OR_NEGATIVE));
    }

    @Test
    @DisplayName("8.3 应发工资为0 → 红色阻断")
    void shouldBlockWhenGrossPayZero() {
        SalaryCalcDTO input = SalaryCalcDTO.builder()
                .employeeId(1L).year(2026).month(7)
                .basicSalary(BigDecimal.ZERO)
                .positionSalary(BigDecimal.ZERO)
                .build();

        List<String> blockings = service.detectBlockings(input, BigDecimal.ZERO, BigDecimal.ZERO);
        assertTrue(blockings.contains(SalaryWarningConstants.BLOCK_GROSS_PAY_ZERO));
    }

    @Test
    @DisplayName("8.3 基本工资为空 → 红色阻断")
    void shouldBlockWhenBasicSalaryMissing() {
        SalaryCalcDTO input = SalaryCalcDTO.builder()
                .employeeId(1L).year(2026).month(7)
                .basicSalary(null)
                .build();

        List<String> blockings = service.detectBlockings(input, BigDecimal.valueOf(5000), BigDecimal.valueOf(4000));
        assertTrue(blockings.contains(SalaryWarningConstants.BLOCK_SALARY_ACCOUNT_MISSING));
    }

    @Test
    @DisplayName("8.3 正常薪资无阻断")
    void shouldHaveNoBlockingsForNormalSalary() {
        SalaryCalcDTO input = SalaryCalcDTO.builder()
                .employeeId(1L).year(2026).month(7)
                .basicSalary(BigDecimal.valueOf(10000))
                .positionSalary(BigDecimal.valueOf(2000))
                .build();

        List<String> blockings = service.detectBlockings(
                input, BigDecimal.valueOf(12000), BigDecimal.valueOf(8500));
        assertNotNull(blockings);
        assertTrue(blockings.isEmpty(), "正常薪资不应有阻断");
    }

    @Test
    @DisplayName("8.3 hasBlockings/hasWarnings 判断")
    void shouldCorrectlyCheckBlockingsAndWarnings() {
        SalaryCalcResultVO withBlockings = SalaryCalcResultVO.builder()
                .blockings(List.of(SalaryWarningConstants.BLOCK_NET_PAY_ZERO_OR_NEGATIVE))
                .build();
        assertTrue(service.hasBlockings(withBlockings));
        assertFalse(service.hasWarnings(withBlockings));

        SalaryCalcResultVO withWarnings = SalaryCalcResultVO.builder()
                .warnings(List.of(SalaryWarningConstants.WARN_LEAVE_OVER_15))
                .build();
        assertFalse(service.hasBlockings(withWarnings));
        assertTrue(service.hasWarnings(withWarnings));

        SalaryCalcResultVO clean = SalaryCalcResultVO.builder().build();
        assertFalse(service.hasBlockings(clean));
        assertFalse(service.hasWarnings(clean));
    }
}
