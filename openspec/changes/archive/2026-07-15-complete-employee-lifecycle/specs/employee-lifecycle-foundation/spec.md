## ADDED Requirements

### Requirement: 员工在职状态枚举

系统 SHALL 提供 `EmployeeStatusEnum` 统一表示员工在职状态，取值 MUST 与现有 `Employee.status` 数字语义对齐：`PENDING_ENTRY(0,待入职)`、`PROBATION(1,试用期)`、`REGULAR(2,正式)`、`PENDING_RESIGN(3,待离职)`、`RESIGNED(4,已离职)`。各业务 Service MUST NOT 再使用裸数字字面量设置或判断员工状态。

#### Scenario: 通过枚举设置员工状态

- **WHEN** 任意流程需要变更员工在职状态
- **THEN** 代码通过 `EmployeeStatusEnum` 的语义化常量设置状态，并通过 `fromCode` 反查，数据库中存储的数字与历史数据保持一致

#### Scenario: 非法状态流转被拒绝

- **WHEN** 代码尝试进行不合法的状态流转（如已离职员工再发起转正）
- **THEN** 系统抛出业务异常并拒绝操作

### Requirement: 工号生成

系统 SHALL 在员工入职确认时生成全局唯一工号，格式 MUST 为「年份(4位) + 部门编码(2位) + 序号(3位)」（示例：`202401005`）。序号 MUST 按「年份 + 部门编码」维度递增，工号 MUST 在 `employee.employee_no` 上具备唯一约束。

#### Scenario: 生成符合规则的工号

- **WHEN** 某员工入职确认到岗，其部门编码为 `01`，入职年份为 2024，该年该部门已有最大序号 004
- **THEN** 系统生成工号 `2024` + `01` + `005` = `202401005`

#### Scenario: 并发入职工号不重复

- **WHEN** 两个入职流程并发生成同工年同部门的工号
- **THEN** 数据库唯一约束阻止重复，冲突方自动重试获取下一个序号，最终两员工工号不同

### Requirement: 系统账号生命周期管理

系统 SHALL 在员工入职确认时自动创建系统账号，并在离职生效时禁用账号。账号创建与禁用 MUST 真实持久化到 `sys_user` 表。新建账号 MUST 以工号为登录名、生成随机初始密码、设置「首次登录强制改密」标记，并将账号主键回填到 `employee.user_id`。

#### Scenario: 入职自动创建可用账号

- **WHEN** 入职申请确认到岗，员工工号已生成
- **THEN** 系统在 `sys_user` 插入一条账号（username=工号、随机密码 BCrypt 存储、force_change_pwd=1、status=1），并将该账号 id 回填到 `employee.user_id`，员工可凭工号+初始密码登录

#### Scenario: 首次登录强制改密

- **WHEN** 新创建账号的员工首次登录
- **THEN** 系统检测到 `force_change_pwd=1`，要求修改密码后方可继续使用，改密后清除该标记

#### Scenario: 离职禁用账号真实生效

- **WHEN** 员工离职生效（到达离职日期）
- **THEN** 系统将其 `sys_user.status` 更新为禁用并持久化，该账号此后无法登录

### Requirement: 定时任务框架

系统 SHALL 启用 Spring 定时任务（`@EnableScheduling`），并提供生命周期相关调度：试用期到期提醒、离职到期自动过渡、审批超时催办。定时任务 MUST 幂等（重复触发不重复处理）。

#### Scenario: 启用调度

- **WHEN** 应用启动
- **THEN** 主启动类声明 `@EnableScheduling`，注册的 `@Scheduled` 方法按 cron 触发

### Requirement: 通知服务抽象

系统 SHALL 提供 `NotificationService` 抽象用于发送业务通知（入职欢迎、转正提醒、审批待办/催办、离职通知），并至少提供一种可用实现（站内信/落库 + 日志）。通知发送失败 MUST NOT 阻断主业务流程。

#### Scenario: 发送入职欢迎通知

- **WHEN** 入职确认到岗后
- **THEN** 系统经 `NotificationService` 向候选人生成一条欢迎通知并记录，通知写入失败不影响入职主流程
