## ADDED Requirements

### Requirement: Onboarding List
The system SHALL display a paginated table of onboarding applications with status and keyword filters. Status tags SHALL follow unified color scheme: 0=灰色(草稿), 1=蓝色(审批中), 2=绿色(已通过), 3=红色(已拒绝), 4=橙色(待入职), 5=绿色(已入职), 6=灰色(已放弃).

#### Scenario: Filter by status
- **WHEN** user selects status=1 filter
- **THEN** system calls GET /onboarding/page?page=1&size=10&status=1

### Requirement: Submit Onboarding Application
The system SHALL provide a comprehensive form with all employee onboarding fields. Required fields: realName, phone, idCard, targetDeptId, targetPositionId, offerSalary, entryDate.

#### Scenario: Submit application
- **WHEN** user fills all required fields and submits
- **THEN** system calls POST /onboarding and redirects to detail

### Requirement: Save Draft
The system SHALL provide a "保存草稿" button to save the form as draft (status=0) without submitting for approval.

#### Scenario: Save as draft
- **WHEN** user clicks "保存草稿"
- **THEN** system calls POST /onboarding/draft with current form data

### Requirement: Approve Onboarding
The system SHALL provide approve/reject actions for pending onboarding applications.

#### Scenario: Approve onboarding
- **WHEN** approver clicks "通过" with comment
- **THEN** system calls PUT /onboarding/{id}/approve with action=1

### Requirement: Confirm Arrival
The system SHALL provide a "确认到岗" button. On confirm, system auto-generates employee number and account.

#### Scenario: Confirm arrival
- **WHEN** HR clicks "确认到岗" on an approved application
- **THEN** system calls POST /onboarding/{id}/confirm-arrival

### Requirement: Withdraw Application
The system SHALL allow withdrawing a pending onboarding application. Employee status reverts.

#### Scenario: Withdraw
- **WHEN** user clicks "撤回" on a pending application
- **THEN** system calls POST /onboarding/{id}/withdraw

### Requirement: Update Entry Date
The system SHALL allow updating the entry date on an approved application before arrival confirmation.

#### Scenario: Change entry date
- **WHEN** HR updates entry date and submits
- **THEN** system calls PUT /onboarding/{id}/entry-date

### Requirement: Abandon Onboarding
The system SHALL provide "放弃入职" action with confirmation dialog.

#### Scenario: Abandon
- **WHEN** HR confirms abandonment
- **THEN** system calls POST /onboarding/{id}/abandon
