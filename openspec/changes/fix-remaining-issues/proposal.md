## Why

后端需求完成情况检查报告（2026-07-16）显示核心业务逻辑已完成约 85%，但多个模块仍存在安全漏洞和功能缺口。其中最严重的是：敏感字段（身份证号/薪资/银行卡号）对所有角色无差别返回，违反需求 2.3 字段级权限定义；工资条二次验证为 TODO 存根；密码过期和会话超时机制完全缺失。此外，组织架构、考勤管理、个人中心等模块存在字段遗漏和功能不完整的问题。本次变更旨在系统性修复所有已知缺口，使后端代码与需求文档完全对齐。

## What Changes

按优先级分三批修复，涵盖 6 个新能力和 2 个已有能力的修改：

**安全加固（高优先级）：**
- 实现 `EmployeeService.toVO()` 字段级权限过滤：部门主管不可见身份证号/薪资/银行卡号/紧急联系人，普通员工仅可见本人敏感字段
- 实现工资条首次查看密码二次验证（PASSWORD），完成 `// TODO: 验证密码`
- 新增密码 90 天过期强制更换：登录拦截 + 定时任务提醒
- 新增 30 分钟无操作自动登出：JWT Token 增加 inactivity 超时机制（Redis 记录最后活跃时间）

**功能补全（中优先级）：**
- 离职生效处理补全：工号标记释放、从考勤组移除、薪资计算至离职日期、敏感信息脱敏实现
- 员工高级搜索扩展：部门树多选、职位多选、职级多选、入职日期范围、手机号哈希匹配
- 薪资核算红色阻断检测：新入职员工未设置薪资档案时阻断批次提交
- 个人中心日历视图端点：按日汇总考勤状态（出勤/请假/迟到/缺卡/旷工）

**完善增强（低优先级）：**
- 部门增加 description 字段 + 部门合并与员工转移功能 + 部门人数递归汇总子部门
- 职位增加所属部门（deptId）字段 + 职级对照补全 P8-P10、S4-S5
- 考勤组增加适用人员（deptId/positionId/employeeIds）、排班制、中午休息、迟到/早退阈值字段
- 补卡申请增加每月最多 2 次限制
- 年假当年入职按剩余月份折算
- 考勤统计补齐个人维度（应出勤天数/实际出勤天数/迟到次数/旷工天数/加班时长）和部门维度（出勤率/迟到率）
- 个人中心"我的档案"端点 + 可编辑/锁定字段区分逻辑
- 薪资趋势接口改为个人维度（当前为全局聚合）
- 系统级 AuditLog（操作日志审计）+ 批量导出审计

## Capabilities

### New Capabilities

- `field-level-permission`: 字段级权限过滤 —— EmployeeService.toVO() 根据 DataScope 角色过滤敏感字段返回值，ResignationService.getMaskedDetail() 实现脱敏逻辑
- `org-structure-enhancement`: 组织架构完善 —— 部门描述字段、部门合并与员工转移、部门人数递归汇总；职位所属部门字段、职级对照补全
- `advanced-employee-search`: 员工高级搜索 —— 部门树多选、职位多选、职级多选、入职日期范围、手机号哈希精确匹配
- `attendance-enhancement`: 考勤管理完善 —— 考勤组字段补全（适用人员/排班制/中午休息/迟到早退阈值）、补卡每月 2 次限制、年假入职折算、考勤统计个人与部门维度指标
- `personal-center`: 个人中心 —— 我的档案端点（含可编辑/锁定字段区分）、日历视图数据接口、薪资趋势个人维度
- `security-compliance`: 安全合规 —— 90 天密码过期强制更换、30 分钟无操作自动登出、系统级 AuditLog 操作审计

### Modified Capabilities

- `resignation-flow`: 离职生效处理补全 —— 工号标记释放、从考勤组移除、薪资计算至离职日期、getMaskedDetail 脱敏实现
- `salary-management-backend`: 薪资模块增强 —— 工资条二次验证密码校验实现、新员工无档案红色阻断检测

## Impact

- **Entity 层**：`Department` 新增 `description`；`Position` 新增 `deptId`；`AttendanceGroup` 新增 `deptId/positionId/employeeIds/shiftType/lunchBreak/lateThreshold/earlyThreshold`；新增 `AuditLog`、`DepartmentMergeLog` 实体
- **Mapper 层**：`EmployeeMapper` 增加高级搜索多条件查询；`DepartmentMapper` 增加递归子部门汇总查询、合并转移；`AttendanceGroupMapper` 字段扩展；新增 `AuditLogMapper`
- **Service 层**：`EmployeeService.toVO()` 增加字段级过滤逻辑；`ResignationService` 补全 4 项生效处理 + 脱敏；`AttendanceGroupService` 字段扩展 + 适用人员关联；`SupplementaryCardService` 增加每月 2 次限制；`AnnualLeaveService` 增加入职折算；`AttendanceStatisticsService` 补全统计维度；`PayslipService` 实现密码二次验证；`SysUserService` 增加密码过期校验与会话超时校验
- **Controller 层**：`EmployeeController` 高级搜索参数扩展；`DepartmentController` 增加合并接口；`PersonalCenterController`（新）我的档案/日历视图/个人薪资趋势；`AuditLogController`（新）审计日志查询与导出
- **安全层**：`SecurityFilter` 或 `AuthInterceptor` 增加 inactivity 超时校验（Redis）；`LoginService` 增加密码过期拦截与重定向
- **数据库**：新增 `sql/phase13-remaining-fixes.sql` 增量脚本，包含 DDL 变更和种子数据补全
- **权限**：核对所有新增端点的权限码定义与角色分配；`field-level-permission` 依赖现有 `DataScopeEnum`
- **测试**：字段过滤、离职处理、高级搜索、补卡限制、年假折算、密码过期、会话超时、审计日志的单元测试
- **非目标（Non-goals）**：前端页面实现；短信验证码通道接入（本期仅实现密码验证）；部门合并前端交互
