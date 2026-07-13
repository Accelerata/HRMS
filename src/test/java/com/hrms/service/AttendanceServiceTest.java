package com.hrms.service;

import com.hrms.entity.AttendanceGroup;
import com.hrms.enums.AttendanceStatus;
import com.hrms.mapper.AttendanceGroupMapper;
import com.hrms.mapper.AttendanceRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AttendanceService 考勤打卡判定 - 单元测试
 * TDD RED → GREEN → 持续验证
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService 考勤打卡判定服务")
class AttendanceServiceTest {

    @Mock
    private AttendanceGroupMapper attendanceGroupMapper;

    @Mock
    private AttendanceRecordMapper attendanceRecordMapper;

    @InjectMocks
    private AttendanceService attendanceService;

    // ── 考勤组模板 ──
    private AttendanceGroup fixedGroup;    // 固定班: 09:00-18:00, 弹性阈值10分钟
    private AttendanceGroup flexibleGroup; // 弹性班: 09:00-18:00, 弹性阈值60分钟

    @BeforeEach
    void setUp() {
        // @InjectMocks 自动注入

        // 固定班：9:00上班，18:00下班，弹性阈值10分钟（即9:10后算迟到），旷工阈值120分钟
        fixedGroup = buildGroup(1L, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, 120);

        // 弹性班：9:00上班，18:00下班，弹性阈值60分钟（即10:00后算迟到），旷工阈值120分钟
        flexibleGroup = buildGroup(2L, 2, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 120);
    }

    private AttendanceGroup buildGroup(Long id, int type, LocalTime start, LocalTime end,
                                        int flexThreshold, int absentThreshold) {
        AttendanceGroup g = new AttendanceGroup();
        g.setId(id);
        g.setGroupType(type);
        g.setStartTime(start);
        g.setEndTime(end);
        g.setFlexThreshold(flexThreshold);
        g.setAbsentHalfDayThreshold(absentThreshold);
        return g;
    }

    // ═══════════════════════════════════════════
    // 固定班 - 上班打卡判定
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("固定班 - 上班打卡判定")
    class FixedShiftPunchIn {

        @Test
        @DisplayName("正常打卡：9:00 整点打卡 → NORMAL")
        void shouldReturnNormalWhenOnTime() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(9, 0));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("正常打卡：9:09 在弹性阈值内 → NORMAL")
        void shouldReturnNormalWhenWithinFlexThreshold() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(9, 9));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("正常打卡：8:50 提前到岗 → NORMAL")
        void shouldReturnNormalWhenEarly() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(8, 50));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("迟到：9:11 超出弹性阈值 → LATE")
        void shouldReturnLateWhenExceedsFlexThreshold() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(9, 11));
            assertEquals(AttendanceStatus.LATE, status);
        }

        @Test
        @DisplayName("迟到：9:30 迟到30分钟 → LATE")
        void shouldReturnLateWhen30MinutesLate() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(9, 30));
            assertEquals(AttendanceStatus.LATE, status);
        }

        @Test
        @DisplayName("旷工半天：迟到超过120分钟(如11:10) → ABSENT_HALF_DAY")
        void shouldReturnAbsentHalfDayWhenLateOverThreshold() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(11, 10));
            assertEquals(AttendanceStatus.ABSENT_HALF_DAY, status);
        }

        @Test
        @DisplayName("旷工半天：迟到正好120分钟(11:00) → ABSENT_HALF_DAY")
        void shouldReturnAbsentHalfDayWhenLateExactlyThreshold() {
            // 9:00 + 10(弹性) + 120(旷工阈值) = 11:10
            // 但这里迟到时间 = 11:00 - 9:00 = 120分钟，超过弹性10后 = 110分钟
            // 重新算：迟到超过弹性阈值的时间 >= 120分钟即旷工
            // 即 打卡时间 >= 9:00 + 10 + 120 = 11:10
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(11, 10));
            assertEquals(AttendanceStatus.ABSENT_HALF_DAY, status);
        }

        @Test
        @DisplayName("缺卡：上班卡为null → MISSING_PUNCH")
        void shouldReturnMissingPunchWhenPunchInIsNull() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, null);
            assertEquals(AttendanceStatus.MISSING_PUNCH, status);
        }
    }

    // ═══════════════════════════════════════════
    // 固定班 - 下班打卡判定
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("固定班 - 下班打卡判定")
    class FixedShiftPunchOut {

        @Test
        @DisplayName("正常打卡：18:00 整点 → NORMAL")
        void shouldReturnNormalWhenOnTime() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(18, 0));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("正常打卡：18:30 晚走 → NORMAL")
        void shouldReturnNormalWhenStayingLate() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(18, 30));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("早退：17:50 提前走 → EARLY")
        void shouldReturnEarlyWhenLeavingBeforeEndTime() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(17, 50));
            assertEquals(AttendanceStatus.EARLY, status);
        }

        @Test
        @DisplayName("早退：17:00 提前1小时 → EARLY")
        void shouldReturnEarlyWhenOneHourEarly() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(17, 0));
            assertEquals(AttendanceStatus.EARLY, status);
        }

        @Test
        @DisplayName("旷工半天：早退超过120分钟(如15:50) → ABSENT_HALF_DAY")
        void shouldReturnAbsentHalfDayWhenEarlyOverThreshold() {
            // 18:00 - 120分钟 = 16:00，早于16:00就算旷工半天
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(15, 59));
            assertEquals(AttendanceStatus.ABSENT_HALF_DAY, status);
        }

        @Test
        @DisplayName("缺卡：下班卡为null → MISSING_PUNCH")
        void shouldReturnMissingPunchWhenPunchOutIsNull() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, null);
            assertEquals(AttendanceStatus.MISSING_PUNCH, status);
        }
    }

    // ═══════════════════════════════════════════
    // 弹性班 - 上班打卡判定
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("弹性班 - 上班打卡判定")
    class FlexibleShiftPunchIn {

        @Test
        @DisplayName("正常打卡：9:30 在弹性60分钟内 → NORMAL")
        void shouldReturnNormalWhenWithinFlexWindow() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    flexibleGroup, LocalTime.of(9, 30));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("正常打卡：9:59 刚好在弹性范围内 → NORMAL")
        void shouldReturnNormalAtFlexBoundary() {
            // 9:00 + 60分钟弹性 = 10:00之前都正常
            AttendanceStatus status = attendanceService.determinePunchIn(
                    flexibleGroup, LocalTime.of(9, 59));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("迟到：10:01 超出弹性阈值 → LATE")
        void shouldReturnLateWhenExceedsFlexWindow() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    flexibleGroup, LocalTime.of(10, 1));
            assertEquals(AttendanceStatus.LATE, status);
        }

        @Test
        @DisplayName("旷工半天：弹性班迟到超过120分钟 → ABSENT_HALF_DAY")
        void shouldReturnAbsentHalfDayWhenLateOverThreshold() {
            // 9:00 + 60(弹性) + 120(旷工阈值) = 12:00
            AttendanceStatus status = attendanceService.determinePunchIn(
                    flexibleGroup, LocalTime.of(12, 0));
            assertEquals(AttendanceStatus.ABSENT_HALF_DAY, status);
        }
    }

    // ═══════════════════════════════════════════
    // 弹性班 - 下班打卡判定
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("弹性班 - 下班打卡判定")
    class FlexibleShiftPunchOut {

        @Test
        @DisplayName("正常打卡：18:00 整点 → NORMAL")
        void shouldReturnNormalWhenOnTime() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    flexibleGroup, LocalTime.of(18, 0));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("早退：17:30 提前走 → EARLY")
        void shouldReturnEarlyWhenLeavingBeforeEndTime() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    flexibleGroup, LocalTime.of(17, 30));
            assertEquals(AttendanceStatus.EARLY, status);
        }

        @Test
        @DisplayName("旷工半天：弹性班早退超过120分钟 → ABSENT_HALF_DAY")
        void shouldReturnAbsentHalfDayWhenEarlyOverThreshold() {
            // 18:00 - 120 = 16:00
            AttendanceStatus status = attendanceService.determinePunchOut(
                    flexibleGroup, LocalTime.of(15, 59));
            assertEquals(AttendanceStatus.ABSENT_HALF_DAY, status);
        }
    }

    // ═══════════════════════════════════════════
    // 边界场景
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("边界场景")
    class EdgeCases {

        @Test
        @DisplayName("精确等于弹性阈值边界点：9:10(固定班) → NORMAL")
        void shouldReturnNormalAtExactFlexBoundaryFixed() {
            AttendanceStatus status = attendanceService.determinePunchIn(
                    fixedGroup, LocalTime.of(9, 10));
            assertEquals(AttendanceStatus.NORMAL, status);
        }

        @Test
        @DisplayName("精确等于旷工阈值边界点：16:00早退 → EARLY(非旷工)")
        void shouldReturnEarlyAtExactAbsentBoundary() {
            // 18:00 - 120 = 16:00，16:00 还没超过120分钟
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(16, 0));
            assertEquals(AttendanceStatus.EARLY, status);
        }

        @Test
        @DisplayName("旷工阈值边界+1分钟：15:59早退 → ABSENT_HALF_DAY")
        void shouldReturnAbsentHalfDayAtBoundaryPlusOne() {
            AttendanceStatus status = attendanceService.determinePunchOut(
                    fixedGroup, LocalTime.of(15, 59));
            assertEquals(AttendanceStatus.ABSENT_HALF_DAY, status);
        }
    }
}
