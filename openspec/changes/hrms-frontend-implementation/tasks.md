## 1. 项目初始化与环境搭建

- [ ] 1.1 使用 `@umijs/max` 创建 `hrms-web` 项目，配置 TypeScript 严格模式
- [ ] 1.2 安装依赖：antd, @ant-design/pro-components, @ant-design/charts (AntV G2), zustand, dayjs, axios
- [ ] 1.3 配置 `config/config.ts`：proxy `/api` → `http://localhost:8080`，开启路由、布局、access 插件
- [ ] 1.4 创建 `config/routes.ts` 完整路由表（含所有 22 模块的菜单路径）
- [ ] 1.5 配置 `.umirc.ts` / `config.ts` 中的主题定制（Ant Design 主题色、布局设置）

## 2. 基础设施层

- [ ] 2.1 封装 `utils/request.ts`：统一请求配置（JWT 拦截器、401 处理、响应解包、错误提示）
- [ ] 2.2 创建 `utils/constants.ts`：定义所有枚举常量（员工状态、假期类型、审批动作、业务类型、批次状态等）
- [ ] 2.3 创建 `models/user.ts`：全局 Zustand store（currentUser、token、login/logout actions）
- [ ] 2.4 实现 `app.tsx` 运行时配置：从 localStorage 恢复 token、权限初始化、登出处理
- [ ] 2.5 实现 `access.ts`：定义 roleCode → 权限映射，控制路由和按钮显隐

## 3. 认证模块 (auth-login)

- [ ] 3.1 实现登录页面 (`pages/User/Login`)：登录表单、错误提示、登录后跳转
- [ ] 3.2 实现 `services/auth.ts`：login、changePassword API
- [ ] 3.3 实现修改密码弹窗

## 4. 布局与通用组件

- [ ] 4.1 配置 Ant Design ProLayout：侧边栏菜单、顶栏用户下拉、面包屑导航
- [ ] 4.2 创建通用 `StatusTag` 组件：统一状态颜色映射（灰/蓝/绿/橙/红）
- [ ] 4.3 创建通用 `SensitiveText` 组件：敏感字段脱敏展示（手机号、身份证、银行账号）
- [ ] 4.4 创建通用 `ApprovalModal` 组件：通过/拒绝/退回 + 评论表单
- [ ] 4.5 创建通用 `DeptTreeSelect` 组件：部门树选择（员工选择器等其他通用选择器按需内嵌）

## 5. 部门管理 (dept-management)

- [ ] 5.1 实现 `services/dept.ts`：getTree、create、update、delete、merge API
- [ ] 5.2 实现部门树页面：Tree 组件 + 右键菜单（新增子部门/编辑/删除/合并）
- [ ] 5.3 实现部门表单弹窗：创建/编辑用 ModalForm
- [ ] 5.4 实现部门合并弹窗：选择目标部门确认

## 6. 职位管理 (position-management)

- [ ] 6.1 实现 `services/position.ts`：getList、getById、create、update、delete API
- [ ] 6.2 实现职位列表页：ProTable + 序列筛选（M/P/S Tab）
- [ ] 6.3 实现职位表单弹窗（positionCode 唯一性校验）

## 7. 员工档案管理 (employee-management)

- [ ] 7.1 实现 `services/employee.ts`：getList、getById、create、update、delete API
- [ ] 7.2 实现员工列表页：ProTable + 高级搜索表单（多选 dept/position/status/grade + 日期范围）
- [ ] 7.3 实现员工详情抽屉：ProDescriptions + SensitiveText 脱敏
- [ ] 7.4 实现员工表单弹窗（创建/编辑共用，字段级权限校验）

## 8. 考勤打卡 (attendance-punch)

- [ ] 8.1 实现 `services/attendance.ts`：punchIn、punchOut、getRecords、getGroups、createGroup、updateGroup、deleteGroup API
- [ ] 8.2 实现打卡页面：打卡按钮、当前时间大屏展示、今日打卡状态
- [ ] 8.3 实现打卡记录页：月度日历视图或列表，状态颜色标记
- [ ] 8.4 实现考勤组管理页：ProTable + CRUD 弹窗

## 9. 考勤统计 (attendance-statistics)

- [ ] 9.1 实现 `services/attendance-statistics.ts`：personal、dept API
- [ ] 9.2 实现个人考勤统计页：StatCard + AntV Gauge 出勤率仪表盘
- [ ] 9.3 实现部门考勤统计页：部门汇总 StatCard + 部门出勤率仪表盘

## 10. 补卡管理 (supplementary-card)

- [ ] 10.1 实现 `services/supplementary-card.ts`：apply、approve、getMy API
- [ ] 10.2 实现补卡申请表单
- [ ] 10.3 实现我的补卡记录列表（状态标签）
- [ ] 10.4 实现补卡审批列表（审批人视图 + ApprovalModal）

## 11. 调休管理 (comp-leave)

- [ ] 11.1 实现 `services/comp-leave.ts`：convert API
- [ ] 11.2 实现加班折算调休触发按钮和结果展示

## 12. 请假管理 (leave-management)

- [ ] 12.1 实现 `services/leave.ts`：calculate、getBalance、initBalance、apply、submit、approve、cancel、getApplications API
- [ ] 12.2 实现请假申请表单：日期+时段选择器、天数试算预览、假期余额展示、交接人选择
- [ ] 12.3 实现请假申请流程（草稿→提交→审批→取消）状态流转
- [ ] 12.4 实现请假记录列表（员工视图）
- [ ] 12.5 实现请假审批列表（审批人视图）
- [ ] 12.6 实现假期余额页面：余额卡片组
- [ ] 12.7 实现年假初始化表单（HR/Admin）

## 13. 请假附件 (leave-attachment)

- [ ] 13.1 实现 `services/leave-attachment.ts`：upload、getAttachments API
- [ ] 13.2 在请假表单中集成 Upload 组件
- [ ] 13.3 在请假详情中展示附件列表和下载

## 14. 请假统计 (leave-statistics)

- [ ] 14.1 实现 `services/leave-statistics.ts`：personal、dept、typeDistribution API
- [ ] 14.2 实现个人请假统计页
- [ ] 14.3 实现部门请假率统计页
- [ ] 14.4 实现请假类型分布 AntV 饼图

## 15. 工作日历 (work-calendar)

- [ ] 15.1 实现 `services/work-calendar.ts`：getCalendar、batchSave、delete API
- [ ] 15.2 实现年度日历视图：节假日/调班颜色标记
- [ ] 15.3 实现批量配置面板：日期多选 + dayType/name 输入 + 批量保存
- [ ] 15.4 实现单日删除功能

## 16. 入职管理 (onboarding)

- [ ] 16.1 实现 `services/onboarding.ts`：getPage、getById、create、draft、update、delete、withdraw、approve、confirmArrival、updateEntryDate、abandon API
- [ ] 16.2 实现入职申请列表页（ProTable + 状态筛选）
- [ ] 16.3 实现入职申请表单（提交/保存草稿两种模式）
- [ ] 16.4 实现入职申请详情 + 完整状态流转（草稿→审批→待入职→已入职/已放弃）
- [ ] 16.5 实现入职审批操作
- [ ] 16.6 实现确认到岗/更新入职日期/放弃入职操作

## 17. 转正管理 (regularization)

- [ ] 17.1 实现 `services/regularization.ts`：getPage、getById、create、approve、getExpiring API
- [ ] 17.2 实现转正申请列表页
- [ ] 17.3 实现转正申请表单
- [ ] 17.4 实现转正审批（3种结果类型：通过/延长/辞退）
- [ ] 17.5 实现即将到期提醒列表

## 18. 调岗管理 (transfer)

- [ ] 18.1 实现 `services/transfer.ts`：getPage、getById、create、approve API
- [ ] 18.2 实现调岗申请列表页
- [ ] 18.3 实现调岗申请表单（自动填充原部门/职位）
- [ ] 18.4 实现双审批状态展示（原部门+新部门）

## 19. 离职管理 (resignation)

- [ ] 19.1 实现 `services/resignation.ts`：getPage、getById、create、approve API
- [ ] 19.2 实现离职申请列表页
- [ ] 19.3 实现离职申请表单（离职类型/交接人/交接说明）
- [ ] 19.4 实现离职审批操作

## 20. 审批工作台 (approval-workbench)

- [ ] 20.1 实现 `services/approval.ts`：getTodo、getDone、getDetail、transfer、createDelegation、deleteDelegation、getMyDelegations API
- [ ] 20.2 实现待办/已办 Tab 切换列表（businessType 图标+标签）
- [ ] 20.3 实现统一审批详情页（业务信息+审批时间线）
- [ ] 20.4 实现审批操作（通过/拒绝）+ 转交按钮
- [ ] 20.5 实现转交弹窗（targetApproverId + reason）
- [ ] 20.6 实现委托管理（我的委托列表 + 新增委托弹窗 + 取消委托）

## 21. 薪资核算 (salary-calc)

- [ ] 21.1 实现 `services/salary.ts`：calculate、batchCalc、getBatches、getBatchRecords、submitBatch、approveBatch、payBatch、archiveBatch、getRecords、getYearly、getPayslips、getPayslipDetail API
- [ ] 21.2 实现单人薪资核算页：选择员工→触发计算→展示薪资明细
- [ ] 21.3 实现批量薪资核算页：选择范围→触发计算→展示处理人数
- [ ] 21.4 实现薪资批次列表页：ProTable + 状态流操作按钮（提交/审批/发放/归档）
- [ ] 21.5 实现批次薪资记录查看
- [ ] 21.6 实现薪资记录查询页（员工筛选 + 年月筛选）
- [ ] 21.7 实现年度薪资记录 AntV 折线图（netPay 趋势）
- [ ] 21.8 实现我的工资条列表页（员工视图）
- [ ] 21.9 实现工资条详情 + 二次密码验证弹窗

## 22. 薪资账套 (salary-account)

- [ ] 22.1 实现 `services/salary-account.ts`：getAccount、getHistory、create、adjust、deactivate API
- [ ] 22.2 实现薪资账套查看页
- [ ] 22.3 实现薪资账套创建/调整表单
- [ ] 22.4 实现薪资变更历史时间线
- [ ] 22.5 实现停用操作

## 23. 薪资方案 (salary-plan)

- [ ] 23.1 实现 `services/salary-plan.ts`：getList、getById、create、update、toggleStatus、getItems、addItem、deleteItem、getScopes、addScope API
- [ ] 23.2 实现方案列表页
- [ ] 23.3 实现方案表单（+ 状态切换）
- [ ] 23.4 实现方案工资项管理（子表 + 增删）
- [ ] 23.5 实现方案适用范围管理（子表 + 新增）

## 24. 薪资报表 (salary-report)

- [ ] 24.1 实现 `services/salary-report.ts`：getTrend、getDeptCost、getComposition API
- [ ] 24.2 实现薪资月度趋势 AntV 双线图（grossTotal + netTotal）
- [ ] 24.3 实现部门成本分布 AntV 饼图
- [ ] 24.4 实现薪资构成 AntV 堆叠图
- [ ] 24.5 实现预警高亮：请假>15天、加班>50小时、薪资波动>30%

## 25. 个人中心 (personal-center)

- [ ] 25.1 实现 `services/personal.ts`：getProfile、updateProfile、getAttendanceCalendar、getSalaryTrend API
- [ ] 25.2 实现个人档案页：ProDescriptions + 字段级编辑 + 锁定提示
- [ ] 25.3 实现个人信息编辑弹窗（仅4个可编辑字段）
- [ ] 25.4 实现考勤日历视图：月度日历 + 状态色块 + 请假 Popover
- [ ] 25.5 实现个人薪资趋势 AntV 折线图

## 26. 审计日志 (audit-log)

- [ ] 26.1 实现 `services/audit-log.ts`：getLogs、exportCSV API
- [ ] 26.2 实现审计日志查询页：ProTable + 操作人/操作类型/资源类型/时间范围筛选
- [ ] 26.3 实现 CSV 导出按钮（下载文件流）

## 27. 联调与收尾

- [ ] 27.1 检查所有模块的权限控制完整性（菜单+按钮+数据范围）
- [ ] 27.2 敏感字段脱敏检查（手机号、身份证、银行账号、薪资）
- [ ] 27.3 统一状态颜色合规性检查（灰/蓝/绿/橙/红规范）
- [ ] 27.4 表格/表单/弹窗/抽屉样式一致性 review
- [ ] 27.5 关闭 Mock、配置真实 API 代理、全流程端到端测试
- [ ] 27.6 构建生产版本（`npm run build`）并验证产物
