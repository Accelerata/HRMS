## ADDED Requirements

### Requirement: Salary Plan List
The system SHALL display a list of salary plans with name, description, status.

#### Scenario: View plans
- **WHEN** page loads
- **THEN** system calls GET /salary/plans

### Requirement: Create/Update/Disable Plan
The system SHALL provide CRUD for plans, including status toggle (0=禁用, 1=启用).

#### Scenario: Create plan
- **WHEN** user creates plan with name and description
- **THEN** system calls POST /salary/plans

#### Scenario: Toggle status
- **WHEN** user toggles plan status
- **THEN** system calls PUT /salary/plans/{id}/status?status=0 or 1

### Requirement: Plan Items Management
The system SHALL display salary items for a plan and allow adding/deleting items. Items have itemName, itemType (BASE/BONUS/ALLOWANCE/DEDUCTION/INSURANCE/FUND/TAX), amount, calculationRule (FIXED/RATIO/FORMULA), sortOrder.

#### Scenario: Add item
- **WHEN** user adds "餐补" with type=ALLOWANCE, amount=500, rule=FIXED
- **THEN** system calls POST /salary/plans/{planId}/items

#### Scenario: Delete item
- **WHEN** user deletes an item
- **THEN** system calls DELETE /salary/plans/{planId}/items/{id}

### Requirement: Plan Scope Management
The system SHALL display and manage plan applicability scopes (DEPT/POSITION/GRADE).

#### Scenario: Add scope
- **WHEN** user adds scopeType=DEPT, scopeId=2, scopeName="技术部"
- **THEN** system calls POST /salary/plans/{planId}/scopes
