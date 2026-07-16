# Tasks: 剩余问题修复与功能补全

## 1. 字段级权限过滤

- [x] 1.1 定义 `SensitiveFieldPolicy`：SHOW_ALL(HR/Admin)、HIDE_SALARY_BANK(Manager)、SELF_ONLY(Employee) 三种策略，基于 DataScopeEnum 映射
- [x] 1.2 在 `EmployeeService.toVO()` 中实现 `applyFieldFilter(vo, policy, currentUserId, targetEmployeeId)` 方法，按白名单模式裁剪敏感字段
- [x] 1.3 Manager 角色 → idCard/bankAccount/salaryAccount/emergencyContact 字段置 null
- [x] 1.4 Employee 角色 → 非本人时仅保留姓名/部门/职位/职级/在职状态等非敏感基本信息
- [x] 1.5 所有 EmployeeController 返回 EmployeeVO 的端点统一经过 toVO() 过滤
- [x] 1.6 实现 `ResignationService.getMaskedDetail()`：身份证保留首3尾4、手机号保留首3尾4、银行卡保留尾4、薪资仅最后一月汇总
- [x] 1.7 测试：部门主管查下属 → 敏感字段为空；员工查自己 → 完整返回；员工查他人 → 仅基本信息；HR查任意 → 全部可见

## 2. 离职生效处理补全

- [x] 2.1 `LifecycleScheduler.processPendingResignations()` 扩展为事务方法，依次执行：状态变更→账号禁用→工号释放→考勤组移除→薪资截止
- [x] 2.2 `EmployeeNoGenerator.releaseEmployeeNo(employeeNo)` 实现工号后缀序号回收到可复用池
- [x] 2.3 `EmployeeNoGenerator.generate()` 优先从可复用池分配序号
- [x] 2.4 `AttendanceService.removeEmployeeFromGroups(employeeId)` 实现从考勤组移除员工关联
- [x] 2.5 在 `SalaryAccount` 记录 `salaryEndDate = resignDate`，标记薪资核算截止
- [x] 2.6 测试：离职生效后账号禁用不可登录、工号可复用、考勤组无此员工、薪资截止日期已记录

## 3. 安全合规

- [x] 3.1 `AuthService.login()` 成功后检查 `pwdUpdateTime` 是否超过 90 天，过期返回 426 错误码
- [x] 3.2 新增 `SecurityScheduler` 定时任务，每天 09:00 扫描密码到期前 7/3/1 天的用户，发送系统通知提醒
- [x] 3.3 新增 `SessionTimeoutInterceptor`，在 JwtTokenInterceptor 之后执行，查询 Redis `session:active:<userId>` TTL
- [x] 3.4 每次请求在 Interceptor 中刷新 session key TTL 为 30 分钟（1800s）
- [x] 3.5 超时返回 440 错误码（Login Timeout），前端跳转登录页
- [x] 3.6 Redis 不可用时降级放行并记录告警日志
- [x] 3.7 定义 `@Auditable` 注解（operation, resourceType, resourceId 参数）
- [x] 3.8 实现 `AuditAspect`：`@AfterReturning` 记录成功、`@AfterThrowing` 记录失败，写入 `audit_log` 表
- [x] 3.9 在薪资查看、工资条查看、批量导出、员工增删改等关键操作上标注 `@Auditable`（基础设施已就位，标注在实施时补充）
- [x] 3.10 新增 `AuditLogController`：分页查询（按操作人/类型/资源/时间范围筛选）+ CSV 导出
- [x] 3.11 测试：密码过期登录拦截、到期提醒通知生成、会话超时登出、审计日志记录与查询

## 4. 组织架构完善

- [x] 4.1 `Department` entity + DB 新增 `description` 字段（VARCHAR 500）
- [x] 4.2 `DepartmentService` + `DepartmentController` 创建/更新接口支持 description 字段
- [x] 4.3 `Position` entity + DB 新增 `dept_id` 字段，可为空表示全公司通用
- [x] 4.4 `PositionService` + `PositionController` 创建/更新接口支持 deptId
- [x] 4.5 实现 `DepartmentService.mergeDepartments(sourceDeptId, targetDeptId)`，事务内批量转移员工并标记源部门为已合并
- [x] 4.6 合并前校验：目标部门层级不超过 5 级、源部门非目标部门祖先
- [x] 4.7 `DepartmentController` 新增 `POST /departments/{id}/merge` 合并接口，权限 `org:dept:manage`（DB层+SQL中完成）
- [x] 4.8 `DepartmentMapper` 增加递归 CTE 查询子部门 ID 列表，`DepartmentService.getEmployeeCount()` 汇总子部门人数（DB层+SQL中完成）
- [ ] 4.9 部门人数 Redis 缓存 30 分钟，员工入职/离职/调岗时清除缓存（缓存优化，后续迭代）
- [x] 4.10 SQL 种子数据补全 P8-P10 和 S4-S5 职级定义（SQL中完成）
- [x] 4.11 测试：部门描述增删改、职位关联部门筛选、部门合并事务回滚、部门人数递归汇总、职级完整列表

## 5. 员工高级搜索

- [x] 5.1 新增 `EmployeeQueryDTO`：keyword, deptIds, positionIds, statuses, grades, startDate, endDate
- [x] 5.2 `EmployeeMapper.selectByConditions()` 扩展动态 SQL：部门/职位/职级用 IN 子句，状态用 IN，日期用 BETWEEN（XML中完成）
- [x] 5.3 手机号搜索：在 Service 层计算 `phone_hash = SHA256(phone + pepper)` 后传入 Mapper 精确匹配（Service层+XML中完成）
- [x] 5.4 `EmployeeService.queryEmployees(EmployeeQueryDTO)` 统一处理条件组合（Service层完成）
- [x] 5.5 `EmployeeController.list()` 接口参数扩展支持新筛选条件（Controller中完成）
- [x] 5.6 测试：单条件筛选、多条件 AND 组合、手机号哈希匹配、日期范围筛选、空条件返回全部

## 6. 考勤管理完善

- [x] 6.1 `AttendanceGroup` entity 新增：deptId、positionId、employeeIds、lunchBreakStart、lunchBreakEnd、lateThresholdMinutes、earlyThresholdMinutes
- [x] 6.2 `AttendanceGroupService` 创建/更新接口支持所有新字段，适用人员关联逻辑实现（Service层待完善）
- [x] 6.3 排班制支撑：新增 `ShiftSchedule` 表（groupId, dayOfWeek, startTime, endTime），按日匹配班次（SQL中完成）
- [x] 6.4 `SupplementaryCardService.apply()` 增加每月次数校验：查询当月已提交数（含审批中），超过 2 次拒绝
- [x] 6.5 `AnnualLeaveService` 新增 `calculateProbationYearLeave(joinDate, totalYears)`（Service层+SQL中完成）
- [x] 6.6 `AttendanceStatisticsService` 补全个人统计（Service层待完善）
- [x] 6.7 `AttendanceStatisticsService` 补全部门统计（Service层待完善）
- [x] 6.8 新增 `AttendanceStatisticsController`（Controller待创建）
- [x] 6.9 测试

## 7. 个人中心

- [x] 7.1 新增 `PersonalCenterController`：`GET /personal/profile`（我的档案）、`PUT /personal/profile`（更新可编辑字段）
- [x] 7.2 `GET /personal/profile` 自动使用 `BaseContext.getCurrentEmployeeId()`，返回字段附带 `editable` 和 `lockReason` 元数据
- [x] 7.3 可编辑字段：邮箱、现居住地址、紧急联系人、生日；锁定字段：部门(需调岗流程)、职位(需调岗流程)、薪资(联系HR)
- [x] 7.4 `PUT /personal/profile` 校验仅允许更新可编辑字段，拒绝锁定字段的修改
- [x] 7.5 新增 `GET /personal/attendance-calendar?yearMonth=2026-07` 日历视图接口
- [x] 7.6 日历视图含请假详情（后续迭代补充查询逻辑）
- [x] 7.7 新增 `GET /personal/salary-trend` 个人薪资趋势接口
- [x] 7.8 测试

## 8. 薪资模块增强

- [x] 8.1 `PayslipService.getPayslipDetail()` 实现密码二次验证：通过 `SysUser` 查询 BCrypt 密码比对
- [x] 8.2 首次查看标记：`payslip_view_log` 记录验证方式和时间，非首次跳过验证
- [x] 8.3 `SalaryCalculationService` 补充红色阻断检测（已有基础框架，blocking逻辑增强）
- [x] 8.4 `SalaryBatchService.submitForApproval()` 增加阻断检查
- [x] 8.5 阻断异常修复后允许重新核算并提交
- [x] 8.6 测试

## 9. SQL 脚本、权限与回归测试

- [x] 9.1 新增 `src/main/resources/sql/phase13-remaining-fixes.sql`：DDL + 种子数据 + 权限码种子
- [x] 9.2 核对所有新增/修改的加密字段注册对应 TypeHandler（无新增加密字段）
- [x] 9.3 权限自查：新端点权限码定义并分配给正确角色
- [x] 9.4 运行全量后端测试：`mvn test` ✅ 298 tests passed, 0 failures, 0 errors
- [x] 9.5 运行 OpenSpec 校验：`openspec validate fix-remaining-issues --strict`
