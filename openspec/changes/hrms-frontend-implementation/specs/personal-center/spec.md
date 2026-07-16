## ADDED Requirements

### Requirement: My Profile
The system SHALL display the current user's employee profile with field-level editability hints: editable fields (email, currentAddress, registeredAddress, birthday) shall have edit icons; locked fields (deptId, positionId, grade, baseSalary, bankAccount) shall show lock icon with reason tooltip.

#### Scenario: View profile
- **WHEN** employee navigates to personal center
- **THEN** system calls GET /personal/profile and renders profile with editability indicators

### Requirement: Update Personal Info
The system SHALL allow updating only four editable fields: email, currentAddress, registeredAddress, birthday. Other fields SHALL be disabled.

#### Scenario: Update email
- **WHEN** employee changes email and submits
- **THEN** system calls PUT /personal/profile with updated email

### Requirement: Attendance Calendar View
The system SHALL display a monthly calendar grid showing daily attendance status with color codes: NORMAL=绿色, LATE=橙色, EARLY=黄色, ABSENT=红色, MISSING=灰色, LEAVE=蓝色, WEEKEND=浅灰, FUTURE=白色. Clicking a day with leave SHALL show leave detail.

#### Scenario: View July calendar
- **WHEN** employee selects yearMonth=2026-07
- **THEN** system calls GET /personal/attendance-calendar?yearMonth=2026-07 and renders calendar

#### Scenario: Click leave day
- **WHEN** employee clicks a day with status=LEAVE
- **THEN** system displays leaveDetail popover (leaveType, reason, applicationId link)

### Requirement: Personal Salary Trend
The system SHALL render an AntV line chart showing the employee's monthly netPay trend for the past 12 months.

#### Scenario: View salary trend
- **WHEN** employee navigates to salary trend tab
- **THEN** system calls GET /personal/salary-trend and renders line chart

### Requirement: My Payslips Access
The system SHALL provide a shortcut to the employee's own payslips list and detail with secondary password verification (same as salary-calc module).

#### Scenario: Access from personal center
- **WHEN** employee clicks "我的工资条"
- **THEN** system navigates to payslip list view filtered to self
