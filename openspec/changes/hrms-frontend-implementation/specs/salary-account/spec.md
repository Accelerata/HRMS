## ADDED Requirements

### Requirement: View Salary Account
The system SHALL display an employee's salary account details: baseSalary, pensionBase, medicalBase, housingFundBase, housingFundRatio, planId, status.

#### Scenario: View account
- **WHEN** HR selects employee
- **THEN** system calls GET /salary/accounts/{employeeId}

### Requirement: View Adjustment History
The system SHALL display salary adjustment history for an employee as a timeline or table.

#### Scenario: View history
- **WHEN** HR clicks "变更历史"
- **THEN** system calls GET /salary/accounts/{employeeId}/history

### Requirement: Create Salary Account
The system SHALL provide a form to create salary account with required fields: employeeId, planId, baseSalary, pensionBase, medicalBase, housingFundBase, housingFundRatio.

#### Scenario: Create account
- **WHEN** HR fills form and submits
- **THEN** system calls POST /salary/accounts

### Requirement: Adjust Salary
The system SHALL provide an adjustment form pre-populated with current values. On submit, system creates a new version and records adjustment history.

#### Scenario: Adjust salary
- **WHEN** HR changes baseSalary from 25000 to 28000 and submits
- **THEN** system calls PUT /salary/accounts/{id}/adjust and shows success

### Requirement: Deactivate Salary Account
The system SHALL provide "停用" button with confirmation to deactivate an account.

#### Scenario: Deactivate
- **WHEN** HR confirms deactivation
- **THEN** system calls PUT /salary/accounts/{id}/deactivate and account status becomes 禁用
