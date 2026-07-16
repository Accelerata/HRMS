## ADDED Requirements

### Requirement: Days Calculation Preview
The system SHALL provide a days calculator before leave application: user enters startDate, startPeriod, endDate, endPeriod and system previews the calculated leave days.

#### Scenario: Calculate half-day leave
- **WHEN** user enters startDate=2026-07-20, startPeriod=0 (AM), endDate=2026-07-20, endPeriod=0 (AM)
- **THEN** system calls GET /leave/days/calculate and displays 0.5 days

#### Scenario: Calculate multi-day leave
- **WHEN** user enters startDate=2026-07-20, startPeriod=0, endDate=2026-07-23, endPeriod=1
- **THEN** system calls GET /leave/days/calculate and displays 3.5 days

### Requirement: Leave Balance Query
The system SHALL display leave balances (annual, comp, personal, sick, marriage, maternity, funeral) for an employee, showing totalDays, usedDays, remainingDays.

#### Scenario: View balance
- **WHEN** user selects employee and year
- **THEN** system calls GET /leave/balance/{employeeId}?year=2026 and renders balance cards

### Requirement: Create Leave Application (Draft)
The system SHALL provide a leave application form: leaveType dropdown, date range picker, startPeriod/endPeriod, reason, handoverTo. On submit, system creates draft (status=0) and displays the calculated days.

#### Scenario: Create draft
- **WHEN** user fills form and submits
- **THEN** system calls POST /leave/apply and displays created application with auto-calculated days

#### Scenario: Insufficient balance
- **WHEN** user applies for 5 days of annual leave but only has 3 remaining
- **THEN** system displays warning "年假余额不足，剩余3天"

### Requirement: Submit Leave Application
The system SHALL provide a "Submit" button on draft leave applications. On click, status changes from 0 (草稿) to 1 (审批中).

#### Scenario: Submit draft
- **WHEN** user clicks "提交" on a draft application
- **THEN** system calls POST /leave/{id}/submit and updates status to 审批中

### Requirement: Approve Leave
The system SHALL provide approve/reject/return actions for pending leave applications. Approver MUST enter a comment.

#### Scenario: Approve leave
- **WHEN** approver clicks "通过" with comment
- **THEN** system calls POST /leave/{id}/approve with action=1

#### Scenario: Return for revision
- **WHEN** approver clicks "退回" with comment
- **THEN** system calls POST /leave/{id}/approve with action=3

### Requirement: Cancel Leave
The system SHALL allow cancellation of leave applications only when status=审批中 and applicant is the current user.

#### Scenario: Cancel pending leave
- **WHEN** applicant clicks "取消" on a 审批中 application
- **THEN** system calls POST /leave/{id}/cancel

### Requirement: Leave Application List
The system SHALL display paginated leave applications for an employee with status tags. Status colors: 0=灰色, 1=蓝色, 2=绿色, 3=红色, 4=灰色(已取消).

#### Scenario: View leave history
- **WHEN** user selects employee
- **THEN** system calls GET /leave/applications/{employeeId} and renders paginated list

### Requirement: Annual Leave Initialization
The system SHALL provide a form (HR/Admin only) to initialize annual leave balance for an employee based on entry date.

#### Scenario: Init annual leave
- **WHEN** HR enters employeeId, entryDate, year and submits
- **THEN** system calls POST /leave/balance/annual/init
