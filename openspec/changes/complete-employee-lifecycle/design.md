## Context

「入转调离」四大流程当前仅有骨架：每个流程都有 Entity / Service / Controller / Mapper，复用同一个模板驱动的审批引擎 `ApprovalStateMachineService`。但审计发现一批阻断缺陷与未落地的业务规则（见 proposal.md）。本 change 在**不重写现有骨架**的前提下，补齐公共底座与四大流程的缺失能力，使后端行为与需求文档第 5 大点一致。

### 约束

- 仅后端；不改前端、不引入新前端依赖。
- 沿用现有技术栈：Spring Boot + MyBatis，不引入新的工作流引擎（如 Activiti/Flowable），审批仍走现有模板驱动引擎并增强。
- 沿用已落地的加密体系（`EncryptedStringTypeHandler` 等），新增敏感字段（如调岗薪资）须注册 TypeHandler 并计算 `*_hash`（如适用）。
- 定时任务用 Spring `@Scheduled`（轻量，无需引入 Quartz/xxl-job）。
- 通知抽象为 `NotificationService` 接口，先用「站内信/日志」实现，预留 RocketMQ/邮件扩展点（CLAUDE.md 要求 RocketMQ，本期以接口+内存实现落地，避免阻塞）。
- 金额精确到分（BigDecimal），遵循 CLAUDE.md。

## Goals / Non-Goals

**Goals:**
- 修复两个阻断/安全缺陷：入职账号落库、离职账号禁用落库。
- 工号生成符合需求规则且全局唯一。
- 四大流程状态流转与需求一致（含入职待入职/确认到岗、转正延长/辞退、调岗三级、离职待离职过渡）。
- 引入定时任务：试用期到期提醒、离职到期过渡、审批超时催办。
- 消除 `Employee.status` 魔法数字，统一 `EmployeeStatusEnum`。
- 审批引擎增加顺序门控。

**Non-Goals:**
- 不实现前端页面与 AntV 图表（后期单独做）。
- 不实现完整的工作流引擎可视化配置（审批模板仍用 SQL 种子数据维护）。
- 不实现委托审批（需求 8.3）、审批人工作台的高级功能（需求 8.2 大部分），仅保留现有 `/todo`、`/done`。
- 不实现真正的邮件/RocketMQ 发送（本期用接口+站内信/日志落地，预留扩展）。
- 不涉及薪资核算本身的计算逻辑（第 7 大点），仅处理离职时「薪资核算至离职日」的标记/截止。

## Decisions

### 1. 员工状态统一为 `EmployeeStatusEnum`

**选择**：新增枚举 `EmployeeStatusEnum`，取值 `PENDING_ENTRY(0,待入职)`、`PROBATION(1,试用期)`、`REGULAR(2,正式)`、`PENDING_RESIGN(3,待离职)`、`RESIGNED(4,已离职)`，与现有 `Employee.status` 数字语义完全对齐，提供 `fromCode` 与流转校验。

**理由**：现有代码在各 Service 中散落 `setStatus(1/2/3/4)` 魔法数字，可读性差且易错。枚举零迁移成本（数字不变），仅提升类型安全与可维护性。

### 2. 工号生成器 `EmployeeNoGenerator`

**选择**：独立组件，规则 `年份(4) + 部门编码(2) + 序号(3)`（如 `202401005`）。序号按「年+部门编码」维度在 DB 中递增（查该年该部门现有最大序号 +1），配合 `employee.employee_no` 唯一约束 + 并发下重试，保证唯一。

**理由**：需求 4.1.1 明确规则；现有 `HRM+yyyyMMdd+随机4位` 既不符规则又有碰撞风险。DB 序号+唯一约束是最可靠方案。

### 3. 系统账号生命周期 `EmployeeAccountService`

**选择**：`SysUserMapper` 新增 `insert` / `updateStatus`；入职通过时创建账号（username=手机号、随机初始密码、`force_change_pwd=1`）并回填 `employee.userId`；离职生效时 `updateStatus(0)` 落库。登录侧校验 `force_change_pwd` 强制改密（本期在 AuthService 增加校验与改密端点）。

**理由**：这是两个阻断/安全缺陷的根治。账号生命周期与员工生命周期绑定。

### 4. 定时任务框架

**选择**：主启动类加 `@EnableScheduling`，新增 `LifecycleScheduler`：
- 每天 08:00 扫描试用期员工，`entryDate + probationMonths - 7天 <= today` 且未发起转正 → 产生「待发起转正」提醒（NotificationService）。
- 每天 00:30 扫描「待离职」员工，`resignDate <= today` → 自动置「已离职」并执行离职生效（账号禁用、工号释放、考勤组移除、薪资截止）。
- 每小时扫描超时审批（`approval_record` 超 48h 未处理）→ 催办通知（升级策略本期仅记录日志+通知，不自动改审批人）。

**理由**：Spring `@Scheduled` 足够；单机部署，暂不需分布式调度。

### 5. 审批引擎顺序门控

**选择**：`processApproval` 增加校验——当前审批记录的 `step_order` 之前必须没有未处理的更低 `step_order` 记录，否则拒绝（「请先完成前置审批」）。`startApproval` 行为不变。

**理由**：需求中入职/转正/离职均为「部门负责人 → HR」串行，调岗为「原部门 → 新部门 → HR」串行。现有引擎一次性生成全部待办且不校验顺序，会出现 HR 先于部门负责人审批的乱序。加门控即可满足串行语义，改动最小。

### 6. 入职「待入职 / 确认到岗」环节

**选择**：审批全部通过后置 `PENDING_ENTRY(4)` 并**停留在该态**，生成工号与账号但标记员工 `status=待入职(0)`；新增端点 `POST /onboarding/{id}/confirm-arrival`，HR 确认实际到岗后员工 `status` 转为 `试用期(1)`、申请单转 `ONBOARDED(5)`，并开始考勤/薪资。

**理由**：现有代码审批通过即入职，跳过「待入职」态，与需求 5.1 状态图不符。

### 7. 转正审批三分支

**选择**：`ApprovalActionEnum` 语义扩展或新增转正专用结果字段，支持「通过 / 延长试用 / 不通过」。通过→员工转正式；延长试用→更新 `regularDate` 并保持试用期；不通过→触发离职/辞退子流程（本期标记为待离职并发起离职审批，或按配置直接置待离职）。

**理由**：需求 5.2.3 明确审批结果三态，现有只有通过/拒绝。

### 8. 调岗三级顺序审批与可调项补全

**选择**：审批模板调岗改为 3 步（`old_dept_manager` step1 → `new_dept_manager` step2 → `hr_specialist` step3），配合顺序门控实现串行；`TransferApplication` 增加 `to_grade`、`to_report_to`、`salary_adjust` 等字段，薪资调整时触发额外审批节点；生效时更新员工相应字段并完整记录异动历史（含原因）。

**理由**：需求 5.3.3 明确三级审批流，现有仅原+新并行两级。

### 9. 离职「待离职」中间态与到期过渡

**选择**：审批通过后置员工 `待离职(3)` 并记录 `resignDate`；定时任务到 `resignDate` 自动转「已离职(4)」并执行生效动作（账号禁用落库、工号释放、考勤组移除、薪资核算截止标记）。离职档案查询走脱敏 VO（身份证/银行卡/薪资掩码）。

**理由**：需求 5.4.2 明确「待离职 → 到达离职日期 → 已离职」，现有审批通过即已离职。

### 10. 通知抽象 `NotificationService`

**选择**：接口 + `InAppNotificationService` 实现（写 `notification` 表 + 日志），用于入职欢迎、转正提醒、审批待办/超时催办、离职通知。预留 `RocketMqNotificationService` 扩展点。

**理由**：需求多处要求通知，但项目无任何通知设施。先以最小可用落地，不阻塞主流程；RocketMQ 集成作为后续。

### 11. 入职二审条件化（task 2.8）

**选择**：
- **标准/非标准职位**：`Position` 表新增 `is_standard` 字段（TINYINT，默认 1=标准，0=非标准），由 HR 在维护职位时手动标记。
- **薪资超职级范围**：新建 `grade_salary_range` 表（grade_code + min_salary + max_salary），独立于 Position，支持按职级精确配置薪资带宽。
- **判定位置**：在 `OnboardingService.submit()` 中完成查询和判断，将 `needHr`（boolean）传入 `ApprovalContext`，`startApproval()` 据此跳过 HR 审批步骤。
- **模板扩展**：`approval_template` 表新增 `condition_expr` 字段（VARCHAR，NULL=无条件），为后续其他业务类型（如调岗薪资调整触发额外审批）的条件化预留通用机制。

**判定逻辑**：标准职位 + 薪资在范围内 → 仅部门主管审批（跳过 HR）；其他任一条件不满足 → 部门主管 + HR 双审。

**理由**：需求 5.1 和 onboarding-flow spec 明确二审为可选，仅非标准职位或薪资超范围时触发。当前 Position 无 isStandard 标记、全项目无薪资范围对照数据，需补齐基础设施。将判定放在 OnboardingService 而非状态机内，保持状态机通用性，避免业务规则侵入审批引擎。

## Risks / Trade-offs

- **[改动面大]** 涉及四大流程 + 公共底座 + 审批引擎，回归风险高 → 通过 tasks.md 分阶段（底座先行，再逐个流程），每阶段编译+测试；每完成一个模块检查权限（CLAUDE.md 要求）。
- **[工号并发]** 并发入职可能序号冲突 → DB 唯一约束 + 捕获 `DuplicateKeyException` 重试（有限次数）。
- **[定时任务幂等]** 重启/重复触发可能重复处理 → 定时任务以「状态+日期」为条件，处理前再校验状态，保证幂等。
- **[顺序门控影响存量]** 已在途的审批单若有乱序待办会被门控拦截 → 门控仅对新产生的审批生效说明，存量数据量小可人工处理；或门控做宽松校验（仅提示不阻断）作为备选。
- **[通知仅站内信]** 不满足「发送邮件」字面需求 → 已抽象接口，后续接 RocketMQ/邮件；本期在 proposal 中明确为非目标。
- **[转正辞退分支]** 「不通过=辞退」涉及离职流程联动，复杂度高 → 本期「不通过」默认置「待离职」并产生提醒，是否自动发起离职审批由配置开关控制。

## Open Questions

1. ~~入职二审「非标准职位/薪资超职级范围」的判定规则~~ **【已决议】**：`Position` 新增 `is_standard` 字段标记非标准职位；新建 `grade_salary_range` 表定义各职级薪资带宽；判定在 `OnboardingService.submit()` 中完成，见 Decision 11。
2. 转正「不通过」是否必须自动进入离职流程，还是仅标记待离职由 HR 手动处理？
3. 通知本期只做站内信是否可接受？还是必须接 RocketMQ/邮件？
4. 离职工号「本年度可复用」——工号含年份，复用是否指同一部门编码序号段复用？需确认复用策略，避免与唯一约束冲突。
