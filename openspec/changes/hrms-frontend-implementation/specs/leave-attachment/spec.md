## ADDED Requirements

### Requirement: Upload Attachment
The system SHALL provide a file upload button in the leave application form supporting common document types (PDF, JPG, PNG). Max file size SHALL be 10MB.

#### Scenario: Upload diagnosis certificate
- **WHEN** user selects a PDF file and uploads
- **THEN** system calls POST /leave/attachments with multipart/form-data and displays uploaded file info

#### Scenario: File too large
- **WHEN** user uploads a file larger than 10MB
- **THEN** system displays error "文件大小不能超过10MB"

### Requirement: View Attachments
The system SHALL display attachment list for a leave application with download links.

#### Scenario: View attachments
- **WHEN** user opens leave detail with attachments
- **THEN** system calls GET /leave/{id}/attachments and renders file list with download buttons
