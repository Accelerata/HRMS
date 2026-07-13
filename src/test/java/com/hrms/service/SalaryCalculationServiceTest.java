package com.hrms.service;

import com.hrms.common.constant.SalaryWarningConstants;
import com.hrms.dto.SalaryCalcDTO;
import com.hrms.entity.SocialInsuranceConfig;
import com.hrms.vo.SalaryCalcResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 薪资核算核心引擎 — 单元测试
 * TDD RED → GREEN → REFACTOR
 *
 * 业务规则（来自 CLAUDE.md）：
 * - 试用期比例折算
 * - 考勤罚款（迟到/事假/旷工）
 * - 社保公积金扣除（基于基数和比例，含上下限）
 * - 个税累计预扣法
 * - 预警机制（请假>15天、加班>50小时、薪资波动>30%）
 * - 金额精确到分
 */
@DisplayName("SalaryCalculationService 薪资核算核心引擎")
class SalaryCalculationServiceTest {

    private SalaryCalculationService service;

    // 默认社保公积金配置（以上海为例）
    private SocialInsuranceConfig siConfig;

    @BeforeEach
    void setUp() {
        service = new SalaryCalculationService();

        siConfig = new SocialInsuranceConfig();
        siConfig.setPensionPersonalRatio(new BigDecimal("0.08"));
        siConfig.setMedicalPersonalRatio(new BigDecimal("0.02"));
        siConfig.setUnemploymentPersonalRatio(new BigDecimal("0.005"));
        siConfig.setHousingFundPersonalRatio(new BigDecimal("0.07"));
        siConfig.setTaxThreshold(new BigDecimal("5000"));
        // 基数上下限
        siConfig.setSocialInsuranceCeiling(new BigDecimal("31014"));
        siConfig.setSocialInsuranceFloor(new BigDecimal("5975"));
        siConfig.setHousingFundCeiling(new BigDecimal("31014"));
        siConfig.setHousingFundFloor(new BigDecimal("2480"));
    }

    // ══════════════════════════════════════════════════════
    // 辅助方法
    // ══════════════════════════════════════════════════════

    /** 断言两个 BigDecimal 在 scale=2 下相等 */
    private void assertAmountEquals(BigDecimal expected, BigDecimal actual, String message) {
        BigDecimal e = expected.setScale(2, RoundingMode.HALF_UP);
        BigDecimal a = actual.setScale(2, RoundingMode.HALF_UP);
        assertEquals(e, a, message + " (expected=" + e + ", actual=" + a + ")");
    }

    // ══════════════════════════════════════════════════════
    // 场景1：正式员工标准薪资计算（无考勤扣款）
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("标准正式员工 — 全勤无扣款")
    class RegularEmployeeFullAttendance {

        @Test
        @DisplayName("正式员工，基本工资10000+岗位5000，全勤，社保公积金和个税正确扣除")
        void shouldCalculateRegularEmployeeCorrectly() {
            // Given
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(1L)
                    .employeeStatus(2) // 正式
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            // When
            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // Then —— 应发部分
            // 基本工资 = 10000 + 5000 = 15000
            assertAmountEquals(new BigDecimal("15000.00"), result.getBasicSalary(),
                    "应发基本工资");
            assertAmountEquals(BigDecimal.ZERO, result.getAttendanceDeduction(),
                    "考勤扣款应为0");
            assertAmountEquals(BigDecimal.ZERO, result.getLeaveDeduction(),
                    "事假扣款应为0");
            assertAmountEquals(new BigDecimal("15000.00"), result.getGrossPay(),
                    "应发合计");

            // 社保: 15000 × (8%+2%+0.5%) = 15000 × 10.5% = 1575.00
            assertAmountEquals(new BigDecimal("1575.00"), result.getSocialInsurancePersonal(),
                    "社保个人缴纳");

            // 公积金: 15000 × 7% = 1050.00
            assertAmountEquals(new BigDecimal("1050.00"), result.getHousingFundPersonal(),
                    "公积金个人缴纳");

            // 应纳税所得额: 15000 - 1575 - 1050 - 5000 = 7375
            assertAmountEquals(new BigDecimal("7375.00"), result.getTaxableIncome(),
                    "应纳税所得额");

            // 个税(累计预扣法): 7375 ≤ 36000, 税率3%
            // 累计应纳税所得额: 7375
            // 个税 = 7375 × 3% - 0 = 221.25
            assertAmountEquals(new BigDecimal("221.25"), result.getTax(),
                    "个人所得税");

            // 实发: 15000 - 1575 - 1050 - 221.25 = 12153.75
            assertAmountEquals(new BigDecimal("12153.75"), result.getNetPay(),
                    "实发工资");

            // 无预警
            assertTrue(result.getWarnings().isEmpty(), "全勤正式员工应无预警");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景2：试用期员工薪资折算
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("试用期员工 — 薪资折算")
    class ProbationEmployee {

        @Test
        @DisplayName("试用期比例80%，基本工资10000+岗位5000，应发=15000×80%=12000")
        void shouldApplyProbationRatio() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(2L)
                    .employeeStatus(1) // 试用期
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(new BigDecimal("0.80"))
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 应发 = 15000 × 0.80 = 12000
            assertAmountEquals(new BigDecimal("12000.00"), result.getBasicSalary(),
                    "试用期应发基本工资");
            assertAmountEquals(new BigDecimal("12000.00"), result.getGrossPay(),
                    "试用期应发合计");

            // 社保仍基于实际基数15000: 15000 × 10.5% = 1575
            assertAmountEquals(new BigDecimal("1575.00"), result.getSocialInsurancePersonal(),
                    "社保基于实际基数");

            // 公积金: 15000 × 7% = 1050
            assertAmountEquals(new BigDecimal("1050.00"), result.getHousingFundPersonal(),
                    "公积金基于实际基数");

            // 应纳税所得额: 12000 - 1575 - 1050 - 5000 = 4375
            assertAmountEquals(new BigDecimal("4375.00"), result.getTaxableIncome(),
                    "试用期应纳税所得额");

            // 个税: 4375 × 3% = 131.25
            assertAmountEquals(new BigDecimal("131.25"), result.getTax(),
                    "试用期个税");

            // 实发: 12000 - 1575 - 1050 - 131.25 = 9243.75
            assertAmountEquals(new BigDecimal("9243.75"), result.getNetPay(),
                    "试用期实发工资");

            // 试用期比例应记录
            assertAmountEquals(new BigDecimal("0.80"), result.getProbationRatio(),
                    "试用期比例");
        }

        @Test
        @DisplayName("试用期比例100%（特殊约定），应发与正式员工相同")
        void shouldApplyFullSalaryWhenProbationRatioIsOne() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(3L)
                    .employeeStatus(1)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            assertAmountEquals(new BigDecimal("15000.00"), result.getBasicSalary(),
                    "试用期比例100%应发=全额");
            assertAmountEquals(new BigDecimal("12153.75"), result.getNetPay(),
                    "试用期比例100%实发=正式员工");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景3：考勤扣款（迟到/早退/旷工/事假）
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("考勤扣款 — 迟到/早退/旷工/事假")
    class AttendanceDeduction {

        @Test
        @DisplayName("迟到3次×50元+早退1次×50元+事假2天，考勤扣200+事假扣1379.31")
        void shouldDeductLateEarlyAndPersonalLeave() {
            // 事假扣款: (10000+5000)/21.75 × 2 = 1379.31 (四舍五入到分)
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(4L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(3)
                    .earlyCount(1)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(new BigDecimal("2"))
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 考勤扣款: 3×50 + 1×50 = 200
            assertAmountEquals(new BigDecimal("200.00"), result.getAttendanceDeduction(),
                    "迟到3次×50+早退1次×50=200");

            // 事假扣款: 15000/21.75×2 = 1379.31
            assertAmountEquals(new BigDecimal("1379.31"), result.getLeaveDeduction(),
                    "事假扣款");

            // 应发: 15000 - 200 - 1379.31 = 13420.69
            assertAmountEquals(new BigDecimal("13420.69"), result.getGrossPay(),
                    "应发合计（扣减考勤+事假后）");
        }

        @Test
        @DisplayName("旷工1.5天（3个半天），扣日工资×1.5")
        void shouldDeductAbsentDays() {
            // 日工资: 15000/21.75 = 689.655...
            // 旷工1.5天扣款: 689.655... × 1.5 = 1034.48 (四舍五入)
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(5L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0)
                    .earlyCount(0)
                    .absentDays(new BigDecimal("1.5"))
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 考勤扣款(旷工): 15000/21.75 × 1.5 = 1034.48
            assertAmountEquals(new BigDecimal("1034.48"), result.getAttendanceDeduction(),
                    "旷工1.5天扣款");

            // 应发: 15000 - 1034.48 = 13965.52
            assertAmountEquals(new BigDecimal("13965.52"), result.getGrossPay(),
                    "应发合计（扣减旷工后）");
        }

        @Test
        @DisplayName("无任何考勤问题时，考勤扣款和事假扣款均为零")
        void shouldHaveNoDeductionWhenFullAttendance() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(6L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            assertAmountEquals(BigDecimal.ZERO, result.getAttendanceDeduction(),
                    "无迟到早退旷工，考勤扣款=0");
            assertAmountEquals(BigDecimal.ZERO, result.getLeaveDeduction(),
                    "无事假，事假扣款=0");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景4：社保公积金基数上下限
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("社保公积金 — 基数上下限")
    class SocialInsuranceLimits {

        @Test
        @DisplayName("工资低于社保下限5975时，按下限5975计算社保")
        void shouldUseFloorWhenBaseBelowMinimum() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(7L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("4000"))
                    .positionSalary(new BigDecimal("1000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("5000")) // 低于下限5975
                    .housingFundBase(new BigDecimal("2000"))     // 低于下限2480
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 社保按下限5975: 5975 × 10.5% = 627.375 → 627.38
            assertAmountEquals(new BigDecimal("627.38"), result.getSocialInsurancePersonal(),
                    "社保按下限5975计算");
            // 公积金按下限2480: 2480 × 7% = 173.60
            assertAmountEquals(new BigDecimal("173.60"), result.getHousingFundPersonal(),
                    "公积金按下限2480计算");
        }

        @Test
        @DisplayName("工资高于社保上限31014时，按上限31014计算社保")
        void shouldUseCeilingWhenBaseAboveMaximum() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(8L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("35000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("40000")) // 高于上限31014
                    .housingFundBase(new BigDecimal("40000"))     // 高于上限31014
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 社保按上限31014: 31014 × 10.5% = 3256.47
            assertAmountEquals(new BigDecimal("3256.47"), result.getSocialInsurancePersonal(),
                    "社保按上限31014计算");
            // 公积金按上限31014: 31014 × 7% = 2170.98
            assertAmountEquals(new BigDecimal("2170.98"), result.getHousingFundPersonal(),
                    "公积金按上限31014计算");
        }

        @Test
        @DisplayName("工资在上下限之间时，按实际基数计算")
        void shouldUseActualBaseWhenWithinRange() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(9L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("15000"))
                    .positionSalary(new BigDecimal("0"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 社保按15000: 15000 × 10.5% = 1575.00
            assertAmountEquals(new BigDecimal("1575.00"), result.getSocialInsurancePersonal(),
                    "社保按实际基数15000计算");
            // 公积金按15000: 15000 × 7% = 1050.00
            assertAmountEquals(new BigDecimal("1050.00"), result.getHousingFundPersonal(),
                    "公积金按实际基数15000计算");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景5：个税累计预扣法（跨税率区间）
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("个税累计预扣法 — 跨月累计、跨税率区间")
    class CumulativeTax {

        @Test
        @DisplayName("1月：应发30000，累计应纳税所得额20625，税率3%，个税618.75")
        void shouldCalculateTaxBracket1January() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(10L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("25000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("25000"))
                    .housingFundBase(new BigDecimal("25000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 社保: 25000 × 10.5% = 2625
            assertAmountEquals(new BigDecimal("2625.00"), result.getSocialInsurancePersonal(),
                    "1月社保");
            // 公积金: 25000 × 7% = 1750
            assertAmountEquals(new BigDecimal("1750.00"), result.getHousingFundPersonal(),
                    "1月公积金");
            // 应纳税所得额: 30000 - 2625 - 1750 - 5000 = 20625
            assertAmountEquals(new BigDecimal("20625.00"), result.getTaxableIncome(),
                    "1月应纳税所得额");
            // 累计应纳税所得额=20625, 税率3%: 20625 × 3% = 618.75
            assertAmountEquals(new BigDecimal("618.75"), result.getTax(),
                    "1月个税");
            assertEquals(0, new BigDecimal("0.03").compareTo(result.getTaxRate()),
                    "税率应为3%");
        }

        @Test
        @DisplayName("2月：累计应纳税所得额41250，跨入10%税率区间，本月个税986.25")
        void shouldCalculateTaxBracket2February() {
            // 1月已算：累计应发30000, 累计社保2625, 累计公积金1750, 累计已缴个税618.75
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(10L)
                    .employeeStatus(2)
                    .year(2026).month(2)
                    .basicSalary(new BigDecimal("25000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("25000"))
                    .housingFundBase(new BigDecimal("25000"))
                    // 1月累计数据
                    .cumulativeGrossPay(new BigDecimal("30000"))
                    .cumulativeSocialInsurance(new BigDecimal("2625"))
                    .cumulativeHousingFund(new BigDecimal("1750"))
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(new BigDecimal("618.75"))
                    .lastMonthNetPay(new BigDecimal("20631.25")) // 30000-2625-1750-618.75
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 累计收入: 30000+30000=60000
            // 累计减除: 5000×2=10000
            // 累计社保: 2625+2625=5250
            // 累计公积金: 1750+1750=3500
            // 累计应纳税所得额: 60000-5250-3500-10000=41250
            // 41250 > 36000, 税率10%, 速算扣除2520
            // 累计应缴: 41250×10%-2520 = 4125-2520 = 1605
            // 本月应缴: 1605 - 618.75 = 986.25
            assertAmountEquals(new BigDecimal("41250.00"), result.getCumulativeTaxableIncome(),
                    "累计应纳税所得额应=41250");
            assertEquals(0, new BigDecimal("0.10").compareTo(result.getTaxRate()),
                    "跨入10%税率区间");
            assertAmountEquals(new BigDecimal("2520.00"), result.getQuickDeduction(),
                    "速算扣除数应=2520");
            assertAmountEquals(new BigDecimal("986.25"), result.getTax(),
                    "2月个税=986.25");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景6：预警机制
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("预警机制 — 请假/加班/薪资波动")
    class WarningMechanism {

        @Test
        @DisplayName("请假18天(>15天)、加班60小时(>50h)、环比波动超30%，三条预警全部触发")
        void shouldTriggerAllThreeWarnings() {
            // 上月实发12153.75，本月因大量请假实发大幅降低
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(11L)
                    .employeeStatus(2)
                    .year(2026).month(2)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(new BigDecimal("18"))  // > 15 天
                    .overtimeHours(new BigDecimal("60"))       // > 50 小时
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(new BigDecimal("12153.75")) // 上月实发
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            List<String> warnings = result.getWarnings();

            // 请假18天 > 15天
            assertTrue(warnings.contains(SalaryWarningConstants.WARN_LEAVE_OVER_15),
                    "请假>15天应触发 LEAVE_OVER_15_DAYS 预警，实际warnings=" + warnings);

            // 加班60小时 > 50小时
            assertTrue(warnings.contains(SalaryWarningConstants.WARN_OVERTIME_OVER_50H),
                    "加班>50h应触发 OVERTIME_OVER_50H 预警，实际warnings=" + warnings);

            // 实发 vs 上月实发 → 波动 > 30%
            assertTrue(warnings.contains(SalaryWarningConstants.WARN_SALARY_CHANGE_OVER_30PCT),
                    "环比波动>30%应触发 SALARY_CHANGE_OVER_30PCT 预警，实际warnings=" + warnings);

            assertEquals(3, warnings.size(), "应触发3条预警");
        }

        @Test
        @DisplayName("请假10天(<15天)、加班30小时(<50h)、波动10%(<30%)，无预警")
        void shouldNotTriggerAnyWarning() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(12L)
                    .employeeStatus(2)
                    .year(2026).month(2)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(new BigDecimal("10"))  // ≤ 15天
                    .overtimeHours(new BigDecimal("30"))       // ≤ 50h
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(new BigDecimal("6000")) // 与本月接近，波动小
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            assertTrue(result.getWarnings().isEmpty(),
                    "正常范围内不应触发预警，实际warnings=" + result.getWarnings());
        }

        @Test
        @DisplayName("薪资环比波动正好30%时触发预警（边界值测试）")
        void shouldTriggerWarningAtExactly30PercentChange() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(13L)
                    .employeeStatus(2)
                    .year(2026).month(2)
                    .basicSalary(new BigDecimal("5000"))
                    .positionSalary(new BigDecimal("0"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("5000"))
                    .housingFundBase(new BigDecimal("5000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(new BigDecimal("10000")) // 上月10000, 本月很低
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 本月实发: 5000 - 5000*10.5% - 5000*7% = 5000 - 525 - 350 = 4125
            // 个税: 应纳税所得额 = 5000 - 525 - 350 - 5000 = -875, 所以税=0
            // 实发: 4125
            // 波动: (10000-4125)/10000 = 58.75% > 30%
            assertTrue(result.getWarnings().contains(SalaryWarningConstants.WARN_SALARY_CHANGE_OVER_30PCT),
                    "波动超过30%应触发预警");
        }

        @Test
        @DisplayName("首次发薪（无上月数据），不触发波动预警")
        void shouldNotTriggerSalaryChangeWarningForFirstPayroll() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(14L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("10000"))
                    .positionSalary(new BigDecimal("5000"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("15000"))
                    .housingFundBase(new BigDecimal("15000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null) // 无上月数据
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            assertFalse(result.getWarnings().contains(SalaryWarningConstants.WARN_SALARY_CHANGE_OVER_30PCT),
                    "首次发薪不应触发薪资波动预警");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景7：金额精度（精确到分）
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("金额精度 — 精确到分")
    class AmountPrecision {

        @Test
        @DisplayName("所有金额结果应精确到分（2位小数，四舍五入）")
        void shouldRoundAllAmountsToCents() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(15L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("8888.88"))
                    .positionSalary(new BigDecimal("1111.11"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(1)
                    .earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(new BigDecimal("0.5"))
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("10000"))
                    .housingFundBase(new BigDecimal("10000"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 验证所有金额的 scale 都不超过 2
            assertTrue(result.getBasicSalary().scale() <= 2,
                    "应发基本工资scale应≤2, 实际=" + result.getBasicSalary().scale());
            assertTrue(result.getAttendanceDeduction().scale() <= 2,
                    "考勤扣款scale应≤2");
            assertTrue(result.getLeaveDeduction().scale() <= 2,
                    "事假扣款scale应≤2");
            assertTrue(result.getGrossPay().scale() <= 2,
                    "应发合计scale应≤2");
            assertTrue(result.getSocialInsurancePersonal().scale() <= 2,
                    "社保scale应≤2");
            assertTrue(result.getHousingFundPersonal().scale() <= 2,
                    "公积金scale应≤2");
            assertTrue(result.getTax().scale() <= 2,
                    "个税scale应≤2");
            assertTrue(result.getNetPay().scale() <= 2,
                    "实发scale应≤2");
        }
    }

    // ══════════════════════════════════════════════════════
    // 场景8：极端边界值
    // ══════════════════════════════════════════════════════

    @Nested
    @DisplayName("极端边界值")
    class EdgeCases {

        @Test
        @DisplayName("应纳税所得额≤0时，个税应为0")
        void shouldReturnZeroTaxWhenTaxableIncomeIsZeroOrNegative() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(16L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("4000"))
                    .positionSalary(new BigDecimal("0"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(0).earlyCount(0)
                    .absentDays(BigDecimal.ZERO)
                    .personalLeaveDays(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("5975"))
                    .housingFundBase(new BigDecimal("2480"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 4000 - 627.38(社保) - 173.60(公积金) - 5000 = -1800.98 ≤ 0
            assertAmountEquals(BigDecimal.ZERO, result.getTax(),
                    "应纳税所得额≤0时个税应为0");
            // 个税率和速算扣除也应为0
            assertAmountEquals(BigDecimal.ZERO, result.getTaxRate(),
                    "无需纳税时税率应为0");
        }

        @Test
        @DisplayName("考勤扣款超过应发工资时，应发合计不低于0")
        void shouldNotAllowNegativeGrossPay() {
            SalaryCalcDTO input = SalaryCalcDTO.builder()
                    .employeeId(17L)
                    .employeeStatus(2)
                    .year(2026).month(1)
                    .basicSalary(new BigDecimal("1000"))
                    .positionSalary(new BigDecimal("0"))
                    .probationRatio(BigDecimal.ONE)
                    .lateCount(100) // 大量迟到
                    .earlyCount(100)
                    .absentDays(new BigDecimal("30"))
                    .personalLeaveDays(new BigDecimal("30"))
                    .overtimeHours(BigDecimal.ZERO)
                    .socialInsuranceBase(new BigDecimal("5975"))
                    .housingFundBase(new BigDecimal("2480"))
                    .cumulativeGrossPay(BigDecimal.ZERO)
                    .cumulativeSocialInsurance(BigDecimal.ZERO)
                    .cumulativeHousingFund(BigDecimal.ZERO)
                    .cumulativeSpecialDeduction(BigDecimal.ZERO)
                    .cumulativeTaxPaid(BigDecimal.ZERO)
                    .lastMonthNetPay(null)
                    .build();

            SalaryCalcResultVO result = service.calculate(input, siConfig);

            // 应发合计不应为负
            assertTrue(result.getGrossPay().compareTo(BigDecimal.ZERO) >= 0,
                    "应发合计不应为负数，实际=" + result.getGrossPay());
        }
    }
}
