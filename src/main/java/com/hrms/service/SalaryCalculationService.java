package com.hrms.service;

import com.hrms.common.constant.SalaryWarningConstants;
import com.hrms.dto.SalaryCalcDTO;
import com.hrms.entity.SocialInsuranceConfig;
import com.hrms.vo.SalaryCalcResultVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 薪资核算核心引擎
 *
 * 核心职责：
 * 1. 试用期比例折算
 * 2. 考勤扣款（迟到/早退/旷工）
 * 3. 事假扣款（基于日工资）
 * 4. 社保公积金扣除（基于基数+比例+上下限）
 * 5. 个税累计预扣法
 * 6. 预警机制（请假>15天、加班>50小时、环比波动>30%）
 *
 * 所有金额精确到分（scale=2, HALF_UP）
 */
@Service
public class SalaryCalculationService {

    /** 个税税率表 — 级数 -> [累计应纳税所得额上限, 税率, 速算扣除数] */
    private static final BigDecimal[][] TAX_BRACKETS = {
            {new BigDecimal("36000"), new BigDecimal("0.03"), BigDecimal.ZERO},
            {new BigDecimal("144000"), new BigDecimal("0.10"), new BigDecimal("2520")},
            {new BigDecimal("300000"), new BigDecimal("0.20"), new BigDecimal("16920")},
            {new BigDecimal("420000"), new BigDecimal("0.25"), new BigDecimal("31920")},
            {new BigDecimal("660000"), new BigDecimal("0.30"), new BigDecimal("52920")},
            {new BigDecimal("960000"), new BigDecimal("0.35"), new BigDecimal("85920")},
            {null, new BigDecimal("0.45"), new BigDecimal("181920")}, // 超过960000
    };

    /** 金额计算精度：2位小数，四舍五入 */
    private static final int SCALE = 2;

    /**
     * 计算单月薪资
     *
     * @param input    薪资计算输入参数
     * @param siConfig 社保公积金配置
     * @return 薪资计算结果
     */
    public SalaryCalcResultVO calculate(SalaryCalcDTO input, SocialInsuranceConfig siConfig) {
        // ── 步骤1: 计算试用期折算后应发基本工资 ──
        BigDecimal totalBaseSalary = input.getBasicSalary().add(input.getPositionSalary());
        BigDecimal probationRatio = input.getProbationRatio() != null
                ? input.getProbationRatio() : BigDecimal.ONE;
        BigDecimal adjustedBaseSalary = totalBaseSalary.multiply(probationRatio)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // ── 步骤2: 考勤扣款（迟到、早退、旷工） ──
        BigDecimal attendanceDeduction = calcAttendanceDeduction(input, totalBaseSalary);

        // ── 步骤3: 事假扣款 ──
        BigDecimal leaveDeduction = calcLeaveDeduction(input, totalBaseSalary);

        // ── 步骤4: 应发合计（不低于0） ──
        BigDecimal grossPay = adjustedBaseSalary
                .subtract(attendanceDeduction)
                .subtract(leaveDeduction)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // ── 步骤5: 社保个人缴纳（含基数上下限） ──
        BigDecimal socialInsurancePersonal = calcSocialInsurance(input, siConfig);

        // ── 步骤6: 公积金个人缴纳（含基数上下限） ──
        BigDecimal housingFundPersonal = calcHousingFund(input, siConfig);

        // ── 步骤7: 个税累计预扣法 ──
        TaxResult taxResult = calcCumulativeTax(grossPay, socialInsurancePersonal,
                housingFundPersonal, input, siConfig);

        // ── 步骤8: 实发工资 ──
        BigDecimal netPay = grossPay
                .subtract(socialInsurancePersonal)
                .subtract(housingFundPersonal)
                .subtract(taxResult.tax)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // ── 步骤9: 预警检测（黄色/橙色） ──
        List<String> warnings = detectWarnings(input, netPay);

        // ── 步骤10: 阻断检测（红色，必须修复） ──
        List<String> blockings = detectBlockings(input, grossPay, netPay);

        // ── 组装结果 ──
        return SalaryCalcResultVO.builder()
                .employeeId(input.getEmployeeId())
                .year(input.getYear())
                .month(input.getMonth())
                .basicSalary(adjustedBaseSalary)
                .attendanceDeduction(attendanceDeduction)
                .leaveDeduction(leaveDeduction)
                .grossPay(grossPay)
                .socialInsurancePersonal(socialInsurancePersonal)
                .housingFundPersonal(housingFundPersonal)
                .taxableIncome(taxResult.taxableIncome)
                .tax(taxResult.tax)
                .netPay(netPay)
                .probationRatio(probationRatio)
                .leaveDays(input.getPersonalLeaveDays() != null
                        ? input.getPersonalLeaveDays() : BigDecimal.ZERO)
                .overtimeHours(input.getOvertimeHours() != null
                        ? input.getOvertimeHours() : BigDecimal.ZERO)
                .lateCount(input.getLateCount() != null ? input.getLateCount() : 0)
                .earlyCount(input.getEarlyCount() != null ? input.getEarlyCount() : 0)
                .absentCount(input.getAbsentDays() != null ? input.getAbsentDays() : BigDecimal.ZERO)
                .warnings(warnings)
                .blockings(blockings)
                .cumulativeTaxableIncome(taxResult.cumulativeTaxableIncome)
                .taxRate(taxResult.taxRate)
                .quickDeduction(taxResult.quickDeduction)
                .build();
    }

    // ══════════════════════════════════════════════════════
    // 考勤扣款计算
    // ══════════════════════════════════════════════════════

    private BigDecimal calcAttendanceDeduction(SalaryCalcDTO input, BigDecimal totalBaseSalary) {
        BigDecimal deduction = BigDecimal.ZERO;

        // 迟到罚款
        int lateCount = input.getLateCount() != null ? input.getLateCount() : 0;
        if (lateCount > 0) {
            deduction = deduction.add(
                    input.getLateFinePerTime().multiply(BigDecimal.valueOf(lateCount)));
        }

        // 早退罚款
        int earlyCount = input.getEarlyCount() != null ? input.getEarlyCount() : 0;
        if (earlyCount > 0) {
            deduction = deduction.add(
                    input.getEarlyFinePerTime().multiply(BigDecimal.valueOf(earlyCount)));
        }

        // 旷工扣款（日工资 × 旷工天数 × 扣款比例）
        // 注意：先用高精度中间值计算，最后再四舍五入，避免精度损失
        BigDecimal absentDays = input.getAbsentDays() != null
                ? input.getAbsentDays() : BigDecimal.ZERO;
        if (absentDays.compareTo(BigDecimal.ZERO) > 0) {
            // 日工资用高精度(6位)避免中间舍入误差
            BigDecimal dailySalary = totalBaseSalary.divide(
                    input.getPaidDaysPerMonth(), 6, RoundingMode.HALF_UP);
            BigDecimal absentDeduction = dailySalary
                    .multiply(absentDays)
                    .multiply(input.getAbsentDeductionRatio())
                    .setScale(SCALE, RoundingMode.HALF_UP);
            deduction = deduction.add(absentDeduction);
        }

        return deduction.setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ══════════════════════════════════════════════════════
    // 事假扣款计算
    // ══════════════════════════════════════════════════════

    private BigDecimal calcLeaveDeduction(SalaryCalcDTO input, BigDecimal totalBaseSalary) {
        BigDecimal leaveDays = input.getPersonalLeaveDays() != null
                ? input.getPersonalLeaveDays() : BigDecimal.ZERO;

        if (leaveDays.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 日工资 = 总工资 / 21.75（高精度中间值，避免舍入误差）
        BigDecimal dailySalary = totalBaseSalary.divide(
                input.getPaidDaysPerMonth(), 6, RoundingMode.HALF_UP);

        // 事假扣款 = 日工资 × 事假天数（最终四舍五入到分）
        return dailySalary.multiply(leaveDays).setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ══════════════════════════════════════════════════════
    // 社保个人缴纳（含基数上下限）
    // ══════════════════════════════════════════════════════

    private BigDecimal calcSocialInsurance(SalaryCalcDTO input, SocialInsuranceConfig config) {
        // 确定实际缴费基数（卡在上下限之间）
        BigDecimal actualBase = clampBase(
                input.getSocialInsuranceBase(),
                config.getSocialInsuranceFloor(),
                config.getSocialInsuranceCeiling());

        // 个人社保总比例 = 养老 + 医疗 + 失业
        BigDecimal totalRatio = config.getPensionPersonalRatio()
                .add(config.getMedicalPersonalRatio())
                .add(config.getUnemploymentPersonalRatio());

        return actualBase.multiply(totalRatio).setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ══════════════════════════════════════════════════════
    // 公积金个人缴纳（含基数上下限）
    // ══════════════════════════════════════════════════════

    private BigDecimal calcHousingFund(SalaryCalcDTO input, SocialInsuranceConfig config) {
        BigDecimal actualBase = clampBase(
                input.getHousingFundBase(),
                config.getHousingFundFloor(),
                config.getHousingFundCeiling());

        return actualBase.multiply(config.getHousingFundPersonalRatio())
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 将缴费基数限制在 [floor, ceiling] 范围内
     */
    private BigDecimal clampBase(BigDecimal base, BigDecimal floor, BigDecimal ceiling) {
        if (base == null) return BigDecimal.ZERO;

        BigDecimal result = base;
        if (floor != null && result.compareTo(floor) < 0) {
            result = floor;
        }
        if (ceiling != null && result.compareTo(ceiling) > 0) {
            result = ceiling;
        }
        return result;
    }

    // ══════════════════════════════════════════════════════
    // 个税累计预扣法
    // ══════════════════════════════════════════════════════

    private TaxResult calcCumulativeTax(BigDecimal grossPay,
                                         BigDecimal socialInsurance,
                                         BigDecimal housingFund,
                                         SalaryCalcDTO input,
                                         SocialInsuranceConfig config) {

        // 当月减除费用（起征点）
        BigDecimal monthlyThreshold = config.getTaxThreshold() != null
                ? config.getTaxThreshold() : new BigDecimal("5000");

        // 当月应纳税所得额
        BigDecimal monthlyTaxable = grossPay
                .subtract(socialInsurance)
                .subtract(housingFund)
                .subtract(monthlyThreshold)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 累计收入（含本月）
        BigDecimal cumulativeGross = input.getCumulativeGrossPay().add(grossPay);
        BigDecimal cumulativeSocialIns = input.getCumulativeSocialInsurance().add(socialInsurance);
        BigDecimal cumulativeHousing = input.getCumulativeHousingFund().add(housingFund);

        // 累计减除费用 = 5000 × 累计月数
        int cumulativeMonths = input.getMonth();
        BigDecimal cumulativeThreshold = monthlyThreshold.multiply(
                BigDecimal.valueOf(cumulativeMonths));

        // 累计应纳税所得额
        BigDecimal cumulativeTaxableIncome = cumulativeGross
                .subtract(cumulativeSocialIns)
                .subtract(cumulativeHousing)
                .subtract(cumulativeThreshold)
                .subtract(input.getCumulativeSpecialDeduction() != null
                        ? input.getCumulativeSpecialDeduction() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 根据累计应纳税所得额查找税率和速算扣除数
        BigDecimal taxRate = BigDecimal.ZERO;
        BigDecimal quickDeduction = BigDecimal.ZERO;

        if (cumulativeTaxableIncome.compareTo(BigDecimal.ZERO) > 0) {
            for (BigDecimal[] bracket : TAX_BRACKETS) {
                if (bracket[0] == null || cumulativeTaxableIncome.compareTo(bracket[0]) <= 0) {
                    taxRate = bracket[1];
                    quickDeduction = bracket[2];
                    break;
                }
            }
        }

        // 累计应预缴个税
        BigDecimal cumulativeTaxShould = cumulativeTaxableIncome
                .multiply(taxRate)
                .subtract(quickDeduction)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 本月应缴个税 = 累计应缴 - 累计已缴
        BigDecimal cumulativeTaxPaid = input.getCumulativeTaxPaid() != null
                ? input.getCumulativeTaxPaid() : BigDecimal.ZERO;
        BigDecimal monthlyTax = cumulativeTaxShould.subtract(cumulativeTaxPaid)
                .max(BigDecimal.ZERO)
                .setScale(SCALE, RoundingMode.HALF_UP);

        return new TaxResult(monthlyTaxable, monthlyTax, cumulativeTaxableIncome, taxRate, quickDeduction);
    }

    /**
     * 个税计算结果内部类
     */
    static class TaxResult {
        final BigDecimal taxableIncome;          // 当月应纳税所得额
        final BigDecimal tax;                     // 当月个税
        final BigDecimal cumulativeTaxableIncome; // 累计应纳税所得额（含本月）
        final BigDecimal taxRate;                 // 适用税率
        final BigDecimal quickDeduction;          // 速算扣除数

        TaxResult(BigDecimal taxableIncome, BigDecimal tax,
                  BigDecimal cumulativeTaxableIncome, BigDecimal taxRate, BigDecimal quickDeduction) {
            this.taxableIncome = taxableIncome;
            this.tax = tax;
            this.cumulativeTaxableIncome = cumulativeTaxableIncome;
            this.taxRate = taxRate;
            this.quickDeduction = quickDeduction;
        }
    }

    // ══════════════════════════════════════════════════════
    // 预警检测（黄色/橙色，不阻断审批）
    // ══════════════════════════════════════════════════════

    private List<String> detectWarnings(SalaryCalcDTO input, BigDecimal netPay) {
        List<String> warnings = new ArrayList<>();

        // 预警1: 请假天数 > 15天
        BigDecimal leaveDays = input.getPersonalLeaveDays() != null
                ? input.getPersonalLeaveDays() : BigDecimal.ZERO;
        if (leaveDays.compareTo(BigDecimal.valueOf(SalaryWarningConstants.LEAVE_DAYS_THRESHOLD)) > 0) {
            warnings.add(SalaryWarningConstants.WARN_LEAVE_OVER_15);
        }

        // 预警2: 加班小时 > 50小时
        BigDecimal overtimeHours = input.getOvertimeHours() != null
                ? input.getOvertimeHours() : BigDecimal.ZERO;
        if (overtimeHours.compareTo(BigDecimal.valueOf(SalaryWarningConstants.OVERTIME_HOURS_THRESHOLD)) > 0) {
            warnings.add(SalaryWarningConstants.WARN_OVERTIME_OVER_50H);
        }

        // 预警3: 环比薪资波动 > 30%
        BigDecimal lastMonthNetPay = input.getLastMonthNetPay();
        if (lastMonthNetPay != null && lastMonthNetPay.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = lastMonthNetPay.subtract(netPay).abs();
            BigDecimal changeRatio = change.divide(lastMonthNetPay, 4, RoundingMode.HALF_UP);
            if (changeRatio.compareTo(
                    BigDecimal.valueOf(SalaryWarningConstants.SALARY_CHANGE_RATIO_THRESHOLD)) >= 0) {
                warnings.add(SalaryWarningConstants.WARN_SALARY_CHANGE_OVER_30PCT);
            }
        }

        return warnings;
    }

    // ══════════════════════════════════════════════════════
    // 阻断检测（红色，必须修复后才能提交审批）
    // ══════════════════════════════════════════════════════

    /**
     * 检测红色阻断级别的异常。
     * 阻断异常会阻止薪资批次提交审批，必须修复后重新核算。
     *
     * @param input    薪资计算输入
     * @param grossPay 应发合计
     * @param netPay   实发工资
     * @return 阻断原因列表，空列表表示无阻断
     */
    public List<String> detectBlockings(SalaryCalcDTO input, BigDecimal grossPay, BigDecimal netPay) {
        List<String> blockings = new ArrayList<>();

        // 阻断1: 实发工资 <= 0（员工拿不到钱）
        if (netPay.compareTo(BigDecimal.ZERO) <= 0) {
            blockings.add(SalaryWarningConstants.BLOCK_NET_PAY_ZERO_OR_NEGATIVE);
        }

        // 阻断2: 应发工资为0（基本工资+岗位工资可能配置错误）
        if (grossPay.compareTo(BigDecimal.ZERO) <= 0) {
            blockings.add(SalaryWarningConstants.BLOCK_GROSS_PAY_ZERO);
        }

        // 阻断3: 基本工资为空或为零（薪资账套可能未正确配置）
        if (input.getBasicSalary() == null || input.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
            blockings.add(SalaryWarningConstants.BLOCK_SALARY_ACCOUNT_MISSING);
        }

        return blockings;
    }

    /**
     * 检查计算结果是否包含阻断异常
     */
    public boolean hasBlockings(SalaryCalcResultVO result) {
        return result.getBlockings() != null && !result.getBlockings().isEmpty();
    }

    /**
     * 检查计算结果是否包含预警
     */
    public boolean hasWarnings(SalaryCalcResultVO result) {
        return result.getWarnings() != null && !result.getWarnings().isEmpty();
    }
}
