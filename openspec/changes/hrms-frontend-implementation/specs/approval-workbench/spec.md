## ADDED Requirements

### Requirement: My Todo List
The system SHALL display pending approval items as a list showing businessTypeName, applicantName, applicantDept, applicationTime, deadline, summary. Each item SHALL link to its approval detail.

#### Scenario: View todo list
- **WHEN** approver navigates to approval workbench
- **THEN** system calls GET /approvals/todo and renders todo list with business type badges

#### Scenario: Empty todo
- **WHEN** approver has no pending items
- **THEN** system displays empty state "暂无待办事项"

### Requirement: My Done List
The system SHALL display completed approval items that the current user has processed.

#### Scenario: View done list
- **WHEN** user switches to "已办" tab
- **THEN** system calls GET /approvals/done and renders list

### Requirement: Unified Approval Detail
The system SHALL display a unified approval detail page showing applicationInfo (context-sensitive) and approvalHistory (timeline). The page MUST show currentActionable flag and nextApprover info.

#### Scenario: View leave approval detail
- **WHEN** approver clicks a leave todo item
- **THEN** system calls GET /approvals/detail/1/{businessId} and renders leave application detail + approval timeline

#### Scenario: View onboarding approval detail
- **WHEN** approver clicks an onboarding todo item
- **THEN** system calls GET /approvals/detail/2/{businessId} and renders onboarding application detail + approval timeline

### Requirement: Transfer Approval Task
The system SHALL provide a transfer dialog to reassign an approval task to another approver, with required reason field.

#### Scenario: Transfer task
- **WHEN** approver selects targetApproverId and enters reason
- **THEN** system calls POST /approvals/records/{id}/transfer

### Requirement: Delegation Management
The system SHALL provide delegation CRUD: create (delegateTo, startDate, endDate, reason), view (GET /approvals/delegations/my), cancel (DELETE /approvals/delegations/{id}).

#### Scenario: Create delegation
- **WHEN** user sets delegation for a date range
- **THEN** system calls POST /approvals/delegations

#### Scenario: Cancel delegation
- **WHEN** user cancels an active delegation
- **THEN** system calls DELETE /approvals/delegations/{id}
