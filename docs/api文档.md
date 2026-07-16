# HRMS 人力资源管理系统 API 文档

> 统一返回格式：`Result<T>`  
> - `code`: 1 = 成功, 0 = 失败  
> - `msg`: 错误信息  
> - `data`: 响应数据  
>  
> 分页返回格式：`PageResult<T>`  
> - `list`: 数据列表  
> - `total`: 总记录数  
> - `page`: 当前页码  
> - `size`: 每页大小  
>  
> Base URL: `/api/v1`

---

## 目录

1. [认证模块](#1-认证模块)
2. [员工档案管理](#2-员工档案管理)
3. [部门管理](#3-部门管理)
4. [职位管理](#4-职位管理)
5. [考勤管理](#5-考勤管理)
6. [考勤统计](#6-考勤统计)
7. [补卡管理](#7-补卡管理)
8. [调休管理](#8-调休管理)
9. [请假管理](#9-请假管理)
10. [请假附件](#10-请假附件)
11. [请假统计](#11-请假统计)
12. [工作日历](#12-工作日历)
13. [入职管理](#13-入职管理)
14. [转正管理](#14-转正管理)
15. [调岗管理](#15-调岗管理)
16. [离职管理](#16-离职管理)
17. [审批工作台](#17-审批工作台)
18. [薪资管理](#18-薪资管理)
19. [薪资账套](#19-薪资账套)
20. [薪资方案](#20-薪资方案)
21. [个人中心](#21-个人中心)
22. [审计日志](#22-审计日志)

---

## 1. 认证模块

### 1.1 用户登录

- **接口**: `POST /api/v1/auth/login`
- **权限**: 无（公开接口）
- **请求体**:

```json
{
  "username": "admin",
  "password": "123456",
  "captcha": ""
}
```

- **响应**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "roleCode": "ADMIN",
    "employeeId": 1
  }
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

---

### 1.2 修改密码

- **接口**: `PUT /api/v1/auth/change-password`
- **权限**: 登录用户
- **请求体**:

```json
{
  "oldPassword": "123456",
  "newPassword": "newPass@2026"
}
```

- **响应**:

```json
{
  "code": 1,
  "msg": null,
  "data": null
}
```

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"oldPassword":"123456","newPassword":"newPass@2026"}'
```

---

## 2. 员工档案管理

### 2.1 分页查询员工列表

- **接口**: `GET /api/v1/employee/list`
- **权限**: `emp:view`
- **Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| deptId | Long | 否 | 部门ID（兼容旧参数） |
| status | Integer | 否 | 状态（兼容旧参数） |
| keyword | String | 否 | 关键词搜索（姓名/工号） |
| phone | String | 否 | 手机号精确搜索 |
| deptIds | List\<Long\> | 否 | 部门ID列表（高级搜索） |
| positionIds | List\<Long\> | 否 | 职位ID列表 |
| statuses | List\<Integer\> | 否 | 状态列表 |
| grades | List\<String\> | 否 | 职级列表 |
| startDate | LocalDate | 否 | 入职开始日期 |
| endDate | LocalDate | 否 | 入职结束日期 |
| page | int | 否 | 页码（默认1） |
| size | int | 否 | 每页大小（默认10） |

- **响应**:

```json
{
  "code": 1,
  "data": {
    "list": [
      {
        "id": 1,
        "employeeNo": "EMP20260001",
        "name": "张三",
        "phone": "138****5678",
        "email": "zhangsan@hrms.com",
        "deptName": "技术部",
        "positionName": "高级Java开发",
        "grade": "P3",
        "status": 1,
        "entryDate": "2026-01-15"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/employee/list?keyword=张三&page=1&size=10" \
  -H "Authorization: Bearer <token>"
```

---

### 2.2 查看员工详情

- **接口**: `GET /api/v1/employee/{id}`
- **权限**: `emp:view`
- **路径参数**: `id` - 员工ID

- **响应**:

```json
{
  "code": 1,
  "data": {
    "id": 1,
    "employeeNo": "EMP20260001",
    "name": "张三",
    "phone": "13800138000",
    "email": "zhangsan@hrms.com",
    "idCard": "3201**********1234",
    "gender": 1,
    "birthday": "1995-06-15",
    "deptId": 1,
    "deptName": "技术部",
    "positionId": 3,
    "positionName": "高级Java开发",
    "grade": "P3",
    "reportTo": 5,
    "workLocation": "北京",
    "entryType": 1,
    "entryDate": "2026-01-15",
    "status": 1,
    "baseSalary": 25000.00,
    "bankAccount": "6222****1234",
    "bankName": "招商银行"
  }
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/employee/1 \
  -H "Authorization: Bearer <token>"
```

---

### 2.3 创建员工档案

- **接口**: `POST /api/v1/employee`
- **权限**: `emp:create`
- **请求体**:

```json
{
  "name": "李四",
  "phone": "13912345678",
  "email": "lisi@hrms.com",
  "idCard": "320102199808151234",
  "gender": 1,
  "birthday": "1998-08-15",
  "registeredAddress": "江苏省南京市鼓楼区xx路xx号",
  "currentAddress": "北京市朝阳区xx小区xx栋",
  "deptId": 2,
  "positionId": 5,
  "grade": "P2",
  "reportTo": 1,
  "workLocation": "北京",
  "entryType": 1,
  "entryDate": "2026-07-16",
  "salaryAccountId": 1,
  "baseSalary": 15000.00,
  "bankAccount": "6222021234567890",
  "bankName": "工商银行",
  "status": 1
}
```

- **响应**:

```json
{
  "code": 1,
  "data": {
    "id": 2,
    "employeeNo": "EMP20260002",
    "name": "李四",
    "...": "..."
  }
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/employee \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name":"李四",
    "phone":"13912345678",
    "email":"lisi@hrms.com",
    "idCard":"320102199808151234",
    "gender":1,
    "deptId":2,
    "positionId":5,
    "grade":"P2",
    "reportTo":1,
    "workLocation":"北京",
    "entryType":1,
    "entryDate":"2026-07-16",
    "baseSalary":15000.00,
    "bankAccount":"6222021234567890",
    "bankName":"工商银行",
    "status":1
  }'
```

---

### 2.4 更新员工档案

- **接口**: `PUT /api/v1/employee/{id}`
- **权限**: `emp:edit`
- **路径参数**: `id` - 员工ID

- **请求体**: 同 [2.3 创建员工档案](#23-创建员工档案)

- **响应**: 同 [2.2 查看员工详情](#22-查看员工详情)

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/employee/2 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "id":2,
    "name":"李四(已更新)",
    "phone":"13912345678",
    "email":"lisi_new@hrms.com",
    "idCard":"320102199808151234",
    "gender":1,
    "deptId":2,
    "positionId":5,
    "grade":"P3",
    "entryType":1,
    "entryDate":"2026-07-16"
  }'
```

---

### 2.5 删除员工档案

- **接口**: `DELETE /api/v1/employee/{id}`
- **权限**: `emp:delete`
- **路径参数**: `id` - 员工ID

- **响应**:

```json
{
  "code": 1,
  "msg": null,
  "data": null
}
```

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/employee/2 \
  -H "Authorization: Bearer <token>"
```

---

## 3. 部门管理

### 3.1 获取部门树

- **接口**: `GET /api/v1/dept/tree`
- **权限**: `org:dept:view`
- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "deptName": "总公司",
      "deptCode": "HQ",
      "parentId": 0,
      "managerId": 1,
      "managerName": "王总",
      "sortOrder": 1,
      "status": 1,
      "employeeCount": 150,
      "children": [
        {
          "id": 2,
          "deptName": "技术部",
          "deptCode": "TECH",
          "parentId": 1,
          "managerId": 5,
          "managerName": "赵经理",
          "sortOrder": 1,
          "status": 1,
          "employeeCount": 45,
          "children": []
        }
      ]
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/dept/tree \
  -H "Authorization: Bearer <token>"
```

---

### 3.2 创建部门

- **接口**: `POST /api/v1/dept`
- **权限**: `org:dept:manage`
- **请求体**:

```json
{
  "parentId": 1,
  "deptName": "产品部",
  "deptCode": "PROD",
  "sortOrder": 3,
  "managerId": 10,
  "status": 1
}
```

- **响应**:

```json
{
  "code": 1,
  "data": null
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/dept \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"parentId":1,"deptName":"产品部","deptCode":"PROD","sortOrder":3,"managerId":10,"status":1}'
```

---

### 3.3 更新部门

- **接口**: `PUT /api/v1/dept/{id}`
- **权限**: `org:dept:manage`
- **路径参数**: `id` - 部门ID

- **请求体**: 同 [3.2 创建部门](#32-创建部门)

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/dept/3 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id":3,"parentId":1,"deptName":"产品研发部","deptCode":"PROD","sortOrder":3,"managerId":10,"status":1}'
```

---

### 3.4 删除部门

- **接口**: `DELETE /api/v1/dept/{id}`
- **权限**: `org:dept:manage`
- **路径参数**: `id` - 部门ID

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/dept/3 \
  -H "Authorization: Bearer <token>"
```

---

### 3.5 部门合并

- **接口**: `POST /api/v1/dept/{id}/merge`
- **权限**: `org:dept:manage`
- **路径参数**: `id` - 源部门ID
- **Query 参数**: `targetDeptId` - 目标部门ID

- **测试数据**:

```bash
curl -X POST "http://localhost:8080/api/v1/dept/4/merge?targetDeptId=2" \
  -H "Authorization: Bearer <token>"
```

---

## 4. 职位管理

### 4.1 查询职位列表

- **接口**: `GET /api/v1/position/list`
- **权限**: `org:position:view`
- **Query 参数**: `sequence` - 序列筛选（M/P/S，可选）

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "positionName": "Java开发工程师",
      "positionCode": "P3",
      "sequence": "P",
      "gradeRange": "P1-P5",
      "defaultProbationMonths": 3,
      "deptId": null,
      "description": "软件开发高级岗",
      "isStandard": 1,
      "status": 1
    }
  ]
}
```

- **测试数据**:

```bash
# 查询全部
curl -X GET http://localhost:8080/api/v1/position/list \
  -H "Authorization: Bearer <token>"

# 按序列筛选
curl -X GET "http://localhost:8080/api/v1/position/list?sequence=P" \
  -H "Authorization: Bearer <token>"
```

---

### 4.2 根据ID查询职位

- **接口**: `GET /api/v1/position/{id}`
- **权限**: `org:position:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/position/1 \
  -H "Authorization: Bearer <token>"
```

---

### 4.3 创建职位

- **接口**: `POST /api/v1/position`
- **权限**: `org:position:manage`
- **请求体**:

```json
{
  "positionName": "前端开发工程师",
  "positionCode": "P2",
  "sequence": "P",
  "gradeRange": "P1-P4",
  "defaultProbationMonths": 3,
  "deptId": null,
  "description": "前端开发岗位",
  "isStandard": 1,
  "status": 1
}
```

> `positionCode` 必填且全局唯一；`sequence` 必填，仅限 M/P/S；`isStandard` 默认 1（标准职位），设为 0 时入职需 HR 二审

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/position \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"positionName":"前端开发工程师","positionCode":"P2","sequence":"P","gradeRange":"P1-P4","defaultProbationMonths":3,"deptId":null,"description":"前端开发岗位","isStandard":1,"status":1}'
```

---

### 4.4 更新职位

- **接口**: `PUT /api/v1/position/{id}`
- **权限**: `org:position:manage`

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/position/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"positionName":"高级Java开发工程师","positionCode":"P4","sequence":"P","gradeRange":"P2-P5","defaultProbationMonths":3,"isStandard":1,"status":1}'
```

---

### 4.5 删除职位

- **接口**: `DELETE /api/v1/position/{id}`
- **权限**: `org:position:manage`

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/position/1 \
  -H "Authorization: Bearer <token>"
```

---

## 5. 考勤管理

### 5.1 上班打卡

- **接口**: `POST /api/v1/attendance/punch-in`
- **权限**: `att:record:punch`
- **请求体**:

```json
{
  "employeeId": 1,
  "groupId": 1,
  "punchTime": "09:00:00"
}
```

- **响应**:

```json
{
  "code": 1,
  "data": {
    "id": 1001,
    "employeeId": 1,
    "employeeName": "张三",
    "groupId": 1,
    "punchDate": "2026-07-16",
    "punchInTime": "09:00:00",
    "punchOutTime": null,
    "status": "NORMAL",
    "lateMinutes": 0
  }
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/attendance/punch-in \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"employeeId":1,"groupId":1,"punchTime":"09:00:00"}'
```

---

### 5.2 下班打卡

- **接口**: `POST /api/v1/attendance/punch-out`
- **权限**: `att:record:punch`
- **请求体**:

```json
{
  "employeeId": 1,
  "groupId": 1,
  "punchTime": "18:00:00"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/attendance/punch-out \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"employeeId":1,"groupId":1,"punchTime":"18:00:00"}'
```

---

### 5.3 查询员工打卡记录

- **接口**: `GET /api/v1/attendance/records/{employeeId}`
- **权限**: `att:record:view`
- **路径参数**: `employeeId` - 员工ID
- **Query 参数**: `page`（默认1）, `size`（默认31）

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/attendance/records/1?page=1&size=31" \
  -H "Authorization: Bearer <token>"
```

---

### 5.4 考勤组列表

- **接口**: `GET /api/v1/attendance/groups`
- **权限**: `att:group:view`

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "groupName": "固定班制-标准",
      "groupType": 1,
      "startTime": "09:00:00",
      "endTime": "18:00:00",
      "flexThreshold": 30,
      "absentHalfDayThreshold": 120,
      "lunchBreakStart": "12:00:00",
      "lunchBreakEnd": "13:00:00",
      "lateThresholdMinutes": 15,
      "earlyThresholdMinutes": 15,
      "status": 1
    }
  ]
}
```

> `groupType`: 1=固定班, 2=弹性班, 3=排班制

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/attendance/groups \
  -H "Authorization: Bearer <token>"
```

---

### 5.5 创建考勤组

- **接口**: `POST /api/v1/attendance/groups`
- **权限**: `att:group:manage`
- **请求体**:

```json
{
  "groupName": "弹性班制",
  "groupType": 2,
  "startTime": "09:30:00",
  "endTime": "18:30:00",
  "flexThreshold": 30,
  "absentHalfDayThreshold": 120,
  "lateThresholdMinutes": 15,
  "earlyThresholdMinutes": 15
}
```

> `groupType`、`startTime`、`endTime` 必填；`flexThreshold` 弹性窗口（分钟）；`absentHalfDayThreshold` 默认 120（超过视为旷工半天）

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/attendance/groups \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"groupName":"弹性班制","groupType":2,"startTime":"09:30:00","endTime":"18:30:00","flexThreshold":30,"absentHalfDayThreshold":120,"lateThresholdMinutes":15,"earlyThresholdMinutes":15}'
```

---

### 5.6 更新考勤组

- **接口**: `PUT /api/v1/attendance/groups/{id}`
- **权限**: `att:group:manage`

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/attendance/groups/2 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"groupName":"弹性班制(修订)","groupType":2,"startTime":"09:00:00","endTime":"18:00:00","flexThreshold":60,"absentHalfDayThreshold":90,"lateThresholdMinutes":15,"earlyThresholdMinutes":15}'
```

---

### 5.7 删除考勤组

- **接口**: `DELETE /api/v1/attendance/groups/{id}`
- **权限**: `att:group:manage`

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/attendance/groups/2 \
  -H "Authorization: Bearer <token>"
```

---

## 6. 考勤统计

### 6.1 个人月度考勤统计

- **接口**: `GET /api/v1/attendance-statistics/personal`
- **权限**: `emp:attendance:view`
- **Query 参数**: `employeeId`, `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": {
    "employeeId": 1,
    "year": 2026,
    "month": 7,
    "totalWorkDays": 23,
    "normalDays": 20,
    "lateDays": 2,
    "earlyDays": 0,
    "absentHalfDays": 1,
    "missingPunchDays": 0,
    "attendanceRate": 0.9565
  }
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/attendance-statistics/personal?employeeId=1&year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

### 6.2 部门月度考勤统计

- **接口**: `GET /api/v1/attendance-statistics/dept`
- **权限**: `emp:attendance:view`
- **Query 参数**: `deptId`, `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": {
    "deptId": 2,
    "year": 2026,
    "month": 7,
    "totalEmployees": 45,
    "recordedEmployeeCount": 43,
    "normalDays": 850,
    "lateDays": 30,
    "earlyDays": 10,
    "absentHalfDays": 15,
    "missingPunchDays": 8,
    "deptAttendanceRate": 0.9318
  }
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/attendance-statistics/dept?deptId=2&year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

## 7. 补卡管理

### 7.1 发起补卡申请

- **接口**: `POST /api/v1/supplementary-card/apply`
- **权限**: `att:card:apply`
- **说明**: `employeeId` 从当前登录 token 自动获取，无需手动传
- **请求体**:

```json
{
  "attendanceDate": "2026-07-15",
  "cardType": 1,
  "supplementTime": "09:05:00",
  "reason": "忘记打卡，实际已按时到岗"
}
```

> `cardType`: 1=上班卡, 2=下班卡；`supplementTime` 格式 HH:mm:ss

- **响应**:

```json
{
  "code": 1,
  "data": {
    "id": 101,
    "employeeId": 1,
    "attendanceDate": "2026-07-15",
    "cardType": 1,
    "supplementTime": "09:05:00",
    "reason": "忘记打卡，实际已按时到岗",
    "status": 0,
    "createTime": "2026-07-16T10:00:00"
  }
}
```

> `status`: 0=草稿, 1=审批中, 2=已通过, 3=已拒绝

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/supplementary-card/apply \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"attendanceDate":"2026-07-15","cardType":1,"supplementTime":"09:05:00","reason":"忘记打卡，实际已按时到岗"}'
```

---

### 7.2 审批补卡申请

- **接口**: `POST /api/v1/supplementary-card/{id}/approve`
- **权限**: `att:card:approve`
- **请求体**:

```json
{
  "action": 1,
  "comment": "确认属实，同意补卡"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/supplementary-card/101/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"comment":"确认属实，同意补卡"}'
```

---

### 7.3 查询我的补卡申请

- **接口**: `GET /api/v1/supplementary-card/my`
- **权限**: `att:card:apply`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/supplementary-card/my \
  -H "Authorization: Bearer <token>"
```

---

## 8. 调休管理

### 8.1 手动触发加班折算调休

- **接口**: `POST /api/v1/comp-leave/convert/{employeeId}`
- **权限**: `att:record:manage`
- **路径参数**: `employeeId` - 员工ID

- **响应**:

```json
{
  "code": 1,
  "data": 2.5
}
```

> 返回折算的调休天数（BigDecimal）

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/comp-leave/convert/1 \
  -H "Authorization: Bearer <token>"
```

---

## 9. 请假管理

### 9.1 天数试算预览

- **接口**: `GET /api/v1/leave/days/calculate`
- **权限**: `att:leave:apply`
- **Query 参数**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| startDate | String | - | 开始日期（yyyy-MM-dd） |
| startPeriod | int | 0 | 开始时段：0=上午, 1=下午 |
| endDate | String | - | 结束日期 |
| endPeriod | int | 1 | 结束时段：0=上午, 1=下午 |

- **响应**:

```json
{
  "code": 1,
  "data": 3.5
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/leave/days/calculate?startDate=2026-07-20&startPeriod=0&endDate=2026-07-23&endPeriod=1" \
  -H "Authorization: Bearer <token>"
```

---

### 9.2 查询员工假期余额

- **接口**: `GET /api/v1/leave/balance/{employeeId}`
- **权限**: `att:leave:view`
- **路径参数**: `employeeId` - 员工ID
- **Query 参数**: `year`（默认2026）

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "employeeId": 1,
      "leaveType": 1,
      "leaveTypeName": "年假",
      "totalDays": 10,
      "usedDays": 3,
      "remainingDays": 7,
      "year": 2026
    },
    {
      "id": 2,
      "employeeId": 1,
      "leaveType": 2,
      "leaveTypeName": "调休",
      "totalDays": 5,
      "usedDays": 0,
      "remainingDays": 5,
      "year": 2026
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/leave/balance/1?year=2026" \
  -H "Authorization: Bearer <token>"
```

---

### 9.3 初始化年假余额

- **接口**: `POST /api/v1/leave/balance/annual/init`
- **权限**: `att:record:manage`
- **Query 参数**: `employeeId`, `entryDate`, `year`（默认2026）

- **测试数据**:

```bash
curl -X POST "http://localhost:8080/api/v1/leave/balance/annual/init?employeeId=2&entryDate=2026-07-16&year=2026" \
  -H "Authorization: Bearer <token>"
```

---

### 9.4 创建请假申请（草稿）

- **接口**: `POST /api/v1/leave/apply`
- **权限**: `att:leave:apply`
- **请求体**:

```json
{
  "leaveType": 1,
  "startDate": "2026-07-20",
  "endDate": "2026-07-22",
  "startPeriod": 0,
  "endPeriod": 1,
  "reason": "个人年假休息",
  "handoverTo": 20
}
```

> `employeeId` 可选，默认取当前登录用户关联的员工；`leaveType`: 1=年假, 2=调休, 3=事假, 4=病假, 5=婚假, 6=产假, 7=丧假

- **响应**:

```json
{
  "code": 1,
  "data": {
    "id": 201,
    "employeeId": 1,
    "employeeName": "张三",
    "leaveType": 1,
    "startDate": "2026-07-20",
    "endDate": "2026-07-22",
    "days": 2.5,
    "reason": "个人年假休息",
    "status": 0,
    "createTime": "2026-07-16T10:30:00"
  }
}
```

- **测试数据**:

```bash
# 不传 employeeId（自动取当前用户）
curl -X POST http://localhost:8080/api/v1/leave/apply \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"leaveType":1,"startDate":"2026-07-20","endDate":"2026-07-22","startPeriod":0,"endPeriod":1,"reason":"个人年假休息","handoverTo":20}'
```

---

### 9.5 提交请假申请

- **接口**: `POST /api/v1/leave/{id}/submit`
- **权限**: `att:leave:apply`
- **路径参数**: `id` - 请假申请ID（由上一步 9.4 创建后返回）

- **测试数据**:

```bash
# {id} 替换为 9.4 创建成功后返回的申请ID
curl -X POST http://localhost:8080/api/v1/leave/{id}/submit \
  -H "Authorization: Bearer <token>"
```

---

### 9.6 审批请假申请

- **接口**: `POST /api/v1/leave/{id}/approve`
- **权限**: `att:leave:approve`
- **请求体**:

```json
{
  "action": 1,
  "comment": "同意请假，工作已安排交接"
}
```

> `action`: 1=通过, 2=拒绝, 3=退回

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/leave/{id}/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"comment":"同意请假，工作已安排交接"}'
```

---

### 9.7 取消请假申请

- **接口**: `POST /api/v1/leave/{id}/cancel`
- **权限**: `att:leave:apply`
- **说明**: 仅审批中状态 + 本人可取消

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/leave/{id}/cancel \
  -H "Authorization: Bearer <token>"
```

---

### 9.8 查询员工请假记录

- **接口**: `GET /api/v1/leave/applications/{employeeId}`
- **权限**: `att:leave:view`
- **Query 参数**: `page`（默认1）, `size`（默认20）

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/leave/applications/1?page=1&size=20" \
  -H "Authorization: Bearer <token>"
```

---

## 10. 请假附件

### 10.1 上传附件

- **接口**: `POST /api/v1/leave/attachments`
- **权限**: `att:leave:apply`
- **Content-Type**: `multipart/form-data`
- **表单参数**: `file` - 上传文件

- **响应**:

```json
{
  "code": 1,
  "data": {
    "id": 1,
    "leaveApplicationId": null,
    "fileName": "诊断证明.pdf",
    "fileSize": 204800,
    "fileUrl": "/uploads/2026/07/diagnosis_abc123.pdf",
    "uploadTime": "2026-07-16T10:00:00"
  }
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/leave/attachments \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/诊断证明.pdf"
```

---

### 10.2 查看申请附件列表

- **接口**: `GET /api/v1/leave/{id}/attachments`
- **权限**: `att:leave:view`
- **路径参数**: `id` - 请假申请ID

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/leave/{id}/attachments \
  -H "Authorization: Bearer <token>"
```

---

## 11. 请假统计

### 11.1 个人请假统计

- **接口**: `GET /api/v1/leave/stats/personal/{employeeId}`
- **权限**: `att:leave:view`
- **Query 参数**: `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": {
    "employeeId": 1,
    "employeeName": "张三",
    "year": 2026,
    "month": 7,
    "totalLeaveDays": 3.5,
    "annualLeaveDays": 2.5,
    "sickLeaveDays": 1.0,
    "personalLeaveDays": 0,
    "otherLeaveDays": 0
  }
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/leave/stats/personal/1?year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

### 11.2 部门请假率统计

- **接口**: `GET /api/v1/leave/stats/dept/{deptId}`
- **权限**: `att:leave:view`
- **Query 参数**: `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": {
    "deptId": 2,
    "deptName": "技术部",
    "year": 2026,
    "month": 7,
    "totalEmployees": 45,
    "leaveEmployeeCount": 8,
    "totalLeaveDays": 25.5,
    "leaveRate": 0.0246
  }
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/leave/stats/dept/2?year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

### 11.3 请假类型分布

- **接口**: `GET /api/v1/leave/stats/type-distribution`
- **权限**: `att:leave:view`
- **Query 参数**: `year`, `month`, `deptId`（可选）

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "leaveType": 1,
      "leaveTypeName": "年假",
      "totalDays": 45.5,
      "count": 20
    },
    {
      "leaveType": 4,
      "leaveTypeName": "病假",
      "totalDays": 20.0,
      "count": 10
    }
  ]
}
```

- **测试数据**:

```bash
# 全公司
curl -X GET "http://localhost:8080/api/v1/leave/stats/type-distribution?year=2026&month=7" \
  -H "Authorization: Bearer <token>"

# 指定部门
curl -X GET "http://localhost:8080/api/v1/leave/stats/type-distribution?year=2026&month=7&deptId=2" \
  -H "Authorization: Bearer <token>"
```

---

## 12. 工作日历

### 12.1 查询节假日/调班列表

- **接口**: `GET /api/v1/calendar`
- **权限**: `att:calendar:manage`
- **Query 参数**: `year`（默认2026）

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "id": 1,
      "calendarDate": "2026-10-01",
      "dayType": 1,
      "name": "国庆节"
    },
    {
      "id": 2,
      "calendarDate": "2026-10-10",
      "dayType": 2,
      "name": "国庆调班补班"
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/calendar?year=2026" \
  -H "Authorization: Bearer <token>"
```

---

### 12.2 批量保存节假日/调班

- **接口**: `POST /api/v1/calendar/batch`
- **权限**: `att:calendar:manage`
- **请求体**:

```json
[
  {
    "calendarDate": "2026-10-01",
    "dayType": 1,
    "name": "国庆节"
  },
  {
    "calendarDate": "2026-10-02",
    "dayType": 1,
    "name": "国庆节"
  },
  {
    "calendarDate": "2026-10-10",
    "dayType": 2,
    "name": "国庆调班补班"
  }
]
```

> `dayType`: 1=法定节假日/休息, 2=调班工作日

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/calendar/batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '[{"calendarDate":"2026-10-01","dayType":1,"name":"国庆节"},{"calendarDate":"2026-10-02","dayType":1,"name":"国庆节"},{"calendarDate":"2026-10-10","dayType":2,"name":"国庆调班补班"}]'
```

---

### 12.3 删除某日配置

- **接口**: `DELETE /api/v1/calendar`
- **权限**: `att:calendar:manage`
- **Query 参数**: `date` - 日期（yyyy-MM-dd）

- **测试数据**:

```bash
curl -X DELETE "http://localhost:8080/api/v1/calendar?date=2026-10-10" \
  -H "Authorization: Bearer <token>"
```

---

## 13. 入职管理

> 类级别权限: `onboarding:manage`

### 13.1 入职申请分页列表

- **接口**: `GET /api/v1/onboarding/page`
- **Query 参数**: `page`（默认1）, `size`（默认10）, `status`, `keyword`

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/onboarding/page?page=1&size=10&status=1&keyword=王" \
  -H "Authorization: Bearer <token>"
```

---

### 13.2 入职申请详情

- **接口**: `GET /api/v1/onboarding/{id}`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/onboarding/1 \
  -H "Authorization: Bearer <token>"
```

---

### 13.3 提交入职申请

- **接口**: `POST /api/v1/onboarding`
- **请求体**:

```json
{
  "realName": "王五",
  "phone": "13712345678",
  "email": "wangwu@hrms.com",
  "idCard": "320103199906201234",
  "targetDeptId": 2,
  "targetPositionId": 5,
  "offerSalary": 18000.00,
  "probationMonths": 3,
  "entryDate": "2026-08-01",
  "gender": 1,
  "grade": "P2",
  "reportTo": 1,
  "workLocation": "北京",
  "entryType": 1,
  "employmentType": 1,
  "probationSalaryRatio": 0.80,
  "bankAccount": "6222039876543210",
  "bankName": "建设银行"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/onboarding \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"realName":"王五","phone":"13712345678","email":"wangwu@hrms.com","idCard":"320103199906201234","targetDeptId":2,"targetPositionId":5,"offerSalary":18000.00,"probationMonths":3,"entryDate":"2026-08-01","gender":1,"grade":"P2","reportTo":1,"workLocation":"北京","entryType":1,"employmentType":1,"bankAccount":"6222039876543210","bankName":"建设银行"}'
```

---

### 13.4 保存草稿

- **接口**: `POST /api/v1/onboarding/draft`
- **请求体**: 同 [13.3 提交入职申请](#133-提交入职申请)

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/draft \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"realName":"王五","phone":"13712345678","email":"wangwu@hrms.com","targetDeptId":2,"targetPositionId":5,"offerSalary":18000.00,"entryDate":"2026-08-01"}'
```

---

### 13.5 更新入职申请

- **接口**: `PUT /api/v1/onboarding/{id}`

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/onboarding/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id":1,"realName":"王五","phone":"13712345678","email":"wangwu_new@hrms.com","targetDeptId":2,"targetPositionId":5,"offerSalary":20000.00,"entryDate":"2026-08-01"}'
```

---

### 13.6 删除草稿

- **接口**: `DELETE /api/v1/onboarding/{id}`

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/onboarding/1 \
  -H "Authorization: Bearer <token>"
```

---

### 13.7 撤回入职申请

- **接口**: `POST /api/v1/onboarding/{id}/withdraw`

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/1/withdraw \
  -H "Authorization: Bearer <token>"
```

---

### 13.8 审批入职申请

- **接口**: `PUT /api/v1/onboarding/{id}/approve`
- **请求体**:

```json
{
  "action": 1,
  "comment": "审批通过，安排入职"
}
```

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/onboarding/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"comment":"审批通过，安排入职"}'
```

---

### 13.9 确认到岗

- **接口**: `POST /api/v1/onboarding/{id}/confirm-arrival`

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/1/confirm-arrival \
  -H "Authorization: Bearer <token>"
```

---

### 13.10 更新入职日期

- **接口**: `PUT /api/v1/onboarding/{id}/entry-date`
- **请求体**:

```json
{
  "entryDate": "2026-08-15"
}
```

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/onboarding/1/entry-date \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"entryDate":"2026-08-15"}'
```

---

### 13.11 标记放弃入职

- **接口**: `POST /api/v1/onboarding/{id}/abandon`

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/onboarding/1/abandon \
  -H "Authorization: Bearer <token>"
```

---

## 14. 转正管理

> 类级别权限: `regularization:manage`

### 14.1 转正申请分页列表

- **接口**: `GET /api/v1/regularization/page`
- **Query 参数**: `page`（默认1）, `size`（默认10）, `status`, `keyword`

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/regularization/page?page=1&size=10&status=1" \
  -H "Authorization: Bearer <token>"
```

---

### 14.2 转正申请详情

- **接口**: `GET /api/v1/regularization/{id}`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/regularization/1 \
  -H "Authorization: Bearer <token>"
```

---

### 14.3 提交转正申请

- **接口**: `POST /api/v1/regularization`
- **请求体**:

```json
{
  "employeeId": 10,
  "probationEndDate": "2026-10-15",
  "applyDate": "2026-07-16"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/regularization \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"employeeId":10,"probationEndDate":"2026-10-15","applyDate":"2026-07-16"}'
```

---

### 14.4 审批转正申请

- **接口**: `PUT /api/v1/regularization/{id}/approve`
- **请求体**:

```json
{
  "action": 1,
  "resultType": 1,
  "comment": "工作表现优秀，同意转正"
}
```

> `resultType`: 1=通过转正, 2=延长试用, 3=不通过辞退
> 当 `resultType=2` 时需传 `extendedMonths`

- **测试数据**:

```bash
# 通过转正
curl -X PUT http://localhost:8080/api/v1/regularization/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"resultType":1,"comment":"工作表现优秀，同意转正"}'

# 延长试用
curl -X PUT http://localhost:8080/api/v1/regularization/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"resultType":2,"extendedMonths":1,"comment":"需继续考察，延长试用1个月"}'
```

---

### 14.5 即将到期员工列表

- **接口**: `GET /api/v1/regularization/expiring`
- **Query 参数**: `days` - 未来N天内到期（默认7）

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "employeeId": 10,
      "employeeName": "赵六",
      "deptName": "技术部",
      "entryDate": "2026-04-15",
      "probationEndDate": "2026-10-15",
      "daysUntilExpire": 7
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/regularization/expiring?days=7" \
  -H "Authorization: Bearer <token>"
```

---

## 15. 调岗管理

> 类级别权限: `transfer:manage`

### 15.1 调岗申请分页列表

- **接口**: `GET /api/v1/transfers/page`
- **Query 参数**: `page`（默认1）, `size`（默认10）, `status`, `keyword`

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/transfers/page?page=1&size=10" \
  -H "Authorization: Bearer <token>"
```

---

### 15.2 调岗申请详情

- **接口**: `GET /api/v1/transfers/{id}`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/transfers/1 \
  -H "Authorization: Bearer <token>"
```

---

### 15.3 提交调岗申请

- **接口**: `POST /api/v1/transfers`
- **请求体**:

```json
{
  "employeeId": 5,
  "fromDeptId": 3,
  "toDeptId": 2,
  "fromPositionId": 7,
  "toPositionId": 5,
  "effectiveDate": "2026-08-01",
  "reason": "业务调整，调至技术部"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"employeeId":5,"fromDeptId":3,"toDeptId":2,"fromPositionId":7,"toPositionId":5,"effectiveDate":"2026-08-01","reason":"业务调整，调至技术部"}'
```

---

### 15.4 审批调岗申请

- **接口**: `PUT /api/v1/transfers/{id}/approve`

> 调岗需原部门和新部门负责人双重审批，通过 `role` 参数区分：
> - `role: "old"` — 原部门负责人审批
> - `role: "new"` — 新部门负责人审批

- **请求体**:

```json
{
  "action": 1,
  "role": "old",
  "comment": "同意调出"
}
```

- **测试数据**:

```bash
# 原部门审批
curl -X PUT http://localhost:8080/api/v1/transfers/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"role":"old","comment":"同意调出"}'

# 新部门审批
curl -X PUT http://localhost:8080/api/v1/transfers/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"role":"new","comment":"同意接收"}'
```

---

## 16. 离职管理

> 类级别权限: `resignation:manage`

### 16.1 离职申请分页列表

- **接口**: `GET /api/v1/resignations/page`
- **Query 参数**: `page`（默认1）, `size`（默认10）, `status`, `keyword`

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/resignations/page?page=1&size=10" \
  -H "Authorization: Bearer <token>"
```

---

### 16.2 离职申请详情

- **接口**: `GET /api/v1/resignations/{id}`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/resignations/1 \
  -H "Authorization: Bearer <token>"
```

---

### 16.3 提交离职申请

- **接口**: `POST /api/v1/resignations`
- **请求体**:

```json
{
  "employeeId": 20,
  "resignationDate": "2026-08-31",
  "resignationType": 1,
  "reason": "个人职业发展规划",
  "handoverTo": 5,
  "handoverNotes": "已整理项目文档"
}
```

> `resignationType`: 1=主动离职, 2=被动离职, 3=协商离职

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/resignations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"employeeId":20,"resignationDate":"2026-08-31","resignationType":1,"reason":"个人职业发展规划","handoverTo":5,"handoverNotes":"已整理项目文档"}'
```

---

### 16.4 审批离职申请

- **接口**: `PUT /api/v1/resignations/{id}/approve`
- **请求体**:

```json
{
  "action": 1,
  "comment": "同意离职，安排交接"
}
```

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/resignations/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"comment":"同意离职，安排交接"}'
```

---

## 17. 审批工作台

### 17.1 我的待办

- **接口**: `GET /api/v1/approvals/todo`
- **权限**: `approval:workbench`

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "recordId": 1,
      "businessType": 1,
      "businessTypeName": "请假申请",
      "businessId": 201,
      "applicantName": "张三",
      "applicantDept": "技术部",
      "applicationTime": "2026-07-16T10:30:00",
      "deadline": "2026-07-18T18:00:00",
      "summary": "张三申请年假2.5天 (2026-07-20 ~ 2026-07-22)"
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/approvals/todo \
  -H "Authorization: Bearer <token>"
```

---

### 17.2 我的已办

- **接口**: `GET /api/v1/approvals/done`
- **权限**: `approval:workbench`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/approvals/done \
  -H "Authorization: Bearer <token>"
```

---

### 17.3 统一审批详情

- **接口**: `GET /api/v1/approvals/detail/{businessType}/{businessId}`
- **权限**: `approval:workbench` 或 `approval:view`
- **路径参数**:
  - `businessType`: 业务类型编码
  - `businessId`: 业务数据ID

> `businessType` 对照：
> - 1 = 请假申请
> - 2 = 入职申请
> - 3 = 转正申请
> - 4 = 调岗申请
> - 5 = 离职申请
> - 6 = 补卡申请
> - 7 = 薪资批次

- **响应**:

```json
{
  "code": 1,
  "data": {
    "applicationInfo": { "...": "..." },
    "approvalHistory": [
      {
        "stepName": "部门主管审批",
        "approverName": "赵经理",
        "action": 1,
        "actionName": "通过",
        "comment": "同意",
        "approvalTime": "2026-07-16T14:00:00"
      }
    ],
    "currentActionable": true,
    "nextApprover": "HR经理"
  }
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/approvals/detail/1/201 \
  -H "Authorization: Bearer <token>"
```

---

### 17.4 转交审批任务

- **接口**: `POST /api/v1/approvals/records/{id}/transfer`
- **权限**: `approval:workbench`
- **路径参数**: `id` - 审批记录ID
- **请求体**:

```json
{
  "targetApproverId": 8,
  "reason": "出差期间无法处理，转交给同事审批"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/approvals/records/1/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"targetApproverId":8,"reason":"出差期间无法处理，转交给同事审批"}'
```

---

### 17.5 设置委托

- **接口**: `POST /api/v1/approvals/delegations`
- **权限**: `approval:workbench`
- **请求体**:

```json
{
  "delegateTo": 8,
  "startDate": "2026-07-20",
  "endDate": "2026-07-25",
  "reason": "休假期间委托审批"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/approvals/delegations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"delegateTo":8,"startDate":"2026-07-20","endDate":"2026-07-25","reason":"休假期间委托审批"}'
```

---

### 17.6 取消委托

- **接口**: `DELETE /api/v1/approvals/delegations/{id}`
- **权限**: `approval:workbench`

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/approvals/delegations/1 \
  -H "Authorization: Bearer <token>"
```

---

### 17.7 查询我的委托

- **接口**: `GET /api/v1/approvals/delegations/my`
- **权限**: `approval:workbench`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/approvals/delegations/my \
  -H "Authorization: Bearer <token>"
```

---

## 18. 薪资管理

### 18.1 单人薪资核算

- **接口**: `POST /api/v1/salary/calculate/{employeeId}`
- **权限**: `salary:batch:calc`
- **Query 参数**: `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": {
    "employeeId": 1,
    "employeeName": "张三",
    "year": 2026,
    "month": 7,
    "baseSalary": 25000.00,
    "attendanceDeduction": 0,
    "leaveDeduction": 0,
    "overtimePay": 500.00,
    "bonusTotal": 2000.00,
    "socialInsurance": 2750.00,
    "housingFund": 3000.00,
    "taxableIncome": 21750.00,
    "incomeTax": 525.00,
    "grossPay": 27000.00,
    "netPay": 21225.00
  }
}
```

- **测试数据**:

```bash
curl -X POST "http://localhost:8080/api/v1/salary/calculate/1?year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

### 18.2 批量薪资核算

- **接口**: `POST /api/v1/salary/batch-calculate`
- **权限**: `salary:batch:calc`
- **请求体**:

```json
{
  "year": 2026,
  "month": 7,
  "deptId": null
}
```

> `deptId` 为 null 则全公司核算

- **响应**:

```json
{
  "code": 1,
  "data": 150
}
```

> 返回核算人数

- **测试数据**:

```bash
# 全公司核算
curl -X POST http://localhost:8080/api/v1/salary/batch-calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"year":2026,"month":7}'

# 指定部门核算
curl -X POST http://localhost:8080/api/v1/salary/batch-calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"year":2026,"month":7,"deptId":2}'
```

---

### 18.3 薪资批次列表

- **接口**: `GET /api/v1/salary/batches`
- **权限**: `salary:batch:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/batches \
  -H "Authorization: Bearer <token>"
```

---

### 18.4 批次薪资记录

- **接口**: `GET /api/v1/salary/batches/{id}/records`
- **权限**: `salary:batch:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/batches/1/records \
  -H "Authorization: Bearer <token>"
```

---

### 18.5 提交薪资批次

- **接口**: `POST /api/v1/salary/batches/{id}/submit`
- **权限**: `salary:batch:submit`
- **说明**: 批次状态变为 PENDING_APPROVAL

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/batches/1/submit \
  -H "Authorization: Bearer <token>"
```

---

### 18.6 审批薪资批次

- **接口**: `POST /api/v1/salary/batches/{id}/approve`
- **权限**: `salary:calc:approve`
- **请求体**:

```json
{
  "action": 1,
  "comment": "核算数据无误，批准发放"
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/batches/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"action":1,"comment":"核算数据无误，批准发放"}'
```

---

### 18.7 批次发放确认

- **接口**: `POST /api/v1/salary/batches/{id}/pay`
- **权限**: `salary:batch:pay`
- **说明**: 仅 APPROVED → PAID

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/batches/1/pay \
  -H "Authorization: Bearer <token>"
```

---

### 18.8 批次归档

- **接口**: `POST /api/v1/salary/batches/{id}/archive`
- **权限**: `salary:batch:archive`
- **说明**: 仅 PAID → ARCHIVED

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/batches/1/archive \
  -H "Authorization: Bearer <token>"
```

---

### 18.9 查询薪资记录

- **接口**: `GET /api/v1/salary/records`
- **权限**: `salary:calc:view`
- **Query 参数**: `employeeId`（可选）, `year`, `month`

> 传 `employeeId` 查单人记录；不传则查全量（仅HR/财务）

- **测试数据**:

```bash
# 查询单员工
curl -X GET "http://localhost:8080/api/v1/salary/records?employeeId=1&year=2026&month=7" \
  -H "Authorization: Bearer <token>"

# 查询全量（需HR/财务权限）
curl -X GET "http://localhost:8080/api/v1/salary/records?year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

### 18.10 年度薪资记录

- **接口**: `GET /api/v1/salary/records/yearly`
- **权限**: `salary:payslip:self`
- **Query 参数**: `employeeId`, `year`

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/salary/records/yearly?employeeId=1&year=2026" \
  -H "Authorization: Bearer <token>"
```

---

### 18.11 我的工资条列表

- **接口**: `GET /api/v1/salary/payslips`
- **权限**: `salary:payslip:self`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/payslips \
  -H "Authorization: Bearer <token>"
```

---

### 18.12 工资条详情

- **接口**: `GET /api/v1/salary/payslips/{recordId}`
- **权限**: `salary:payslip:self`
- **Query 参数**: `password` - 二次验证密码（首次查看时必填）

- **测试数据**:

```bash
# 首次查看（需密码验证）
curl -X GET "http://localhost:8080/api/v1/salary/payslips/1?password=123456" \
  -H "Authorization: Bearer <token>"

# 已验证后查看
curl -X GET http://localhost:8080/api/v1/salary/payslips/1 \
  -H "Authorization: Bearer <token>"
```

---

### 18.13 薪资月度趋势报表

- **接口**: `GET /api/v1/salary/reports/trend`
- **权限**: `salary:report:view`

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "yearMonth": "2026-01",
      "grossTotal": 1500000.00,
      "netTotal": 1200000.00,
      "employeeCount": 100
    },
    {
      "yearMonth": "2026-02",
      "grossTotal": 1520000.00,
      "netTotal": 1215000.00,
      "employeeCount": 102
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/reports/trend \
  -H "Authorization: Bearer <token>"
```

---

### 18.14 部门成本分布报表

- **接口**: `GET /api/v1/salary/reports/dept-cost`
- **权限**: `salary:report:view`
- **Query 参数**: `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "deptId": 2,
      "deptName": "技术部",
      "grossTotal": 500000.00,
      "netTotal": 400000.00,
      "employeeCount": 45,
      "percentage": 0.35
    }
  ]
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/salary/reports/dept-cost?year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

### 18.15 薪资构成占比报表

- **接口**: `GET /api/v1/salary/reports/composition`
- **权限**: `salary:report:view`
- **Query 参数**: `year`, `month`

- **响应**:

```json
{
  "code": 1,
  "data": {
    "baseSalary": 1200000.00,
    "bonus": 200000.00,
    "overtime": 50000.00,
    "allowance": 80000.00,
    "socialInsurance": 165000.00,
    "housingFund": 180000.00,
    "incomeTax": 75000.00
  }
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/salary/reports/composition?year=2026&month=7" \
  -H "Authorization: Bearer <token>"
```

---

## 19. 薪资账套

### 19.1 查询员工薪资账套

- **接口**: `GET /api/v1/salary/accounts/{employeeId}`
- **权限**: `salary:account:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/accounts/1 \
  -H "Authorization: Bearer <token>"
```

---

### 19.2 查询薪资变更历史

- **接口**: `GET /api/v1/salary/accounts/{employeeId}/history`
- **权限**: `salary:account:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/accounts/1/history \
  -H "Authorization: Bearer <token>"
```

---

### 19.3 创建薪资账套

- **接口**: `POST /api/v1/salary/accounts`
- **权限**: `salary:account:manage`
- **请求体**:

```json
{
  "employeeId": 1,
  "planId": 1,
  "baseSalary": 25000.00,
  "pensionBase": 25000.00,
  "medicalBase": 25000.00,
  "housingFundBase": 25000.00,
  "housingFundRatio": 12.00,
  "status": 1
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"employeeId":1,"planId":1,"baseSalary":25000.00,"pensionBase":25000.00,"medicalBase":25000.00,"housingFundBase":25000.00,"housingFundRatio":12.00,"status":1}'
```

---

### 19.4 调整薪资

- **接口**: `PUT /api/v1/salary/accounts/{id}/adjust`
- **权限**: `salary:account:manage`
- **请求体**: 同 [19.3 创建薪资账套](#193-创建薪资账套)

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/salary/accounts/1/adjust \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"baseSalary":28000.00,"pensionBase":28000.00,"medicalBase":28000.00,"housingFundBase":28000.00}'
```

---

### 19.5 停用薪资账套

- **接口**: `PUT /api/v1/salary/accounts/{id}/deactivate`
- **权限**: `salary:account:manage`

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/salary/accounts/1/deactivate \
  -H "Authorization: Bearer <token>"
```

---

## 20. 薪资方案

### 20.1 薪资方案列表

- **接口**: `GET /api/v1/salary/plans`
- **权限**: `salary:plan:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/plans \
  -H "Authorization: Bearer <token>"
```

---

### 20.2 薪资方案详情

- **接口**: `GET /api/v1/salary/plans/{id}`
- **权限**: `salary:plan:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/plans/1 \
  -H "Authorization: Bearer <token>"
```

---

### 20.3 创建薪资方案

- **接口**: `POST /api/v1/salary/plans`
- **权限**: `salary:plan:manage`
- **请求体**:

```json
{
  "planName": "标准薪资方案",
  "description": "适用于正式员工的标准薪资方案",
  "status": 1
}
```

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/plans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"planName":"标准薪资方案","description":"适用于正式员工的标准薪资方案","status":1}'
```

---

### 20.4 更新薪资方案

- **接口**: `PUT /api/v1/salary/plans/{id}`
- **权限**: `salary:plan:manage`

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/salary/plans/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"planName":"标准薪资方案(修订)","description":"2026年修订版标准薪资方案"}'
```

---

### 20.5 切换方案状态

- **接口**: `PUT /api/v1/salary/plans/{id}/status`
- **权限**: `salary:plan:manage`
- **Query 参数**: `status` - 0=禁用, 1=启用

- **测试数据**:

```bash
# 启用
curl -X PUT "http://localhost:8080/api/v1/salary/plans/1/status?status=1" \
  -H "Authorization: Bearer <token>"

# 禁用
curl -X PUT "http://localhost:8080/api/v1/salary/plans/1/status?status=0" \
  -H "Authorization: Bearer <token>"
```

---

### 20.6 方案工资项目列表

- **接口**: `GET /api/v1/salary/plans/{planId}/items`
- **权限**: `salary:plan:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/plans/1/items \
  -H "Authorization: Bearer <token>"
```

---

### 20.7 添加方案工资项目

- **接口**: `POST /api/v1/salary/plans/{planId}/items`
- **权限**: `salary:plan:manage`
- **请求体**:

```json
{
  "itemName": "餐补",
  "itemType": "ALLOWANCE",
  "amount": 500.00,
  "calculationRule": "FIXED",
  "sortOrder": 3
}
```

> `itemType`: BASE=基本工资, BONUS=奖金, ALLOWANCE=补贴, DEDUCTION=扣款, INSURANCE=社保, FUND=公积金, TAX=个税
> `calculationRule`: FIXED=固定金额, RATIO=比例计算, FORMULA=公式计算

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/plans/1/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"itemName":"餐补","itemType":"ALLOWANCE","amount":500.00,"calculationRule":"FIXED","sortOrder":3}'
```

---

### 20.8 删除方案工资项目

- **接口**: `DELETE /api/v1/salary/plans/{planId}/items/{id}`
- **权限**: `salary:plan:manage`

- **测试数据**:

```bash
curl -X DELETE http://localhost:8080/api/v1/salary/plans/1/items/3 \
  -H "Authorization: Bearer <token>"
```

---

### 20.9 方案适用范围列表

- **接口**: `GET /api/v1/salary/plans/{planId}/scopes`
- **权限**: `salary:plan:view`

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/salary/plans/1/scopes \
  -H "Authorization: Bearer <token>"
```

---

### 20.10 添加方案适用范围

- **接口**: `POST /api/v1/salary/plans/{planId}/scopes`
- **权限**: `salary:plan:manage`
- **请求体**:

```json
{
  "scopeType": "DEPT",
  "scopeId": 2,
  "scopeName": "技术部"
}
```

> `scopeType`: DEPT=部门, POSITION=职位, GRADE=职级

- **测试数据**:

```bash
curl -X POST http://localhost:8080/api/v1/salary/plans/1/scopes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"scopeType":"DEPT","scopeId":2,"scopeName":"技术部"}'
```

---

## 21. 个人中心

### 21.1 我的档案

- **接口**: `GET /api/v1/personal/profile`
- **权限**: 登录用户（需关联员工身份）

- **响应**:

```json
{
  "code": 1,
  "data": {
    "profile": {
      "id": 1,
      "employeeNo": "EMP20260001",
      "name": "张三",
      "phone": "138****5678",
      "email": "zhangsan@hrms.com",
      "deptName": "技术部",
      "positionName": "高级Java开发",
      "grade": "P3",
      "entryDate": "2026-01-15",
      "status": 1
    },
    "editability": {
      "email": { "editable": true, "lockReason": null },
      "currentAddress": { "editable": true, "lockReason": null },
      "registeredAddress": { "editable": true, "lockReason": null },
      "birthday": { "editable": true, "lockReason": null },
      "deptId": { "editable": false, "lockReason": "需调岗流程" },
      "positionId": { "editable": false, "lockReason": "需调岗流程" },
      "grade": { "editable": false, "lockReason": "需调岗流程" },
      "baseSalary": { "editable": false, "lockReason": "如需修改请联系 HR" },
      "bankAccount": { "editable": false, "lockReason": "如需修改请联系 HR" }
    }
  }
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/personal/profile \
  -H "Authorization: Bearer <token>"
```

---

### 21.2 更新个人信息

- **接口**: `PUT /api/v1/personal/profile`
- **权限**: 登录用户
- **说明**: 仅允许更新 email, currentAddress, registeredAddress, birthday 四个字段

- **请求体**:

```json
{
  "email": "zhangsan_new@hrms.com",
  "currentAddress": "北京市海淀区xx路xx号",
  "birthday": "1995-06-15"
}
```

- **测试数据**:

```bash
curl -X PUT http://localhost:8080/api/v1/personal/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"email":"zhangsan_new@hrms.com","currentAddress":"北京市海淀区xx路xx号"}'
```

---

### 21.3 考勤日历视图

- **接口**: `GET /api/v1/personal/attendance-calendar`
- **权限**: 登录用户
- **Query 参数**: `yearMonth` - 格式 yyyy-MM

- **响应**:

```json
{
  "code": 1,
  "data": [
    {
      "date": "2026-07-01",
      "isWorkday": true,
      "status": "NORMAL"
    },
    {
      "date": "2026-07-02",
      "isWorkday": true,
      "status": "LEAVE",
      "leaveDetail": {
        "leaveType": 1,
        "leaveTypeName": "年假",
        "reason": "个人年假休息",
        "applicationId": 201
      }
    },
    {
      "date": "2026-07-05",
      "isWorkday": false,
      "status": "WEEKEND"
    }
  ]
}
```

> `status` 枚举：NORMAL=正常, LATE=迟到, EARLY=早退, ABSENT=旷工, MISSING=缺卡, LEAVE=请假, WEEKEND=周末, FUTURE=未来

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/personal/attendance-calendar?yearMonth=2026-07" \
  -H "Authorization: Bearer <token>"
```

---

### 21.4 个人薪资趋势

- **接口**: `GET /api/v1/personal/salary-trend`
- **权限**: `salary:payslip:self`

- **响应**:

```json
{
  "code": 1,
  "data": [
    { "yearMonth": "2026-02", "netPay": 21225.00 },
    { "yearMonth": "2026-03", "netPay": 21225.00 },
    { "yearMonth": "2026-04", "netPay": 22000.00 },
    { "yearMonth": "2026-05", "netPay": 22000.00 },
    { "yearMonth": "2026-06", "netPay": 22000.00 },
    { "yearMonth": "2026-07", "netPay": 21225.00 }
  ]
}
```

- **测试数据**:

```bash
curl -X GET http://localhost:8080/api/v1/personal/salary-trend \
  -H "Authorization: Bearer <token>"
```

---

## 22. 审计日志

### 22.1 审计日志分页查询

- **接口**: `GET /api/v1/audit-logs`
- **权限**: `audit:log:view`
- **Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| operatorId | Long | 否 | 操作人ID |
| operation | String | 否 | 操作类型 |
| resourceType | String | 否 | 资源类型 |
| startTime | LocalDateTime | 否 | 操作开始时间 |
| endTime | LocalDateTime | 否 | 操作结束时间 |
| page | int | 否 | 页码（默认1） |
| size | int | 否 | 每页大小（默认20） |

- **响应**:

```json
{
  "list": [
    {
      "id": 1,
      "operatorId": 1,
      "operatorName": "admin",
      "operation": "CREATE",
      "resourceType": "EMPLOYEE",
      "resourceId": "1",
      "result": "SUCCESS",
      "errorMessage": null,
      "clientIp": "192.168.1.100",
      "createTime": "2026-07-16T10:00:00"
    }
  ],
  "total": 500,
  "page": 1,
  "size": 20
}
```

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/audit-logs?operation=CREATE&resourceType=EMPLOYEE&page=1&size=20" \
  -H "Authorization: Bearer <token>"
```

---

### 22.2 导出审计日志

- **接口**: `GET /api/v1/audit-logs/export`
- **权限**: `audit:log:export`
- **Query 参数**: 同 [22.1 审计日志分页查询](#221-审计日志分页查询)（不含page/size）
- **返回**: CSV 文件下载

- **测试数据**:

```bash
curl -X GET "http://localhost:8080/api/v1/audit-logs/export?operation=CREATE&startTime=2026-07-01T00:00:00&endTime=2026-07-31T23:59:59" \
  -H "Authorization: Bearer <token>" \
  -o audit_logs_20260716.csv
```

---

## 附录：通用枚举说明

### 员工状态 (Employee Status)

| 值 | 说明 |
|----|------|
| 0 | 待入职 |
| 1 | 试用期 |
| 2 | 正式 |
| 3 | 待离职 |
| 4 | 已离职 |

### 假期类型 (Leave Type)

| 值 | 说明 |
|----|------|
| 1 | 年假 |
| 2 | 调休 |
| 3 | 事假 |
| 4 | 病假 |
| 5 | 婚假 |
| 6 | 产假 |
| 7 | 丧假 |

### 审批动作 (Approval Action)

| 值 | 说明 |
|----|------|
| 1 | 通过 |
| 2 | 拒绝 |
| 3 | 退回 |

### 审批业务类型 (Business Type)

| 值 | 说明 |
|----|------|
| 1 | 请假申请 |
| 2 | 入职申请 |
| 3 | 转正申请 |
| 4 | 调岗申请 |
| 5 | 离职申请 |
| 6 | 补卡申请 |
| 7 | 薪资批次 |

### 薪资批次状态

```
DRAFT → PENDING_APPROVAL → APPROVED → PAID → ARCHIVED
```

### 权限码汇总

| 权限码 | 说明 |
|--------|------|
| `emp:view` | 查看员工档案 |
| `emp:create` | 创建员工档案 |
| `emp:edit` | 编辑员工档案 |
| `emp:delete` | 删除员工档案 |
| `org:dept:view` | 查看部门树 |
| `org:dept:manage` | 管理部门 |
| `org:position:view` | 查看职位 |
| `org:position:manage` | 管理职位 |
| `att:record:punch` | 打卡 |
| `att:record:view` | 查看打卡记录 |
| `att:record:manage` | 管理考勤记录 |
| `att:group:view` | 查看考勤组 |
| `att:group:manage` | 管理考勤组 |
| `att:leave:view` | 查看请假 |
| `att:leave:apply` | 请假申请 |
| `att:leave:approve` | 审批请假 |
| `att:card:apply` | 补卡申请 |
| `att:card:approve` | 审批补卡 |
| `att:calendar:manage` | 管理工作日历 |
| `emp:attendance:view` | 查看考勤统计 |
| `onboarding:manage` | 入职管理 |
| `regularization:manage` | 转正管理 |
| `transfer:manage` | 调岗管理 |
| `resignation:manage` | 离职管理 |
| `approval:workbench` | 审批工作台 |
| `approval:view` | 查看审批详情 |
| `salary:batch:calc` | 薪资核算 |
| `salary:batch:view` | 查看薪资批次 |
| `salary:batch:submit` | 提交薪资批次 |
| `salary:batch:pay` | 薪资发放 |
| `salary:batch:archive` | 薪资归档 |
| `salary:calc:approve` | 审批薪资 |
| `salary:calc:view` | 查看薪资记录 |
| `salary:payslip:self` | 查看本人工资条 |
| `salary:account:view` | 查看薪资账套 |
| `salary:account:manage` | 管理薪资账套 |
| `salary:plan:view` | 查看薪资方案 |
| `salary:plan:manage` | 管理薪资方案 |
| `salary:report:view` | 查看薪资报表 |
| `audit:log:view` | 查看审计日志 |
| `audit:log:export` | 导出审计日志 |

---

> 📅 文档生成日期：2026-07-16  
> 🏷️ 版本：v1.0  
> 🔐 所有薪资/个人信息接口均需严格按角色权限校验，敏感字段（身份证、手机号、银行账号、薪资）以密文存储，通过 `EncryptedStringTypeHandler` / `EncryptedBigDecimalTypeHandler` 透明加解密。
