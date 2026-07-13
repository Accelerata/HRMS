package com.hrms.service;

import com.hrms.common.exception.BaseException;
import com.hrms.entity.LeaveBalance;
import com.hrms.entity.OvertimeRecord;
import com.hrms.enums.LeaveType;
import com.hrms.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LeaveService 假期余额与请假扣减 - 单元测试
 * TDD RED → GREEN → 持续验证
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveService 假期余额与请假扣减服务")
class LeaveServiceTest {

    @Mock
    private LeaveBalanceMapper leaveBalanceMapper;

    @Mock
    private LeaveApplicationMapper leaveApplicationMapper;

    @Mock
    private OvertimeRecordMapper overtimeRecordMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        // @InjectMocks 自动注入
    }

    // ═══════════════════════════════════════════
    // 年假计算
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("年假天数计算")
    class AnnualLeaveCalculation {

        @Test
        @DisplayName("入职满1年不满10年 → 5天年假")
        void shouldReturn5DaysFor1To10Years() {
            // 入职 3 年（2023年入职，计算2026年年假）
            LocalDate entryDate = LocalDate.of(2023, 7, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(new BigDecimal("5"), days);
        }

        @Test
        @DisplayName("入职满10年不满20年 → 10天年假")
        void shouldReturn10DaysFor10To20Years() {
            // 入职 12 年（2014年入职，计算2026年年假）
            LocalDate entryDate = LocalDate.of(2014, 7, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(new BigDecimal("10"), days);
        }

        @Test
        @DisplayName("入职满20年 → 15天年假")
        void shouldReturn15DaysFor20PlusYears() {
            // 入职 22 年（2004年入职，计算2026年年假）
            LocalDate entryDate = LocalDate.of(2004, 7, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(new BigDecimal("15"), days);
        }

        @Test
        @DisplayName("入职不满1年 → 0天年假")
        void shouldReturn0DaysForLessThan1Year() {
            // 入职不到1年（2026年1月入职，计算2026年年假）
            LocalDate entryDate = LocalDate.of(2026, 1, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(BigDecimal.ZERO, days);
        }

        @Test
        @DisplayName("入职恰好满1年边界值 → 5天年假")
        void shouldReturn5DaysAtExactly1Year() {
            // 恰好满1年
            LocalDate entryDate = LocalDate.of(2025, 7, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(new BigDecimal("5"), days);
        }

        @Test
        @DisplayName("入职恰好满10年边界值 → 10天年假")
        void shouldReturn10DaysAtExactly10Years() {
            LocalDate entryDate = LocalDate.of(2016, 7, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(new BigDecimal("10"), days);
        }

        @Test
        @DisplayName("入职恰好满20年边界值 → 15天年假")
        void shouldReturn15DaysAtExactly20Years() {
            LocalDate entryDate = LocalDate.of(2006, 7, 1);
            BigDecimal days = leaveService.calculateAnnualLeaveDays(entryDate, 2026);
            assertEquals(new BigDecimal("15"), days);
        }
    }

    // ═══════════════════════════════════════════
    // 调休计算
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("调休天数计算")
    class CompensatoryLeaveCalculation {

        @Test
        @DisplayName("加班8小时 → 1天调休")
        void shouldReturn1DayFor8Hours() {
            List<OvertimeRecord> records = Arrays.asList(
                    buildOvertime(8.0),
                    buildOvertime(0.0)  // edge: 0 hours
            );
            // 8 hours total / 8 = 1 day
            // 0 hours / 8 = 0 days
            // total = 1 day
            BigDecimal days = leaveService.calculateCompensatoryDays(records);
            assertEquals(new BigDecimal("1.0"), days);
        }

        @Test
        @DisplayName("加班16小时 → 2天调休")
        void shouldReturn2DaysFor16Hours() {
            List<OvertimeRecord> records = Arrays.asList(
                    buildOvertime(8.0),
                    buildOvertime(8.0)
            );
            BigDecimal days = leaveService.calculateCompensatoryDays(records);
            assertEquals(new BigDecimal("2.0"), days);
        }

        @Test
        @DisplayName("加班4小时 → 0天调休（不满8小时不计）")
        void shouldReturn0DaysForLessThan8Hours() {
            List<OvertimeRecord> records = List.of(
                    buildOvertime(4.0)
            );
            BigDecimal days = leaveService.calculateCompensatoryDays(records);
            assertEquals(new BigDecimal("0.0"), days);
        }

        @Test
        @DisplayName("加班10小时 → 1天调休（2小时不满8不计）")
        void shouldTruncateRemainder() {
            List<OvertimeRecord> records = List.of(
                    buildOvertime(10.0)  // 10 / 8 = 1 day, 2 hours remainder
            );
            BigDecimal days = leaveService.calculateCompensatoryDays(records);
            assertEquals(new BigDecimal("1.0"), days);
        }

        @Test
        @DisplayName("多笔加班累计：3+5+6 → 1天调休（14/8=1）")
        void shouldAccumulateAndTruncate() {
            List<OvertimeRecord> records = Arrays.asList(
                    buildOvertime(3.0),
                    buildOvertime(5.0),
                    buildOvertime(6.0)  // total 14h / 8 = 1d + 6h remainder
            );
            BigDecimal days = leaveService.calculateCompensatoryDays(records);
            assertEquals(new BigDecimal("1.0"), days);
        }

        @Test
        @DisplayName("无加班记录 → 0天调休")
        void shouldReturn0DaysForEmptyRecords() {
            BigDecimal days = leaveService.calculateCompensatoryDays(Collections.emptyList());
            assertEquals(new BigDecimal("0.0"), days);
        }

        @Test
        @DisplayName("加班正好24小时 → 3天调休")
        void shouldReturn3DaysFor24Hours() {
            List<OvertimeRecord> records = List.of(
                    buildOvertime(24.0)
            );
            BigDecimal days = leaveService.calculateCompensatoryDays(records);
            assertEquals(new BigDecimal("3.0"), days);
        }
    }

    // ═══════════════════════════════════════════
    // 余额校验
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("余额是否足够")
    class BalanceCheck {

        @Test
        @DisplayName("余额5天，请假3天 → 足够")
        void shouldReturnTrueWhenEnough() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("0"));
            assertTrue(leaveService.hasEnoughBalance(balance, new BigDecimal("3")));
        }

        @Test
        @DisplayName("余额5天，请假5天 → 足够（刚好用完）")
        void shouldReturnTrueWhenExactlyEnough() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("0"));
            assertTrue(leaveService.hasEnoughBalance(balance, new BigDecimal("5")));
        }

        @Test
        @DisplayName("余额5天，已用3天，请假3天 → 不足")
        void shouldReturnFalseWhenRemainingInsufficient() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("3"));
            assertFalse(leaveService.hasEnoughBalance(balance, new BigDecimal("3")));
        }

        @Test
        @DisplayName("余额5天，请假6天 → 不足")
        void shouldReturnFalseWhenRequestExceedsTotal() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("0"));
            assertFalse(leaveService.hasEnoughBalance(balance, new BigDecimal("6")));
        }

        @Test
        @DisplayName("余额0.5天，请假0.5天 → 足够")
        void shouldReturnTrueForHalfDay() {
            LeaveBalance balance = buildBalance(1L, LeaveType.COMPENSATORY,
                    new BigDecimal("1.0"), new BigDecimal("0.5"));
            assertTrue(leaveService.hasEnoughBalance(balance, new BigDecimal("0.5")));
        }
    }

    // ═══════════════════════════════════════════
    // 余额扣减
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("余额扣减")
    class BalanceDeduction {

        @Test
        @DisplayName("扣减1天 → 已用1天、剩余4天")
        void shouldDeduct1Day() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("0"));
            LeaveBalance result = leaveService.deductBalance(balance, new BigDecimal("1"));

            assertEquals(new BigDecimal("1"), result.getUsedDays());
            assertEquals(new BigDecimal("4"), result.getRemainingDays());
        }

        @Test
        @DisplayName("扣减半天(0.5天) → 已用0.5天、剩余4.5天")
        void shouldDeductHalfDay() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("0"));
            LeaveBalance result = leaveService.deductBalance(balance, new BigDecimal("0.5"));

            assertEquals(new BigDecimal("0.5"), result.getUsedDays());
            assertEquals(new BigDecimal("4.5"), result.getRemainingDays());
        }

        @Test
        @DisplayName("扣减后刚好用完 → 已用5天、剩余0天")
        void shouldExhaustBalance() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("0"));
            LeaveBalance result = leaveService.deductBalance(balance, new BigDecimal("5"));

            assertEquals(new BigDecimal("5"), result.getUsedDays());
            assertEquals(BigDecimal.ZERO, result.getRemainingDays());
        }

        @Test
        @DisplayName("累积扣减：已有2天 + 再扣1天 → 已用3天")
        void shouldAccumulateDeduction() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("2"));
            LeaveBalance result = leaveService.deductBalance(balance, new BigDecimal("1"));

            assertEquals(new BigDecimal("3"), result.getUsedDays());
            assertEquals(new BigDecimal("2"), result.getRemainingDays());
        }

        @Test
        @DisplayName("余额不足扣减 → 抛出BaseException")
        void shouldThrowExceptionWhenInsufficient() {
            LeaveBalance balance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("4"));
            // 剩余只有1天，请2天
            BaseException ex = assertThrows(BaseException.class,
                    () -> leaveService.deductBalance(balance, new BigDecimal("2")));
            assertTrue(ex.getMessage().contains("余额不足") || ex.getMessage().contains("不足"),
                    "应提示余额不足: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // 综合场景
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("综合场景")
    class IntegrationScenarios {

        @Test
        @DisplayName("年假和调休分别独立管理")
        void shouldManageLeaveTypesIndependently() {
            LeaveBalance annualBalance = buildBalance(1L, LeaveType.ANNUAL, new BigDecimal("5"), new BigDecimal("1"));
            LeaveBalance compBalance = buildBalance(1L, LeaveType.COMPENSATORY, new BigDecimal("3"), new BigDecimal("0"));

            // 年假扣1天
            assertTrue(leaveService.hasEnoughBalance(annualBalance, new BigDecimal("1")));
            // 调休扣2天
            assertTrue(leaveService.hasEnoughBalance(compBalance, new BigDecimal("2")));
            // 年假余额不够扣5天（已用1天，剩余4天）
            assertFalse(leaveService.hasEnoughBalance(annualBalance, new BigDecimal("5")));
        }

        @Test
        @DisplayName("事假不校验余额（无上限）")
        void shouldAllowPersonalLeaveWithoutLimit() {
            LeaveBalance balance = buildBalance(1L, LeaveType.PERSONAL, new BigDecimal("0"), new BigDecimal("0"));
            // 事假没有固定余额上限，始终允许
            assertTrue(leaveService.hasEnoughBalance(balance, new BigDecimal("30")));
        }

        @Test
        @DisplayName("病假不校验余额（无上限）")
        void shouldAllowSickLeaveWithoutLimit() {
            LeaveBalance balance = buildBalance(1L, LeaveType.SICK, new BigDecimal("0"), new BigDecimal("0"));
            assertTrue(leaveService.hasEnoughBalance(balance, new BigDecimal("15")));
        }
    }

    // ═══════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════

    private LeaveBalance buildBalance(Long employeeId, LeaveType leaveType,
                                       BigDecimal total, BigDecimal used) {
        LeaveBalance b = new LeaveBalance();
        b.setId(1L);
        b.setEmployeeId(employeeId);
        b.setLeaveType(leaveType.getCode());
        b.setTotalDays(total);
        b.setUsedDays(used);
        b.setRemainingDays(total.subtract(used));
        b.setYear(2026);
        return b;
    }

    private OvertimeRecord buildOvertime(double hours) {
        OvertimeRecord r = new OvertimeRecord();
        r.setId(1L);
        r.setEmployeeId(1L);
        r.setOvertimeDate(LocalDate.of(2026, 7, 1));
        r.setHours(BigDecimal.valueOf(hours));
        r.setConvertedToComp(0);
        return r;
    }
}
