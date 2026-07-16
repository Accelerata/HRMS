## Context

HRMS 后端已完成约 85% 的核心业务逻辑，298 个单元测试全部通过。但后端需求完成情况检查报告揭示了一批安全漏洞、功能缺口和字段遗漏问题。本次变更涉及 8 个模块的跨切面修改，核心挑战在于：(1) 字段级权限过滤需在不破坏现有 VO 结构的前提下按角色动态裁剪敏感字段；(2) 安全合规需要同时覆盖登录拦截和定时提醒两个触达路径；(3) 离职生效处理需将多个分散的后置操作纳入事务性边界；(4) 高级搜索需要兼容加密字段的查询限制。

## Goals / Non-Goals

**Goals:**
- 实现字段级权限过滤，确保部门主管不可见员工身份证号/薪资/银行卡号，普通员工仅可见本人敏感字段
- 补全离职生效的 4 项后置处理（工号释放、考勤组移除、薪资截止、脱敏）
- 实现密码 90 天过期强制更换（登录拦截 + 到期前提醒）
- 实现 30 分钟无操作自动登出（Redis 最后活跃时间 + JWT 拦截器校验）
- 扩展员工高级搜索支持部门树多选、职位多选、职级多选、入职日期范围、手机号哈希匹配
- 补全考勤组字段（适用人员/排班制/中午休息/迟到早退阈值）、补卡限制、年假折算、考勤统计
- 新增个人中心端点（我的档案、日历视图、个人薪资趋势）
- 新建系统级 AuditLog 操作审计

**Non-Goals:**
- 前端页面实现（本期仅后端）
- 短信验证码通道接入（工资条二次验证仅实现密码方式）
- 部门合并的前端拖拽交互
- 修改现有审批流程或状态机
- 引入新的外部依赖或中间件

## Decisions

### D1: 字段级权限过滤 —— AOP 注解 + DataScope 判断

**选择**：在 `EmployeeService.toVO()` 中根据 `BaseContext.getDataScope()` 返回值，使用白名单模式裁剪敏感字段。
**备选方案**：每个 Controller 方法独立过滤 → 代码分散易遗漏，放弃。
**实现**：
- 定义 `SensitiveFieldPolicy` 枚举：`SHOW_ALL`(HR/Admin)、`HIDE_SALARY_BANK`(Manager)、`SELF_ONLY`(Employee)
- toVO() 调用 `applyFieldFilter(vo, dataScope, currentUserId)` 方法
- Manager → 将 idCard/bankAccount/salary 相关字段置 null
- Employee(非本人) → 仅保留姓名/部门/职位等非敏感基本信息
- Employee(本人) → 完整返回本人敏感字段

### D2: 会话超时 —— Redis lastActiveTime + Interceptor

**选择**：在 JWT 认证拦截器中增加 Redis 查询 `session:active:<userId>` key，TTL=30min。每次请求刷新 TTL。若 key 不存在则返回 401。
**备选方案**：JWT 内嵌 expiration → JWT 无法主动失效，无法实现 inactivity 超时。放弃。
**实现**：
- 新增 `SessionTimeoutInterceptor`（在 `AuthInterceptor` 之后执行）
- `RedisTemplate` 维护 `session:active:<userId>` → `lastActiveTime`，TTL=1800s
- 超时返回 `ResultCode.SESSION_EXPIRED`，前端跳转登录页
- 配置 `spring.session.timeout=30m`

### D3: 密码过期 —— 登录检查 + 定时提醒

**选择**：登录时检查 `SysUser.pwdUpdateTime`，超过 90 天返回特定错误码强制跳转修改密码页；定时任务每天扫描未来 7 天内将到期的用户发送提醒。
**备选方案**：拦截器检查所有请求 → 用户体验差（正在工作中被踢出）。放弃。
**实现**：
- `LoginService.login()` 成功后检查 `pwdUpdateTime.isBefore(LocalDateTime.now().minusDays(90))`
- 过期返回 `ResultCode.PASSWORD_EXPIRED`（区别于密码错误）
- `SecurityScheduler` 每天 09:00 扫描到期前 7 天的用户，发送系统通知

### D4: 离职生效处理 —— 事务性批量操作

**选择**：在 `LifecycleScheduler.processPendingResignations()` 中扩展为一个事务方法，依次执行：状态变更 → 账号禁用 → 工号标记释放 → 考勤组移除 → 薪资核算截止标记。
**备选方案**：事件驱动（RocketMQ）异步处理 → 引入额外复杂度，当前离职量级无需异步。放弃。
**实现**：
- 工号释放：`EmployeeNoGenerator.releaseEmployeeNo(employeeNo)` → 将工号后缀序号加入可复用池
- 考勤组移除：`AttendanceGroupService.removeEmployee(employeeId)` → 删除关联记录
- 薪资截止：在 `SalaryAccount` 记录 `salaryEndDate = resignDate`
- 脱敏实现：`ResignationService.getMaskedDetail()` 实现身份证/银行卡号/手机号掩码逻辑

### D5: 高级搜索 —— MyBatis 动态 SQL + 哈希匹配

**选择**：扩展 `EmployeeMapper.selectByConditions()` 方法，使用 `<if>` 标签动态拼接多选条件（部门/职位/职级用 IN 子句，日期用 BETWEEN，手机号用 phone_hash = 计算值）。
**备选方案**：MyBatis-Plus QueryWrapper → 项目使用原生 MyBatis，不引入 Plus。放弃。
**实现**：
- 新增 `EmployeeQueryDTO`：`keyword`, `deptIds[]`, `positionIds[]`, `statuses[]`, `grades[]`, `startDate`, `endDate`
- 手机号搜索：先计算 `phone_hash = SHA256(phone + pepper)` 再精确匹配
- 部门树多选：前端传叶子节点 ID 列表 → 后端直接用 IN 查询（无需递归查询子部门）

### D6: AuditLog —— AOP 注解驱动

**选择**：定义 `@Auditable` 注解，通过 `AuditAspect` 切面拦截 Controller 方法，记录操作人、操作类型、目标资源、请求参数、响应状态和执行时间。
**备选方案**：手动在每个方法中插入 → 侵入性强且易遗漏。放弃。
**实现**：
- `@Auditable(operation = "SALARY_VIEW", resourceType = "PAYSLIP")`
- `AuditAspect` 通过 `@AfterReturning` 和 `@AfterThrowing` 记录成功/失败
- 仅记录写操作和敏感读操作（薪资查看、批量导出），不记录普通查询

### D7: 部门人数递归汇总 —— CTE 查询 + 缓存

**选择**：使用 MySQL 递归 CTE（Common Table Expression）查询部门树，一次性汇总所有子部门人数，Redis 缓存 30 分钟。
**备选方案**：应用层递归查询 → N+1 查询问题。放弃。
**实现**：
```sql
WITH RECURSIVE dept_tree AS (
  SELECT id, parent_id FROM department WHERE id = #{deptId}
  UNION ALL
  SELECT d.id, d.parent_id FROM department d
  INNER JOIN dept_tree dt ON d.parent_id = dt.id
)
SELECT COUNT(*) FROM employee WHERE dept_id IN (SELECT id FROM dept_tree)
  AND status IN ('PROBATION', 'REGULAR')
```

## Risks / Trade-offs

- **[Risk] 字段过滤遗漏** → **Mitigation**: 使用白名单机制（默认为敏感不可见，显式声明可见），避免开发时忘记添加过滤规则；在单元测试中覆盖所有角色组合
- **[Risk] Redis 不可用时会话超时误判** → **Mitigation**: `SessionTimeoutInterceptor` 捕获 Redis 连接异常后降级放行（允许访问但记录告警），避免 Redis 故障导致全站不可用
- **[Risk] 部门合并数据一致性** → **Mitigation**: 在 `@Transactional` 内批量更新员工 deptId + 部门记录状态，任何一步失败全部回滚；合并前校验目标部门层级不超过 5 级
- **[Risk] 密码过期强制跳转引发用户投诉** → **Mitigation**: 提前 7/3/1 天发送系统通知提醒；过期后给予 3 次跳过机会（仅限紧急情况），但每次跳过均记录审计
- **[Trade-off] 考勤统计实时计算 vs 预聚合** → 选择实时计算（月度级别数据量可控），避免维护物化视图的同步逻辑；若后续性能瓶颈可引入定时预聚合表

## Open Questions

- 工号释放后复用策略：是否需要等待冷却期（如同一财年内不可复用）？当前设计为立即可复用
- 系统级 AuditLog 的保留周期：建议与运维确认，默认保留 180 天后归档或删除
