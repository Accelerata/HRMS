# approval-engine-enhancement

## MODIFIED Requirements

### Requirement: 审批结果通知

系统 SHALL 在审批产生待办、审批通过、审批拒绝时，经 `NotificationService` 通知相关人员（审批人、提交人）。提交人 MUST 以审批上下文中显式携带的 submitterUserId 为准，不得误投给审批人。

#### Scenario: 审批拒绝通知提交人

- **WHEN** 某申请被审批拒绝
- **THEN** 系统向提交人发送拒绝通知（含审批意见）

#### Scenario: 提交人通知不误投审批人

- **WHEN** 审批上下文携带 submitterUserId=员工甲，第一级审批人为主管乙
- **THEN** 审批结果通知发送给员工甲而非主管乙

## ADDED Requirements

### Requirement: 多业务类型支持

系统 SHALL 支持七类审批业务类型：入职(1)、转正(2)、调岗(3)、离职(4)、薪资批次(5)、请假(6)、补卡(7)，各类型经审批模板定义步骤后即可接入引擎的顺序门控、超时催办与通知能力。

#### Scenario: 新业务类型接入

- **WHEN** 请假类型(6)存在审批模板且某请假申请提交
- **THEN** 引擎按模板生成审批待办，顺序门控与 48h 超时催办对其生效

### Requirement: 直接上级审批人指向

系统 SHALL 支持 `direct_supervisor` 审批人指向：解析为员工 `report_to` 汇报人关联的系统账号；汇报人缺失或无账号时回退部门负责人；仍无法解析时回退 HR 专员兜底并记录告警，不得静默丢弃审批步骤。

#### Scenario: 按汇报人解析

- **WHEN** 员工甲的 report_to 为主管乙且乙有关联账号
- **THEN** direct_supervisor 步骤的审批人生成为乙

#### Scenario: 汇报人缺失回退部门负责人

- **WHEN** 员工甲无 report_to，其部门负责人为丙
- **THEN** direct_supervisor 步骤的审批人生成为丙

### Requirement: 委托自动改派钩子

引擎生成审批记录时 SHALL 检查审批人是否存在生效中的委托，命中则自动将审批人改派为被委托人并留存原审批人信息（assign_type=2），待办通知发送给被委托人。

#### Scenario: 命中委托自动改派

- **WHEN** 审批人 A 存在生效中委托（被委托人 B），引擎为某业务生成 A 的审批记录
- **THEN** 记录审批人为 B、原审批人留存为 A、assign_type=2，B 收到待办通知

### Requirement: 改派任务代审留痕

审批记录 SHALL 承载改派信息（original_approver_id/name、assign_type：0-正常 1-转交 2-委托）。对 assign_type 非 0 的记录执行审批时，引擎 MUST 在审批意见前自动附加「{操作人} 代 {原审批人} 审批」留痕。

#### Scenario: 代审意见留痕

- **WHEN** B 审批 assign_type=2、原审批人为 A 的记录，意见为「同意」
- **THEN** 存储的审批意见为「B 代 A 审批：同意」
