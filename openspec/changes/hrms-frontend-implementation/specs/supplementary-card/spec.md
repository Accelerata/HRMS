## ADDED Requirements

### Requirement: Submit Supplementary Card Application
The system SHALL provide a form to submit a supplementary card application with attendanceDate, cardType (1=上班卡/2=下班卡), supplementTime, and reason. employeeId MUST be auto-derived from current user.

#### Scenario: Submit application
- **WHEN** user fills form and submits
- **THEN** system calls POST /supplementary-card/apply and displays created application with status=0 (草稿)

### Requirement: Approve Supplementary Card
The system SHALL display a list of pending supplementary card applications for approvers. Approver can approve (action=1) or reject (action=2) with a comment. Status SHALL update to 2 (已通过) or 3 (已拒绝).

#### Scenario: Approve application
- **WHEN** approver clicks "通过" and enters comment
- **THEN** system calls POST /supplementary-card/{id}/approve with action=1

#### Scenario: Reject application
- **WHEN** approver clicks "拒绝" and enters comment
- **THEN** system calls POST /supplementary-card/{id}/approve with action=2

### Requirement: My Applications
The system SHALL display the current user's supplementary card applications with status tags (color-coded: 0=灰色草稿, 1=蓝色审批中, 2=绿色已通过, 3=红色已拒绝).

#### Scenario: View my applications
- **WHEN** user navigates to "我的补卡记录"
- **THEN** system calls GET /supplementary-card/my and renders list with status tags
