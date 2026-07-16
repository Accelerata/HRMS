## Why

HRMS 后端已完成 22 个业务模块、100+ 个 REST API 接口的开发，但缺少对应的前端界面。HR 专员、部门主管、财务和普通员工无法通过浏览器访问系统，所有操作依赖 curl/Postman 调用，无法满足实际业务需求。本变更旨在构建完整的 SPA 前端，实现人力资源"入转调离"全流程线上化和数据可视化。

## What Changes

- **全新构建** React + Umi 单页应用项目，TypeScript 严格模式
- 实现 **22 个前端功能模块**，覆盖认证、组织、员工、考勤、异动、审批、薪资、个人中心、审计日志
- 基于 Ant Design 5.x 组件库构建 UI，使用 AntV 实现数据可视化（考勤统计图、薪资趋势图、部门成本分布图、请假类型分布图）
- 基于角色 RBAC 实现菜单/按钮级权限控制和数据范围隔离
- 封装统一请求库（基于 Umi request/axios），统一处理 JWT Token、错误拦截、响应转换
- 实现通用业务组件：`@ant-design/pro-table` 分页表格、表单弹窗、审批操作、状态标签、部门树选择器等
- 薪资模块工资条二次密码验证、敏感字段脱敏展示

## Capabilities

### New Capabilities

- `auth-login`: 用户登录/登出、Token 管理、修改密码
- `employee-management`: 员工档案 CRUD、高级搜索（多条件组合筛选）、分页列表
- `dept-management`: 部门树形展示、CRUD、部门合并
- `position-management`: 职位 CRUD、按序列（M/P/S）筛选
- `attendance-punch`: 上班/下班打卡、打卡记录查询
- `attendance-statistics`: 个人/部门月度考勤统计、AntV 可视化
- `supplementary-card`: 补卡申请/审批/我的补卡记录
- `comp-leave`: 加班折算调休
- `leave-management`: 请假申请（草稿→提交→审批→取消）、天数试算、假期余额查询、请假记录
- `leave-attachment`: 请假附件上传/查看
- `leave-statistics`: 个人/部门请假统计、请假类型分布（AntV 饼图）
- `work-calendar`: 工作日历管理（节假日/调班批量配置）
- `onboarding`: 入职申请全流程（草稿→提交→审批→确认到岗→放弃/撤回）
- `regularization`: 转正申请/审批（通过/延长/辞退）、即将到期提醒列表
- `transfer`: 调岗申请/双审（原部门+新部门）
- `resignation`: 离职申请/审批/交接
- `approval-workbench`: 统一审批工作台（待办/已办）、审批详情、转交、委托
- `salary-calc`: 单人/批量薪资核算、薪资批次管理（提交→审批→发放→归档）
- `salary-account`: 薪资账套 CRUD、调薪、变更历史
- `salary-plan`: 薪资方案管理（方案/工资项/适用范围）
- `salary-report`: 薪资报表 AntV 可视化（月度趋势折线图、部门成本分布图、薪资构成占比图）
- `personal-center`: 个人档案查看编辑、考勤日历、薪资趋势图、我的工资条（二次验证）
- `audit-log`: 审计日志查询/CSV 导出

### Modified Capabilities

<!-- 无现有前端能力需要修改，所有均为新增 -->

## Impact

- **新增项目**: `/hrms-web/` — 基于 Umi 4 的全新前端工程，位于仓库根目录，与后端 `HRMS/` 平级
- **目录关系**:
  ```
  /Users/Zhuanz/Downloads/HRMS/
  ├── HRMS/           ← Spring Boot 后端
  ├── hrms-web/       ← React + Umi 前端（新建）
  └── openspec/       ← 项目管理文档
  ```
- **依赖**: React 18, Umi 4, Ant Design 5, AntV G2, @ant-design/pro-components, Zustand, dayjs, axios
- **后端对接**: 调用现有 `http://localhost:8080/api/v1/*` REST API，需配置 Umi proxy 解决开发环境跨域
- **权限模型**: 前端根据登录返回的 `roleCode` 控制路由和操作按钮可见性，敏感接口后端二次校验
