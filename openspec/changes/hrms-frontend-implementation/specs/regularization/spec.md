## ADDED Requirements

### Requirement: Regularization List
The system SHALL display a paginated table of regularization applications with status and keyword filters.

#### Scenario: View list
- **WHEN** page loads
- **THEN** system calls GET /regularization/page?page=1&size=10

### Requirement: Submit Regularization Application
The system SHALL provide a form with employeeId, probationEndDate, applyDate. System SHALL auto-populate probationEndDate based on employee entry date.

#### Scenario: Submit application
- **WHEN** HR selects employee and submits
- **THEN** system calls POST /regularization

### Requirement: Approve Regularization with Result Type
The system SHALL provide three approval result types: 1=通过转正, 2=延长试用 (requires extendedMonths), 3=不通过辞退.

#### Scenario: Approve with regularization
- **WHEN** approver selects resultType=1 and submits
- **THEN** system calls PUT /regularization/{id}/approve and employee status changes to 正式

#### Scenario: Extend probation
- **WHEN** approver selects resultType=2, enters extendedMonths=1
- **THEN** system calls PUT /regularization/{id}/approve with extendedMonths field

#### Scenario: Reject regularization
- **WHEN** approver selects resultType=3
- **THEN** system calls PUT /regularization/{id}/approve and employee status changes accordingly

### Requirement: Expiring Employee List
The system SHALL display employees whose probation is expiring within N days (default 7). List SHALL show employeeName, deptName, entryDate, probationEndDate, daysUntilExpire.

#### Scenario: View expiring list
- **WHEN** HR views the expiring list with days=7
- **THEN** system calls GET /regularization/expiring?days=7 and renders table sorted by daysUntilExpire ascending
