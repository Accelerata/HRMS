## ADDED Requirements

### Requirement: Employee List with Advanced Search
The system SHALL display a paginated employee table with advanced multi-condition filtering (keyword, phone, deptIds, positionIds, statuses, grades, date range). The table MUST show employeeNo, name, phone (masked), email, deptName, positionName, grade, status, entryDate. Deleting an employee SHALL require confirmation.

#### Scenario: Basic search
- **WHEN** user enters keyword in search bar and clicks search
- **THEN** system queries GET /employee/list with keyword parameter and displays matching results

#### Scenario: Advanced search
- **WHEN** user expands advanced search panel and selects multiple dept/position/status filters
- **THEN** system sends corresponding list parameters and displays filtered results

#### Scenario: Pagination
- **WHEN** user navigates to page 3 or changes page size to 20
- **THEN** system re-queries with updated page/size parameters

### Requirement: Employee Detail View
The system SHALL display full employee information in a drawer or dedicated page, including masked sensitive fields (phone, idCard, bankAccount). HR and ADMIN roles SHALL see all fields.

#### Scenario: View employee detail
- **WHEN** user clicks on an employee row
- **THEN** system calls GET /employee/{id} and displays detail in a drawer

### Requirement: Create Employee
The system SHALL provide a form (ModalForm) for creating employee records with validation (required fields: name, phone, email, deptId, positionId, entryDate). On success, refresh list.

#### Scenario: Create with valid data
- **WHEN** user fills all required fields and submits
- **THEN** system calls POST /employee and refreshes the employee list

#### Scenario: Validation errors
- **WHEN** user submits with missing required fields
- **THEN** system highlights invalid fields with error messages

### Requirement: Edit Employee
The system SHALL allow editing of employee records via a pre-populated form. Fields SHALL be validated same as creation.

#### Scenario: Edit employee
- **WHEN** user clicks edit button and modifies fields
- **THEN** system calls PUT /employee/{id} and refreshes detail/list

### Requirement: Delete Employee
The system SHALL require confirmation before deletion and MUST prevent deletion of employees with active processes.

#### Scenario: Delete with confirmation
- **WHEN** user clicks delete and confirms in popconfirm
- **THEN** system calls DELETE /employee/{id} and removes row from table

### Requirement: RBAC Button Control
The system SHALL show/hide action buttons (Create, Edit, Delete) based on user's role permissions (emp:create, emp:edit, emp:delete). Non-privileged users MUST NOT see these buttons.

#### Scenario: HR views employee list
- **WHEN** user with HR role views the employee list
- **THEN** Create, Edit, and Delete buttons are visible

#### Scenario: Employee views employee list
- **WHEN** user with EMPLOYEE role views the employee list
- **THEN** only View button is visible; Create/Edit/Delete are hidden
