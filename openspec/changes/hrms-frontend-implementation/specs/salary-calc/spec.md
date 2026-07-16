## ADDED Requirements

### Requirement: Single Employee Salary Calculation
The system SHALL provide a salary calculation trigger: select employee, year, month, and click "核算". Result SHALL display full salary breakdown: baseSalary, attendanceDeduction, leaveDeduction, overtimePay, bonusTotal, socialInsurance, housingFund, taxableIncome, incomeTax, grossPay, netPay.

#### Scenario: Calculate salary
- **WHEN** HR selects employeeId=1, year=2026, month=7, clicks "核算"
- **THEN** system calls POST /salary/calculate/1?year=2026&month=7 and renders salary breakdown

### Requirement: Batch Salary Calculation
The system SHALL provide a batch calculation dialog: select year, month, optional deptId, then trigger calculation for all employees in scope. Progress indicator SHALL show while calculating.

#### Scenario: Company-wide batch
- **WHEN** HR selects year=2026, month=7, deptId=null, clicks "批量核算"
- **THEN** system calls POST /salary/batch-calculate and displays count of processed employees

#### Scenario: Department-specific batch
- **WHEN** HR selects deptId=2
- **THEN** system only calculates for employees in department 2

### Requirement: Salary Batches List
The system SHALL display batches with status flow: DRAFT → PENDING_APPROVAL → APPROVED → PAID → ARCHIVED. Each batch SHALL show actionable buttons matching current status.

#### Scenario: View batches
- **WHEN** page loads
- **THEN** system calls GET /salary/batches and renders list with status badges

### Requirement: Batch Records View
The system SHALL display salary records within a batch in a paginated table.

#### Scenario: View batch records
- **WHEN** user clicks a batch row
- **THEN** system calls GET /salary/batches/{id}/records and renders record table

### Requirement: Submit Batch for Approval
The system SHALL provide "提交" button when batch status is DRAFT. On click, status changes to PENDING_APPROVAL.

#### Scenario: Submit batch
- **WHEN** HR clicks "提交" on a DRAFT batch
- **THEN** system calls POST /salary/batches/{id}/submit

### Requirement: Approve Salary Batch
The system SHALL provide approve/reject actions on PENDING_APPROVAL batches.

#### Scenario: Approve batch
- **WHEN** finance manager approves batch with comment
- **THEN** system calls POST /salary/batches/{id}/approve with action=1

### Requirement: Pay Salary Batch
The system SHALL provide "发放" button when batch is APPROVED. On click, status changes to PAID.

#### Scenario: Process payment
- **WHEN** finance clicks "发放"
- **THEN** system calls POST /salary/batches/{id}/pay

### Requirement: Archive Salary Batch
The system SHALL provide "归档" button when batch is PAID. On click, status changes to ARCHIVED.

#### Scenario: Archive batch
- **WHEN** HR clicks "归档"
- **THEN** system calls POST /salary/batches/{id}/archive

### Requirement: Query Salary Records
The system SHALL display salary records filtered by employeeId (optional), year, month. HR/FINANCE see all records, employees see only own.

#### Scenario: HR queries all records
- **WHEN** HR selects year=2026, month=7 (no employeeId)
- **THEN** system calls GET /salary/records?year=2026&month=7

#### Scenario: Employee queries own record
- **WHEN** employee with employeeId=1 views salary record
- **THEN** system auto-filters to employeeId=1

### Requirement: Yearly Salary Records
The system SHALL display yearly salary summary for an employee as AntV line chart showing monthly netPay trend.

#### Scenario: View yearly trend
- **WHEN** user selects employeeId=1, year=2026
- **THEN** system calls GET /salary/records/yearly?employeeId=1&year=2026 and renders line chart

### Requirement: My Payslips
The system SHALL display the employee's own payslip list with month/year.

#### Scenario: View payslips
- **WHEN** employee navigates to "我的工资条"
- **THEN** system calls GET /salary/payslips

### Requirement: Payslip Detail with Secondary Verification
The system SHALL require password verification on first view of a specific payslip. If password not yet verified for this record, system MUST prompt for password before displaying detail.

#### Scenario: First view requires password
- **WHEN** employee first opens payslip for recordId=1
- **THEN** system shows password modal; on correct password, calls GET /salary/payslips/1?password=xxx

#### Scenario: Subsequent views skip password
- **WHEN** employee re-opens the same payslip after successful verification
- **THEN** system calls GET /salary/payslips/1 without password

#### Scenario: Wrong password
- **WHEN** employee enters incorrect password
- **THEN** system displays error "密码错误，请重试"
