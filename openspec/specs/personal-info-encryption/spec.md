# personal-info-encryption

## Purpose

对 HRMS 中 Employee 和 OnboardingApplication 实体的个人敏感信息（PII）进行 AES-256-GCM 加密存储，通过 MyBatis TypeHandler 实现持久层透明加解密，并通过 SHA-256 哈希辅助索引支持加密字段的精确查询。

## Requirements

### Requirement: 敏感字段自动加密存储

系统 SHALL 在数据写入数据库时，自动对 Employee 和 OnboardingApplication 实体中的以下字段进行 AES-256-GCM 加密后存储：`phone`、`email`、`idCard`、`registeredAddress`、`currentAddress`、`bankAccount`、`bankName`。加密过程 MUST 对业务层代码透明，通过 MyBatis TypeHandler 在持久层自动完成。

#### Scenario: 创建员工时自动加密敏感字段

- **WHEN** HR 专员通过 API 创建新员工档案，提交包含明文手机号、身份证号、地址等敏感信息的请求
- **THEN** 系统在 INSERT 时将 `phone`、`idCard`、`email`、`registeredAddress`、`currentAddress`、`bankAccount`、`bankName` 字段自动加密后写入数据库，数据库存储的值为 Base64 编码的密文，Service 层代码无需调用任何加密方法

#### Scenario: 更新员工时自动加密敏感字段

- **WHEN** HR 专员更新员工档案中的手机号或地址
- **THEN** 系统在 UPDATE 时自动加密新值后写入数据库，已加密的旧值被覆盖

#### Scenario: 读取员工时自动解密敏感字段

- **WHEN** 任何有权限的用户通过 API 查询员工详情
- **THEN** 系统在 SELECT 后自动将加密字段解密为明文返回给业务层，VO 中展示原始明文信息

### Requirement: 加密字段精确查询支持

系统 SHALL 支持对需要高频精确匹配的加密字段（`phone`、`idCard`、`bankAccount`）进行等值查询。每个查询字段 MUST 维护一个对应的 SHA-256 哈希辅助索引列（`<field>_hash`），查询时通过哈希值匹配，避免全表解密。

#### Scenario: 按手机号查找员工

- **WHEN** 系统需要根据手机号 `13800138000` 查找员工（如入职查重、登录）
- **THEN** 系统计算 `SHA-256(13800138000 + pepper)` 得到哈希值，通过 `WHERE phone_hash = ?` 在数据库匹配，返回匹配的员工记录，其敏感字段自动解密为明文

#### Scenario: 按身份证号查重

- **WHEN** 系统需要校验身份证号是否已存在
- **THEN** 系统计算身份证号的哈希值，通过 `WHERE id_card_hash = ?` 查询是否存在匹配记录

### Requirement: 加密类型处理器注册

系统 SHALL 提供 `EncryptedStringTypeHandler` 和 `EncryptedBigDecimalTypeHandler` 两个 MyBatis TypeHandler，分别处理 String 类型和 BigDecimal 类型的敏感字段加解密。TypeHandler MUST 从 `EncryptionConfig` 获取密钥，支持通过字段元数据判断是否启用加密。

#### Scenario: String 类型加密字段的 TypeHandler 处理

- **WHEN** MyBatis 执行包含 `phone`、`email`、`idCard` 等 String 类型敏感字段的 INSERT/UPDATE/SELECT
- **THEN** `EncryptedStringTypeHandler.setParameter()` 在写入前自动加密，`getResult()` 在读取后自动解密

#### Scenario: BigDecimal 类型加密字段的 TypeHandler 处理

- **WHEN** MyBatis 执行包含 `baseSalary`、`offerSalary` 等 BigDecimal 类型敏感字段的 INSERT/UPDATE/SELECT
- **THEN** `EncryptedBigDecimalTypeHandler.setParameter()` 将 BigDecimal 转字符串后加密存储，`getResult()` 解密后还原为 BigDecimal

### Requirement: 加密密钥管理

系统 SHALL 通过环境变量 `HRMS_ENCRYPTION_KEY` 获取 256-bit 主密钥（Base64 编码）。MUST 通过 HKDF 从主密钥派生数据加密密钥（DEK）和 HMAC 密钥。密钥明文 MUST NOT 出现在日志、配置文件和代码中。系统启动时若未配置密钥 MUST 拒绝启动并给出明确错误提示。

#### Scenario: 正常启动加载密钥

- **WHEN** 系统启动时环境变量 `HRMS_ENCRYPTION_KEY` 已设置有效的 256-bit Base64 密钥
- **THEN** `EncryptionConfig` Bean 初始化成功，通过 HKDF 派生 DEK 和 HMAC 密钥并缓存于内存

#### Scenario: 缺少密钥时拒绝启动

- **WHEN** 系统启动时环境变量 `HRMS_ENCRYPTION_KEY` 未设置或无效
- **THEN** Spring 容器启动失败，抛出 `IllegalStateException` 并提示 "HRMS_ENCRYPTION_KEY environment variable is required but not set"

### Requirement: 数据库 Schema 扩展

系统 MUST 为需要加密的敏感字段新增对应的加密列（`<field>_encrypted` VARCHAR(512)）和哈希索引列（`<field>_hash` CHAR(64)）。新增 `encryption_version` 列用于标记加密版本。表的原有明文字段在迁移完成后 MUST 被删除。

#### Scenario: employee 表新增加密列

- **WHEN** 执行数据库迁移脚本
- **THEN** `employee` 表新增 `phone_encrypted`、`phone_hash`、`email_encrypted`、`id_card_encrypted`、`id_card_hash`、`registered_address_encrypted`、`current_address_encrypted`、`bank_account_encrypted`、`bank_account_hash`、`bank_name_encrypted`、`base_salary_encrypted`、`encryption_version` 列

#### Scenario: onboarding_application 表新增加密列

- **WHEN** 执行数据库迁移脚本
- **THEN** `onboarding_application` 表新增对应的加密列和哈希辅助列
