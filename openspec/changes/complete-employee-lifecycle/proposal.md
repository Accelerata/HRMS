## Why

需求文档第 5 大点定义了「入转调离」四大人事异动流程（入职、转正、调岗、离职），这是 HRMS 实现「员工数字化档案、入转调离全流程线上化」目标的核心。经代码审计，四大流程目前仅有「提交 → 审批 → 改状态」的骨架（完成度约 40%~50%），且存在**阻断性缺陷**和一批未落地的业务规则：

- **入职通过后员工无法登录**：`OnboardingService.executeOnboarding` 构建了 `SysUser` 但 `SysUserMapper` 根本没有 `insert` 方法，账号永不落库。
- **离职员工仍可登录（安全缺陷）**：`ResignationService.executeResignation` 仅 `user.setStatus(0)` 但从不持久化，账号禁用是空操作。
- **工号生成规则错误且会碰撞**：实际为 `HRM+yyyyMMdd+4位随机数`，不符合「年份4位+部门编码2位+序号3位」且无唯一性保证。
- **无定时任务、无通知**：全项目无 `@EnableScheduling`，试用期到期提醒、离职到期过渡、审批超时催办均未实现；无任何邮件/站内信/RocketMQ 通知。
- **业务流程不完整**：入职缺「待入职→确认到岗」环节、转正缺「延长试用/辞退」分支、调岗缺第 3 级 HR 备案、离职缺「待离职」中间态与到期过渡。
- **四大流程接口对非管理员全部 403**：`OnboardingController`/`RegularizationController`/`TransferController`/`ResignationController` 使用的 `onboarding:manage`、`regularization:manage`、`transfer:manage`、`resignation:manage` 四个权限码在 RBAC 种子数据中均未定义，按 `PermissionAspect` 逻辑仅系统管理员（dataScope=1）可访问，普通 HR 无法使用任何入转调离功能。
- **状态用魔法数字**：`Employee.status` 是裸 Integer（0~4），无可读性与类型安全。

## What Changes

补齐「入转调离」后端缺失能力，使代码与需求第 5 大点吻合（**仅后端，前端后期再做**）：

- **生命周期公共底座**：新增 `EmployeeStatusEnum`；重写工号生成器（年4+部门2+序号3，DB 序号+唯一约束防碰撞）；`SysUserMapper` 增加 insert/update，打通账号创建与禁用，新增「首次登录强制改密」标记；引入 `@EnableScheduling` 定时任务框架。
- **入职流程补全**：落地「已批准待入职 → HR 确认到岗 → 已入职」环节；审批通过后自动完成工号生成 + 账号创建 + 通知；补全表单字段（录用类型、试用期薪资比例、试用期取职位默认、汇报人默认部门负责人）；支持草稿删除、审批中撤回（限第一级）、待入职修改入职日期/标记放弃；二审按「非标准职位/薪资超职级范围」条件触发。
- **转正流程补全**：新增定时任务每天扫描试用期员工，到期前 7 天产生「待发起转正」提醒；审批结果支持「通过 / 延长试用 / 不通过（辞退）」三分支；转正调薪接入审批。
- **调岗流程补全**：审批流补第 3 级「HR 负责人备案」并按「原部门 → 新部门 → HR」顺序流转；可调项补全职级、直接汇报人、薪资调整（调薪触发额外审批）；发起时校验在职状态。
- **离职流程补全**：落地「审批通过 → 待离职 →（到达离职日期）→ 已离职」，定时任务自动过渡；离职生效时账号禁用落库、释放工号、从考勤组移除、薪资核算至离职日；离职日期校验 ≥ 今天、交接人必填；离职档案脱敏查询；补 `resignation:manage` 权限码。
- **审批引擎增强**：增加审批顺序门控（step_order 串行）；审批超时（每级 48h）定时催办/升级；审批结果与待办通知。

## Capabilities

### New Capabilities

- `employee-lifecycle-foundation`: 员工生命周期公共底座 —— `EmployeeStatusEnum`、工号生成器、系统账号创建/禁用（含首次登录强制改密）、定时任务框架。
- `onboarding-flow`: 入职流程完整实现 —— 状态流转（含待入职/确认到岗）、通过后自动处理（工号+账号+通知）、条件二审、表单字段与草稿/撤回/放弃操作。
- `regularization-flow`: 转正流程完整实现 —— 试用期到期定时提醒、审批三分支（通过/延长试用/辞退）、转正调薪审批。
- `transfer-flow`: 调岗流程完整实现 —— 三级顺序审批（原部门→新部门→HR备案）、可调项补全（职级/汇报人/薪资）、发起状态校验。
- `resignation-flow`: 离职流程完整实现 —— 待离职中间态与到期自动过渡、账号禁用落库、工号释放、考勤组/薪资处理、档案脱敏、表单校验。
- `approval-engine-enhancement`: 审批引擎增强 —— 顺序门控、超时催办/升级、审批通知。

### Modified Capabilities

<!-- 现有 specs 仅 personal-info-encryption / salary-encryption / data-migration-encryption，本 change 不改变其需求，故无修改 -->

## Impact

- **Entity 层**：`Employee`（status 语义、userId 回填）、`SysUser`（新增 force_change_pwd 等）、`TransferApplication`（补 grade/reportTo/salary 字段）、`ResignationApplication`（待离职相关）、`ApprovalRecord`（超时字段）。
- **Mapper 层**：`SysUserMapper` 新增 insert/update；`EmployeeMapper` 新增工号序号查询、待离职到期查询；各业务 Mapper 补查询方法。
- **Service 层**：四大流程 Service 重写 execute/submit/approve 逻辑；新增 `EmployeeNoGenerator`、`EmployeeAccountService`、`NotificationService`、定时任务类；`ApprovalStateMachineService` 增加顺序门控。
- **Controller 层**：各流程 Controller 新增端点（确认到岗、撤回、删除草稿、标记放弃、延长试用等）。
- **枚举/常量**：新增 `EmployeeStatusEnum`，迁移各 Service 中的魔法数字。
- **数据库**：新增/变更列（sys_user.force_change_pwd、approval_record.due_time、transfer_application 扩展列、employee 工号唯一约束等），提供增量 SQL 脚本。
- **配置**：`application.yml` 增加定时任务、通知相关配置；主启动类增加 `@EnableScheduling`。
- **安全**：修复离职账号禁用空操作、入职账号不落库两个安全/阻断缺陷；落实字段级权限与脱敏。
- **权限**：补 RBAC 种子数据——`onboarding:manage`、`regularization:manage`、`transfer:manage`、`resignation:manage` 四个流程权限码当前均未定义（整个 resources 无此定义），导致非系统管理员访问四流程接口一律 403；需新增并分配给相应角色。遵循 CLAUDE.md「每完成一个模块检查权限管理是否完成」。
