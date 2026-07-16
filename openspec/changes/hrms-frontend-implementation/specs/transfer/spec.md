## ADDED Requirements

### Requirement: Transfer List
The system SHALL display a paginated table of transfer applications.

#### Scenario: View transfer list
- **WHEN** page loads
- **THEN** system calls GET /transfers/page?page=1&size=10

### Requirement: Submit Transfer Application
The system SHALL provide a form with employeeId, fromDeptId, toDeptId, fromPositionId, toPositionId, effectiveDate, reason. fromDeptId/fromPositionId SHALL auto-populate based on selected employee.

#### Scenario: Submit transfer
- **WHEN** HR selects employee, target dept/position, effective date, and reason
- **THEN** system calls POST /transfers

### Requirement: Dual Approval for Transfer
The system SHALL require approval from both the original department manager (role=old) and new department manager (role=new). The approval dialog MUST show which role is currently pending.

#### Scenario: Old department approves
- **WHEN** old dept manager approves with role="old"
- **THEN** system calls PUT /transfers/{id}/approve with role="old"

#### Scenario: New department approves
- **WHEN** new dept manager approves with role="new"
- **THEN** system calls PUT /transfers/{id}/approve with role="new"

#### Scenario: View dual approval status
- **WHEN** viewing transfer detail
- **THEN** system displays both approval statuses clearly (原部门: 已通过/待审批, 新部门: 已通过/待审批)
