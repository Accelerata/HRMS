## ADDED Requirements

### Requirement: Personal Leave Statistics
The system SHALL display monthly leave stats for an employee: totalLeaveDays broken down by leave type (annualLeaveDays, sickLeaveDays, personalLeaveDays, otherLeaveDays).

#### Scenario: View personal leave stats
- **WHEN** user selects employee, year, month
- **THEN** system calls GET /leave/stats/personal/{employeeId}?year=2026&month=7 and renders stat cards

### Requirement: Department Leave Rate
The system SHALL display department leave rate (leaveEmployeeCount / totalEmployees) with leaveRate percentage. The rate SHALL be visualized with an AntV stat card.

#### Scenario: View department leave rate
- **WHEN** user selects dept, year, month
- **THEN** system calls GET /leave/stats/dept/{deptId}?year=2026&month=7

### Requirement: Leave Type Distribution Chart
The system SHALL render an AntV pie/donut chart showing leave type distribution (annual/sick/personal/other) by total days, with optional deptId filter.

#### Scenario: Company-wide distribution
- **WHEN** user selects year=2026, month=7 (no deptId)
- **THEN** system calls GET /leave/stats/type-distribution?year=2026&month=7 and renders pie chart

#### Scenario: Department-specific distribution
- **WHEN** user selects deptId=2
- **THEN** system adds deptId filter and chart updates accordingly
