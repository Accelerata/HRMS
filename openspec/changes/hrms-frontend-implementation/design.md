## Context

HRMS 后端（`HRMS/`）基于 Spring Boot 构建，已实现 22 个业务模块的 REST API（Base URL: `/api/v1`），统一返回 `Result<T>` / `PageResult<T>` 格式。前端项目 `hrms-web/` 独立于后端目录，与 `HRMS/` 平级放置。

**约束**:
- 前端框架 React + Umi 4
- UI 库 Ant Design 5 + @ant-design/pro-components
- 图表库 AntV G2
- 状态管理 Zustand
- 需对接 JWT Bearer Token 认证
- 需按 RBAC 控制菜单和按钮权限 (@/roles: ADMIN, HR, MANAGER, FINANCE, EMPLOYEE)
- 敏感字段脱敏展示（身份证、手机号、银行账号、薪资）
- 薪资模块工资条需二次密码验证

## Goals / Non-Goals

**Goals:**
- 实现全部 22 个业务模块的前端页面，覆盖 HRMS 全流程
- 提供一致的用户体验和交互模式（ProTable 分页 + 弹窗表单 + 状态标签）
- 基于角色的路由守卫和按钮级权限控制
- 覆盖核心薪资/考勤模块的数据可视化（AntV）
- 开发环境的 API 代理、Token 自动注入、统一错误处理
- 前后端分离架构，独立部署

**Non-Goals:**
- 不实现后端 API（已有）
- 不在此版本实现国际化 i18n
- 不在此版本实现 PWA/离线能力
- 不在此版本实现 E2E 自动化测试
- 不在此版本实现移动端/小程序适配

## Decisions

### 1. 项目框架: Umi 4 + Ant Design 5

- **选择**: `@umijs/max` (Umi Max) 作为脚手架
- **原因**: Umi 内置路由、构建、代理、Mock 等能力，与 Ant Design 深度集成；Umi Max 提供即开即用的布局、权限、数据流等插件
- **替代项**: Vite + React Router — 更轻量但需手动集成布局/权限/代理，开发效率较低

### 2. 状态管理: Zustand

- **选择**: 全局用户状态 + 各模块局部状态
- **原因**: 比 Redux 轻量但功能完备；用于存储 `currentUser`（角色/权限/token）和全局通知
- **模块级状态**: 大部分 CRUD 页面的列表/筛选/分页状态由 ProTable 自行管理，不放入全局 store

### 3. 项目结构: 按功能模块分层

> 前端项目位于仓库根目录 `/hrms-web/`，与后端 `/HRMS/` 平级独立部署。

```
hrms-web/                          # 仓库根目录：/Users/Zhuanz/Downloads/HRMS/hrms-web/
├── src/
│   ├── app.tsx                 # 运行时配置（布局/权限/请求）
│   ├── access.ts               # 权限定义
│   ├── global.less             # 全局样式
│   ├── services/               # API 请求层（按模块拆分）
│   │   ├── auth.ts
│   │   ├── employee.ts
│   │   ├── attendance.ts
│   │   └── ...
│   ├── pages/                  # 页面（按模块组织）
│   │   ├── Dashboard/          # 首页工作台
│   │   ├── Employee/           # 员工档案
│   │   ├── Organization/       # 部门 + 职位
│   │   ├── Attendance/         # 考勤打卡/统计/补卡/调休
│   │   ├── Leave/              # 请假+附件+统计
│   │   ├── Onboarding/         # 入职管理
│   │   ├── Regularization/     # 转正管理
│   │   ├── Transfer/           # 调岗管理
│   │   ├── Resignation/        # 离职管理
│   │   ├── Approval/           # 审批工作台
│   │   ├── Salary/             # 薪资核算/批次/账套/方案/报表
│   │   ├── Personal/           # 个人中心
│   │   ├── Audit/              # 审计日志
│   │   └── User/               # 登录/修改密码
│   ├── components/             # 通用业务组件
│   │   ├── DeptTreeSelect/     # 部门树选择器
│   │   ├── PositionSelect/     # 职位选择器
│   │   ├── EmployeeSelect/     # 员工选择器
│   │   ├── StatusTag/          # 状态标签
│   │   ├── ApprovalActions/    # 审批操作按钮组
│   │   └── SensitiveText/      # 敏感信息脱敏展示
│   ├── utils/                  # 工具函数
│   │   ├── request.ts          # 统一请求配置
│   │   └── constants.ts        # 枚举常量
│   └── models/                 # 全局 Zustand store
│       └── user.ts
├── config/
│   ├── config.ts               # Umi 配置
│   └── routes.ts               # 路由配置
└── package.json
```

### 4. API 请求层: 统一封装 Umi request

- **Token 自动注入**: 请求拦截器从 localStorage 读取 token 添加到 Authorization Header
- **401 处理**: 响应拦截器捕获 401 → 清除 token → 跳转登录页
- **响应解包**: 自动提取 `response.data`，非 `code=1` 时弹出 message.error
- **代理配置**: 开发环境 `/api` 代理到 `http://localhost:8080`

### 5. 权限控制方案

- **路由权限**: `access.ts` 根据 `roleCode` 控制菜单可见性。ADMIN 全部可见，HR 除审计日志外全部可见，MANAGER 仅本部门数据，FINANCE 仅薪资相关，EMPLOYEE 仅个人数据
- **按钮权限**: 通过 `access` 控制表格操作列的编辑/删除/审批按钮显隐
- **敏感字段**: 身份证/手机号/银行卡号/薪资字段，前端展示时脱敏（中间 **遮盖**），仅在编辑弹窗中完整展示

### 6. 组件复用策略

- **通用表格页**: 使用 `@ant-design/pro-table` 统一处理分页、排序、筛选
- **通用表单弹窗**: 使用 `@ant-design/pro-form` + `ModalForm` 处理增/改
- **通用详情抽屉**: 使用 `@ant-design/pro-descriptions` + `Drawer` 处理详情
- **通用审批弹窗**: 封装 `ApprovalModal` 组件，支持通过/拒绝/退回三种动作

### 7. 数据可视化 (AntV G2)

- 考勤统计: 月度出勤率仪表盘 + 迟到/早退/缺勤趋势折线图
- 薪资趋势: 月度薪资总额/人均折线图
- 部门成本分布: 饼图或矩形树图
- 薪资构成占比: 堆叠柱状图或南丁格尔玫瑰图
- 请假类型分布: 饼图

### 8. Mock 数据策略

- 开发阶段使用 Umi 内置 Mock 功能（`mock/` 目录）
- Mock 数据模拟 `Result<T>` / `PageResult<T>` 返回格式
- 后端就绪后通过删除 Mock 文件或关闭 Mock 切换到真实 API

## Risks / Trade-offs

- **[风险] 23 个模块同时开发可能导致代码一致性差** → 先建立 3-5 个典型 CRUD 模块确立模式，然后复制模式批量完成其余模块
- **[风险] Umi 4 可能在构建/路由方面存在未知兼容问题** → 前期仅使用稳定插件（`@umijs/max`），避免自定义 Webpack 配置
- **[风险] 权限前端控制可能被绕过** → 前端权限仅用于 UI 控制，所有敏感操作必须由后端二次校验
- **[权衡] 选择了 Umi 而非 Vite** → Umi 功能更完整（内置路由/布局/权限/代理）但构建速度不如 Vite。考虑到团队需要快速交付全功能系统，选择 Umi 以换取更高的开发效率
- **[权衡] 按模块而非按类型组织目录** → 每个功能模块的 pages/services 放在一起，比按类型（all-pages/all-services）更容易维护和迁移
