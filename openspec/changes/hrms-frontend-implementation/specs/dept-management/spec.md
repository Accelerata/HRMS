## ADDED Requirements

### Requirement: Department Tree Display
The system SHALL display the organization structure as an expandable Ant Design Tree with employee counts per node. The tree MUST support up to 5 levels of nesting.

#### Scenario: View department tree
- **WHEN** page loads
- **THEN** system calls GET /dept/tree and renders hierarchical tree with employee counts

### Requirement: Create Department
The system SHALL provide a modal form to create a department under a selected parent node. Fields: parentId (auto-selected), deptName, deptCode, managerId, sortOrder, status.

#### Scenario: Add sub-department
- **WHEN** user selects parent node, clicks "Add", fills form, and submits
- **THEN** system calls POST /dept and refreshes the tree

### Requirement: Edit Department
The system SHALL allow editing department info via modal form pre-populated with current data.

#### Scenario: Edit department
- **WHEN** user right-clicks or selects edit action on a department node
- **THEN** form opens with current data; on submit, calls PUT /dept/{id}

### Requirement: Delete Department
The system SHALL prevent deletion of departments that have employees or sub-departments.

#### Scenario: Delete empty department
- **WHEN** user deletes a department with no employees
- **THEN** system calls DELETE /dept/{id} and removes node from tree

#### Scenario: Delete non-empty department
- **WHEN** user attempts to delete a department with employees
- **THEN** system displays error "部门下存在员工，无法删除"

### Requirement: Merge Departments
The system SHALL provide a merge dialog to select a target department for merging employees and sub-departments into.

#### Scenario: Merge departments
- **WHEN** user selects source dept, opens merge dialog, selects target dept, and confirms
- **THEN** system calls POST /dept/{id}/merge?targetDeptId=X and refreshes tree
