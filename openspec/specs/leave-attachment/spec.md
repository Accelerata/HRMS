# leave-attachment

## Purpose

支持请假申请上传证明材料附件（如医院证明、结婚证复印件），将文件存储至阿里云 OSS 并在 `leave_attachment` 表记录元数据。提交时按假期类型与天数强制校验附件必填。

## Requirements

### Requirement: 附件上传与存储

系统 SHALL 提供附件上传接口，将证明文件存储至阿里云 OSS 并在 `leave_attachment` 表记录元数据（文件名、objectKey、访问 URL、大小、类型、上传人）。系统 MUST 校验文件扩展名（jpg/jpeg/png/pdf）与大小（≤10MB，可配置），超限拒绝。objectKey MUST 按 `leave/{yyyyMM}/{uuid}.{ext}` 规则生成以保证唯一。OSS 上传失败时 MUST NOT 落库元数据。

#### Scenario: 上传成功返回 URL

- **WHEN** 员工上传一个合法的 PDF 证明文件
- **THEN** 文件写入 OSS，`leave_attachment` 落一行（application_id 暂为 NULL），返回附件 id 与访问 URL

#### Scenario: 非法文件类型被拒绝

- **WHEN** 员工上传 .exe 文件
- **THEN** 系统拒绝并提示文件类型不支持

#### Scenario: 超过大小限制被拒绝

- **WHEN** 员工上传超过 10MB 的图片
- **THEN** 系统拒绝并提示文件过大

### Requirement: 附件绑定请假申请

员工 SHALL 在提交请假申请时通过 `attachmentIds` 将已上传附件绑定到申请（回写 `application_id`）。系统 MUST 校验所绑附件归属当前用户本人，绑定他人附件 MUST 拒绝。

#### Scenario: 申请绑定附件

- **WHEN** 员工创建请假申请并携带 2 个本人已上传附件 id
- **THEN** 两个附件的 application_id 回写为该申请 id

#### Scenario: 绑定他人附件被拒绝

- **WHEN** 员工申请中携带他人上传的附件 id
- **THEN** 系统拒绝（越权防护）

### Requirement: 证明材料条件必填

提交审批时，系统 MUST 对需证明材料的假期类型强制校验已绑定至少 1 个附件：病假且天数 >1 天、婚假、产假。病假 ≤1 天及其余类型不强制。未满足时提交 MUST 被拒绝并提示上传证明材料。

#### Scenario: 病假 2 天无证明提交失败

- **WHEN** 员工提交 2 天病假申请且未上传附件
- **THEN** 系统拒绝提交并提示请上传医院证明

#### Scenario: 病假 1 天免传

- **WHEN** 员工提交 1 天病假申请且无附件
- **THEN** 提交通过（不强制附件）

#### Scenario: 婚假无证明提交失败

- **WHEN** 员工提交婚假申请且未上传结婚证复印件
- **THEN** 系统拒绝提交并提示请上传证明材料

### Requirement: 附件查看

申请人、该申请的审批人与 HR SHALL 能查看请假申请的附件列表。无关员工 MUST NOT 查看他人申请附件。

#### Scenario: 审批人查看附件

- **WHEN** 审批人查看待审请假申请的附件列表
- **THEN** 返回该申请已绑定附件的文件名与访问 URL
