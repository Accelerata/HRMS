## 1. 加密基础设施搭建

- [x] 1.1 创建 `EncryptionConfig` 配置类，从环境变量 `HRMS_ENCRYPTION_KEY` 读取 Base64 编码的 256-bit 主密钥，通过 HKDF 派生 DEK 和 HMAC 密钥，启动时校验密钥有效性
- [x] 1.2 创建 `EncryptionUtil` 工具类，实现 AES-256-GCM 加密/解密方法、HMAC-SHA256 确定性 IV 派生方法、SHA-256 哈希计算方法（含 pepper）
- [x] 1.3 创建 `EncryptedStringTypeHandler`（继承 `BaseTypeHandler<String>`），在 `setNonNullParameter` 中加密，在 `getNullableResult` 中解密，支持加密列为 NULL 时回退读取原文列
- [x] 1.4 创建 `EncryptedBigDecimalTypeHandler`（继承 `BaseTypeHandler<BigDecimal>`），BigDecimal → String → 加密存储，读取时解密 → String → BigDecimal
- [x] 1.5 在 `application.yml` 中添加加密相关配置项（密钥环境变量引用、是否启用加密的开关 `hrms.encryption.enabled`）

## 2. Employee 表加密改造

- [x] 2.1 创建数据库迁移脚本 `phase5-encryption.sql`：为 `employee` 表添加所有加密列和哈希索引列
- [x] 2.2 更新 `EmployeeMapper.xml` 的 `BaseColumns` SQL 片段，将敏感字段映射到加密列，并在 INSERT/UPDATE 语句中为敏感字段注册 TypeHandler
- [x] 2.3 更新 `EmployeeMapper.xml` 中按 `phone` 搜索的查询条件，改为通过 `phone_hash` 匹配
- [x] 2.4 更新 `EmployeeService` 中涉及手机号查重的逻辑，使用哈希索引查询
- [x] 2.5 更新 `Employee` 实体类，新增 `phoneHash`、`idCardHash`、`bankAccountHash`、`encryptionVersion` 字段

## 3. OnboardingApplication 表加密改造

- [x] 3.1 数据库迁移：为 `onboarding_application` 表新增所有加密列和哈希索引列
- [x] 3.2 更新 `OnboardingApplicationMapper.xml`，所有敏感字段的 SELECT/INSERT/UPDATE 映射到加密列并注册 TypeHandler
- [x] 3.3 更新 `OnboardingService`，提交/保存/更新时计算哈希索引，executeOnboarding 时设置 Employee 的 hash 字段

## 4. 薪资表加密改造

- [x] 4.1 数据库迁移：为 `salary_account` 表新增所有金额字段的加密列（已包含在 phase5-encryption.sql）
- [x] 4.2 更新 `SalaryAccountMapper.xml`，所有金额字段注册 `EncryptedBigDecimalTypeHandler`
- [x] 4.3 数据库迁移：为 `salary_record` 表新增所有金额字段的 `*_encrypted` 列
- [x] 4.4 更新 `SalaryRecordMapper.xml`，所有金额字段注册 `EncryptedBigDecimalTypeHandler`
- [x] 4.5 数据库迁移：为 `regularization_application` 表新增 `formal_salary_encrypted` 列
- [x] 4.6 更新 `RegularizationApplicationMapper.xml`，`formalSalary` 字段注册 `EncryptedBigDecimalTypeHandler`

## 5. 存量数据加密迁移

- [x] 5.1 编写 `DataEncryptionMigrator` Java 迁移工具类，支持分批（每批 1000 行）读取明文、加密、写回加密列，输出进度日志，支持幂等执行（跳过已加密行）
- [x] 5.2 实现 `employee` 表数据迁移
- [x] 5.3 实现 `onboarding_application` 表数据迁移
- [x] 5.4 实现 `salary_account` 表数据迁移
- [x] 5.5 实现 `salary_record` 表数据迁移
- [x] 5.6 实现 `regularization_application` 表数据迁移

## 6. 迁移验证与清理

- [x] 6.1 编写 `DataMigrationValidator` 数据完整性校验工具：随机抽取 5% 记录，解密后与明文原文逐字段对比
- [x] 6.2 在验证通过后，执行 DDL 脚本删除所有明文列（独立脚本，需人工确认后执行）→ `phase7-cleanup-plaintext.sql`
- [x] 6.3 将加密列重命名为原列名（如 `phone_encrypted` → `phone`），保持应用层代码的一致性 → `phase8-rename-encrypted.sql` + 5 个 Mapper XML 更新

## 7. 权限与安全加固

- [x] 7.1 确认 `DataScopeAspect` 中敏感字段的过滤逻辑不受加解密影响（VO 中字段已解密为明文后再进行权限过滤）
- [x] 7.2 确认 `EmployeeService` 中部门主管角色不可见 `idCard`、`bankAccount`、`baseSalary` 的权限控制仍然生效
- [x] 7.3 确认日志中不打印加密密钥或密文数据（已配置 `com.hrms.crypto: warn` 级别）

## 8. 测试

- [x] 8.1 编写 `EncryptionUtilTest` 单元测试：验证加密 → 解密的往返正确性，验证相同明文多次加密的差异（随机 IV vs 确定性 IV），验证篡改密文后解密失败的 GCM 认证检测
- [x] 8.2 编写 `EncryptedStringTypeHandlerTest` 和 `EncryptedBigDecimalTypeHandlerTest` 单元测试：H2 内存数据库 + 真实 JDBC，21 个用例全部通过
- [x] 8.3 编写 `EmployeeServiceTest` 集成测试：验证创建员工后数据库存的是密文，读取员工后返回的是明文 → `EmployeeServiceIntegrationTest`，8 个用例全部通过
- [x] 8.4 编写按手机号查询的集成测试：验证哈希索引查询的正确性 → 含在 `EmployeeServiceIntegrationTest` 中（手机号/身份证号/银行账号哈希查询 + 唯一性校验）
- [ ] 8.5 端到端测试：通过 EmployeeController API 创建 → 查询 → 更新员工，验证全链路加解密正确
- [ ] 8.6 性能基准测试：对比加密前后的 CRUD 操作耗时，确认性能影响在可接受范围内

## 9. 文档与部署

- [x] 9.1 编写密钥配置文档：说明如何生成 256-bit 密钥、如何设置环境变量、生产环境建议使用 KMS/Vault → `docs/encryption-key-guide.md`
- [ ] 9.2 编写部署 checklist：按顺序执行 DDL → 数据迁移 → 验证 → 删除明文列 → 部署新代码
- [x] 9.3 更新 `CLAUDE.md` 中技术栈部分，补充加密相关说明供后续 AI 辅助开发参考
