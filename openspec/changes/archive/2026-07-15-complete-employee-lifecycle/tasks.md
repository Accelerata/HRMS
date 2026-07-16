## 1. 生命周期公共底座（employee-lifecycle-foundation）

- [x] 1.1 新增 `EmployeeStatusEnum`（待入职0/试用期1/正式2/待离职3/已离职4），提供 `fromCode`、状态流转合法性校验方法
- [x] 1.2 新增 `EmployeeNoGenerator` 组件：按「年4+部门编码2+序号3」生成工号，DB 查询该年该部门最大序号 +1
- [x] 1.3 `employee` 表增加 `employee_no` 唯一约束（增量 SQL `phase9-lifecycle.sql`）；生成器捕获唯一冲突并重试
- [x] 1.4 `SysUserMapper` 新增 `insert`、`updateStatus`、`updatePassword` 方法及对应 XML/注解 SQL
- [x] 1.5 `SysUser` 实体新增 `forceChangePwd` 字段；`sys_user` 表增加 `force_change_pwd` 列（增量 SQL）
- [x] 1.6 新增 `EmployeeAccountService`：创建账号（username=手机号、随机初始密码、forceChangePwd=1、绑定角色）、禁用账号、回填 `employee.userId`
- [x] 1.7 登录侧（`AuthService`）校验 `forceChangePwd`，强制首次登录改密；新增改密端点
- [x] 1.8 主启动类增加 `@EnableScheduling`；新增 `NotificationService` 接口 + `InAppNotificationService` 实现（notification 表 + 日志）
- [x] 1.9 新增 `notification` 表（增量 SQL）与 `NotificationMapper`
- [x] 1.10 **补 RBAC 种子数据（前置，阻断非管理员使用）**：新增 `onboarding:manage`、`regularization:manage`、`transfer:manage`、`resignation:manage` 四个流程权限码，并分配给相应角色（HR 等）；当前四个码均未种子化，非系统管理员访问四流程接口一律 403。同步核对 `sys_role_permission` 关联

## 2. 入职流程补全（onboarding-flow）

- [x] 2.1 `executeOnboarding` 改为：生成工号（用 EmployeeNoGenerator）+ 创建账号（EmployeeAccountService）+ 回填 userId + 员工置「待入职(0)」+ 申请单停留 `PENDING_ENTRY(4)`，不再直接入职
- [x] 2.2 新增「确认到岗」端点 `POST /onboarding/{id}/confirm-arrival`：员工转「试用期(1)」、申请单转 `ONBOARDED(5)`、写异动日志（employeeId 正确回填）
- [x] 2.3 入职通过后触发通知（欢迎候选人 + 通知 HR/部门负责人，经 NotificationService）
- [x] 2.4 表单字段补全：`OnboardingApplication`/DTO 增加「录用类型(全职/兼职/实习)」「试用期薪资比例」；试用期默认取 `Position.defaultProbationMonths`；汇报人默认部门负责人
- [x] 2.5 草稿删除端点 `DELETE /onboarding/{id}`（仅草稿可删）
- [x] 2.6 审批中撤回端点 `POST /onboarding/{id}/withdraw`（仅第一级未被审批时可撤回，回到草稿）
- [x] 2.7 待入职「修改入职日期」「标记放弃」端点
- [x] 2.8 入职二审条件化：非标准职位或薪资超职级范围时才生成第二级 HR 审批（审批模板支持条件/动态步骤）

## 3. 转正流程补全（regularization-flow）

- [x] 3.1 `LifecycleScheduler` 新增定时任务：每天扫描试用期员工，`entryDate + probationMonths - 7 <= today` 且未发起转正 → 产生「待发起转正」提醒
- [x] 3.2 审批结果支持三分支：通过 / 延长试用 / 不通过（辞退）；`ApprovalActionDTO` 或转正专用 DTO 携带结果类型
- [x] 3.3 「延长试用」分支：更新 `regularDate` 延长、保持试用期状态、记录异动
- [x] 3.4 「不通过」分支：按配置置「待离职」并产生提醒（或发起离职审批），记录异动
- [x] 3.5 转正后薪资调整接入审批：调薪时 `formalSalary` 经加密 TypeHandler 存储并触发相应审批节点
- [x] 3.6 `probationSummary`（试用期表现评价）增加必填校验

## 4. 调岗流程补全（transfer-flow）

- [x] 4.1 调岗审批模板改为三级：`old_dept_manager`(step1) → `new_dept_manager`(step2) → `hr_specialist`(step3)（phase3 SQL 或增量 SQL 调整）
- [x] 4.2 `TransferService.approve` 支持三级顺序审批（配合顺序门控），第三级 HR 备案通过后生效
- [x] 4.3 `TransferApplication`/DTO 增加可调项：`toGrade`、`toReportTo`、`salaryAdjust`；生效时更新员工职级/汇报人
- [x] 4.4 薪资调整触发额外审批节点；调薪金额经加密 TypeHandler 存储
- [x] 4.5 `submit` 校验员工在职状态为「试用期/正式」；校验 `toDeptId != fromDeptId`（部门必须变更）
- [x] 4.6 异动日志完整记录：原岗位/新岗位/调岗日期/原因（before/after 数据补全）

## 5. 离职流程补全（resignation-flow）

- [x] 5.1 `submit` 校验：在职状态为「试用期/正式」、离职日期 ≥ 今天、交接人必填
- [x] 5.2 审批通过后员工置「待离职(3)」并记录 `resignDate`，不立即置已离职
- [x] 5.3 `LifecycleScheduler` 定时任务：`resignDate <= today` 的待离职员工自动转「已离职(4)」并执行生效动作
- [x] 5.4 离职生效：账号禁用落库（EmployeeAccountService.disable）、释放工号、从考勤组移除、薪资核算截止标记
- [x] 5.5 离职档案脱敏查询：脱敏 VO 对身份证/银行卡/薪资掩码（按角色）
- [x] 5.6 核对离职接口的数据权限（IDOR 防护）：非授权角色不可查看/审批他人离职申请（权限码种子见 1.10）
- [x] 5.7 离职类型枚举对齐需求（辞职/辞退/合同到期不续签/其他）+ 离职原因（主动/被动/协商）

## 6. 审批引擎增强（approval-engine-enhancement）

- [x] 6.1 `processApproval` 增加顺序门控：存在更低 step_order 的未处理记录时拒绝当前审批
- [x] 6.2 `approval_record` 增加 `due_time` 列（增量 SQL）；`startApproval` 计算每级 48h 截止时间
- [x] 6.3 `LifecycleScheduler` 定时任务：扫描超时未处理审批 → 催办通知（升级策略记录日志，不自动改审批人）
- [x] 6.4 审批动作（通过/拒绝/待办）经 NotificationService 通知相关人

## 7. 测试与验证

- [x] 7.1 `EmployeeNoGenerator` 单元测试：规则正确性、序号递增、唯一冲突重试
- [x] 7.2 `EmployeeAccountService` 集成测试：创建账号落库、禁用落库、userId 回填
- [x] 7.3 入职全链路集成测试：提交→审批→待入职→确认到岗→已入职，验证工号/账号/状态
- [x] 7.4 转正三分支测试：通过/延长试用/不通过
- [x] 7.5 调岗三级审批测试 + 发起状态校验测试
- [x] 7.6 离职待离职→定时过渡→已离职测试（账号禁用落库、脱敏）
- [x] 7.7 审批顺序门控测试 + 超时催办测试
- [ ] 7.8 全量回归：JDK 17 下 `mvn test` 通过（注意本机 JDK 26 与 Lombok 不兼容，须用 JDK 17）

## 8. 文档与权限核对

- [x] 8.1 更新增量 SQL 脚本说明（phase9-lifecycle.sql）
- [x] 8.2 核对四大流程权限码与数据权限（IDOR 防护）：确认 `onboarding:manage`/`regularization:manage`/`transfer:manage`/`resignation:manage` 已种子化并分配角色，非管理员（HR）可端到端访问四流程接口，遵循 CLAUDE.md「每完成一个模块检查权限管理是否完成」
- [x] 8.3 更新 CLAUDE.md / openspec 说明定时任务、通知、工号规则约定
