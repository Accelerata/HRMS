## ADDED Requirements

### Requirement: User Login
The system SHALL provide a login form with username, password, and optional captcha fields. On successful authentication, the system MUST store the JWT token, user info (userId, username, realName, roleCode, employeeId) in localStorage and redirect to the dashboard.

#### Scenario: Successful login
- **WHEN** user enters valid username and password and submits
- **THEN** system stores token, redirects to dashboard, and shows welcome message

#### Scenario: Login failure
- **WHEN** user enters invalid credentials
- **THEN** system displays error message "用户名或密码错误" without revealing which field is wrong

#### Scenario: Expired token
- **WHEN** user performs any API request with an expired token
- **THEN** system intercepts the 401 response, clears stored credentials, and redirects to login page

### Requirement: User Logout
The system SHALL provide a logout button in the header. On click, it MUST clear token and user info from localStorage and redirect to login page.

#### Scenario: Logout
- **WHEN** user clicks logout button
- **THEN** system clears credentials and navigates to /login

### Requirement: Change Password
The system SHALL provide a password change form accessible from the user dropdown menu, requiring old password and new password confirmation.

#### Scenario: Change password success
- **WHEN** user enters correct old password and valid new password
- **THEN** system calls PUT /auth/change-password and displays success message

#### Scenario: Wrong old password
- **WHEN** user enters incorrect old password
- **THEN** system displays error "原密码错误"
