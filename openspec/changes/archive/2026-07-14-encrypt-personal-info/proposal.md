## Why

HRMS 系统存储了大量员工个人敏感信息（身份证号、手机号、银行账号、家庭住址等），当前以明文形式存储在数据库中，存在严重的数据泄露风险。根据《个人信息保护法》(PIPL) 和《数据安全法》要求，企业必须对个人信息采取加密等安全保护措施。一旦发生数据库拖库或内部越权访问，明文存储将导致批量员工隐私泄露，给公司和员工带来不可挽回的损失。

## What Changes

- **数据库敏感字段加密存储**：对 Employee、OnboardingApplication 等表中涉及个人隐私的字段（身份证号、手机号、银行账号、地址等）进行 AES-256 加密后存储
- **透明加解密层**：通过 MyBatis TypeHandler 实现写入自动加密、读取自动解密，业务代码无需感知加解密逻辑
- **加密密钥管理**：密钥与代码分离，通过环境变量或配置中心管理，支持密钥轮换
- **搜索兼容方案**：对需要精确匹配的加密字段（如手机号）采用确定性加密或哈希辅助索引
- **已有数据迁移**：提供数据迁移脚本，将存量明文数据加密后回写数据库
- **薪资数据保护**：SalaryAccount 和 SalaryRecord 中的薪资字段同步进行加密保护

## Capabilities

### New Capabilities

- `personal-info-encryption`: 员工个人敏感信息（身份证、手机号、银行账号、地址等）的加密存储与透明解密，包括加密算法选型、MyBatis TypeHandler 实现、密钥管理方案
- `salary-encryption`: 薪资相关数据（基本工资、绩效工资、社保公积金基数等）的加密存储，确保薪资数据即使被拖库也无法直接读取
- `data-migration-encryption`: 存量明文数据的加密迁移，支持分批处理、断点续传和数据完整性校验

### Modified Capabilities

<!-- 无现有 specs，此为全新模块 -->

## Impact

- **Entity 层**: Employee、OnboardingApplication、SalaryAccount、SalaryRecord 等实体的敏感字段需要改造
- **DTO/VO 层**: EmployeeSaveDTO、OnboardingSaveDTO、EmployeeVO 等数据传输对象 — 加解密在持久层完成，DTO 层无感知（传输中仍为明文，仅在存储时加密）
- **Mapper 层**: 涉及敏感字段的 MyBatis Mapper XML 需要注册 TypeHandler
- **查询逻辑**: 对加密字段的 WHERE 条件查询（如按手机号查员工）需要调整为加密后匹配或哈希索引查询
- **数据迁移**: 需要编写一次性迁移脚本处理存量数据
- **配置文件**: application.yml 需新增加密密钥配置项
- **依赖**: 无需引入新的第三方加密库，使用 JDK 自带 `javax.crypto` 或 Spring Security Crypto
