## ADDED Requirements

### Requirement: Resignation List
The system SHALL display a paginated table of resignation applications with status and keyword filters.

#### Scenario: View list
- **WHEN** page loads
- **THEN** system calls GET /resignations/page?page=1&size=10

### Requirement: Submit Resignation Application
The system SHALL provide a form with employeeId, resignationDate, resignationType (1=主动/2=被动/3=协商), reason, handoverTo, handoverNotes.

#### Scenario: Submit resignation
- **WHEN** HR fills form and submits
- **THEN** system calls POST /resignations

### Requirement: Approve Resignation
The system SHALL provide approve/reject actions for resignation applications. On approval, employee status SHALL change to 待离职, and system MUST disable the employee account.

#### Scenario: Approve resignation
- **WHEN** approver approves with comment
- **THEN** system calls PUT /resignations/{id}/approve and employee moves to 待离职 status
