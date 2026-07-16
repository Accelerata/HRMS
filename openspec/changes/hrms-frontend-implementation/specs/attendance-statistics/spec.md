## ADDED Requirements

### Requirement: Personal Monthly Statistics
The system SHALL display monthly attendance statistics for a selected employee, including totalWorkDays, normalDays, lateDays, earlyDays, absentHalfDays, missingPunchDays, and attendanceRate. Data MUST be visualized using AntV gauge chart for attendance rate.

#### Scenario: View personal stats
- **WHEN** user selects employee, year=2026, month=7
- **THEN** system calls GET /attendance-statistics/personal and renders stat cards + gauge chart

### Requirement: Department Monthly Statistics
The system SHALL display department-level monthly attendance summary with deptAttendanceRate. The rate SHALL be visualized as an AntV gauge chart.

#### Scenario: View department stats
- **WHEN** user selects dept, year=2026, month=7
- **THEN** system calls GET /attendance-statistics/dept and renders department summary

### Requirement: Attendance Trend Chart
The system SHALL display an AntV line chart showing daily attendance trends (late/early/absent counts) across the month for a department.

#### Scenario: Department trend view
- **WHEN** department attendance stats page loads
- **THEN** system renders line chart comparing normal/late/early/absent days across months
