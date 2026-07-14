//package com.hrms.crypto;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Map;
//
///**
// * 存量数据加密迁移工具
// * 将已存在的明文敏感数据分批加密后写入加密列
// * 支持幂等执行（跳过已加密行）、进度输出、分批提交
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataEncryptionMigrator {
//
//    private final JdbcTemplate jdbcTemplate;
//    private final EncryptionUtil encryptionUtil;
//
//    private static final int BATCH_SIZE = 1000;
//
//    // ═══════════════ Employee 表迁移 ═══════════════
//
//    /**
//     * 迁移 employee 表的敏感字段
//     */
//    public void migrateEmployee() {
//        if (!encryptionUtil.isEnabled()) {
//            log.warn("加密未启用，跳过 employee 表迁移");
//            return;
//        }
//
//        int total = countNonNullPlaintext("employee", "phone");
//        log.info("开始迁移 employee 表: 共 {} 条明文记录", total);
//
//        int offset = 0;
//        int migrated = 0;
//
//        while (offset < total) {
//            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                    "SELECT id, phone, email, id_card, registered_address, current_address, " +
//                    "bank_account, bank_name, base_salary " +
//                    "FROM employee WHERE phone IS NOT NULL AND phone_encrypted IS NULL " +
//                    "LIMIT ? OFFSET ?", BATCH_SIZE, offset);
//
//            if (rows.isEmpty()) break;
//
//            for (Map<String, Object> row : rows) {
//                Long id = (Long) row.get("id");
//                String phone = (String) row.get("phone");
//                String email = (String) row.get("email");
//                String idCard = (String) row.get("id_card");
//                String registeredAddress = (String) row.get("registered_address");
//                String currentAddress = (String) row.get("current_address");
//                String bankAccount = (String) row.get("bank_account");
//                String bankName = (String) row.get("bank_name");
//                BigDecimal baseSalary = (BigDecimal) row.get("base_salary");
//
//                jdbcTemplate.update(
//                        "UPDATE employee SET " +
//                        "phone_encrypted = ?, phone_hash = ?, " +
//                        "email_encrypted = ?, " +
//                        "id_card_encrypted = ?, id_card_hash = ?, " +
//                        "registered_address_encrypted = ?, " +
//                        "current_address_encrypted = ?, " +
//                        "bank_account_encrypted = ?, bank_account_hash = ?, " +
//                        "bank_name_encrypted = ?, " +
//                        "base_salary_encrypted = ? " +
//                        "WHERE id = ?",
//                        encrypt(phone), hash(phone),
//                        encrypt(email),
//                        encrypt(idCard), hash(idCard),
//                        encrypt(registeredAddress),
//                        encrypt(currentAddress),
//                        encrypt(bankAccount), hash(bankAccount),
//                        encrypt(bankName),
//                        encryptBigDecimal(baseSalary),
//                        id);
//                migrated++;
//            }
//
//            offset += rows.size();
//            log.info("employee 迁移进度: {}/{} ({}%)", migrated, total,
//                    total > 0 ? migrated * 100 / total : 100);
//        }
//        log.info("employee 表迁移完成: 共加密 {} 条记录", migrated);
//    }
//
//    // ═══════════════ OnboardingApplication 表迁移 ═══════════════
//
//    public void migrateOnboardingApplication() {
//        if (!encryptionUtil.isEnabled()) return;
//
//        int total = countNonNullPlaintext("onboarding_application", "phone");
//        log.info("开始迁移 onboarding_application 表: 共 {} 条明文记录", total);
//
//        int offset = 0;
//        int migrated = 0;
//
//        while (offset < total) {
//            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                    "SELECT id, phone, email, id_card, registered_address, current_address, " +
//                    "bank_account, bank_name, offer_salary " +
//                    "FROM onboarding_application WHERE phone IS NOT NULL AND phone_encrypted IS NULL " +
//                    "LIMIT ? OFFSET ?", BATCH_SIZE, offset);
//
//            if (rows.isEmpty()) break;
//
//            for (Map<String, Object> row : rows) {
//                Long id = (Long) row.get("id");
//                jdbcTemplate.update(
//                        "UPDATE onboarding_application SET " +
//                        "phone_encrypted = ?, phone_hash = ?, " +
//                        "email_encrypted = ?, " +
//                        "id_card_encrypted = ?, id_card_hash = ?, " +
//                        "registered_address_encrypted = ?, " +
//                        "current_address_encrypted = ?, " +
//                        "bank_account_encrypted = ?, bank_account_hash = ?, " +
//                        "bank_name_encrypted = ?, " +
//                        "offer_salary_encrypted = ? " +
//                        "WHERE id = ?",
//                        encrypt((String) row.get("phone")),
//                        hash((String) row.get("phone")),
//                        encrypt((String) row.get("email")),
//                        encrypt((String) row.get("id_card")),
//                        hash((String) row.get("id_card")),
//                        encrypt((String) row.get("registered_address")),
//                        encrypt((String) row.get("current_address")),
//                        encrypt((String) row.get("bank_account")),
//                        hash((String) row.get("bank_account")),
//                        encrypt((String) row.get("bank_name")),
//                        encryptBigDecimal((BigDecimal) row.get("offer_salary")),
//                        id);
//                migrated++;
//            }
//            offset += rows.size();
//            log.info("onboarding_application 迁移进度: {}/{}", migrated, total);
//        }
//        log.info("onboarding_application 表迁移完成: 共加密 {} 条记录", migrated);
//    }
//
//    // ═══════════════ SalaryAccount 表迁移 ═══════════════
//
//    public void migrateSalaryAccount() {
//        if (!encryptionUtil.isEnabled()) return;
//
//        int total = countNonNullPlaintext("salary_account", "basic_salary");
//        log.info("开始迁移 salary_account 表: 共 {} 条明文记录", total);
//
//        int offset = 0;
//        int migrated = 0;
//
//        while (offset < total) {
//            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                    "SELECT id, basic_salary, position_salary, performance_salary, " +
//                    "social_insurance_base, housing_fund_base " +
//                    "FROM salary_account WHERE basic_salary IS NOT NULL AND basic_salary_encrypted IS NULL " +
//                    "LIMIT ? OFFSET ?", BATCH_SIZE, offset);
//
//            if (rows.isEmpty()) break;
//
//            for (Map<String, Object> row : rows) {
//                Long id = (Long) row.get("id");
//                jdbcTemplate.update(
//                        "UPDATE salary_account SET " +
//                        "basic_salary_encrypted = ?, " +
//                        "position_salary_encrypted = ?, " +
//                        "performance_salary_encrypted = ?, " +
//                        "social_insurance_base_encrypted = ?, " +
//                        "housing_fund_base_encrypted = ? " +
//                        "WHERE id = ?",
//                        encryptBigDecimal((BigDecimal) row.get("basic_salary")),
//                        encryptBigDecimal((BigDecimal) row.get("position_salary")),
//                        encryptBigDecimal((BigDecimal) row.get("performance_salary")),
//                        encryptBigDecimal((BigDecimal) row.get("social_insurance_base")),
//                        encryptBigDecimal((BigDecimal) row.get("housing_fund_base")),
//                        id);
//                migrated++;
//            }
//            offset += rows.size();
//            log.info("salary_account 迁移进度: {}/{}", migrated, total);
//        }
//        log.info("salary_account 表迁移完成: 共加密 {} 条记录", migrated);
//    }
//
//    // ═══════════════ SalaryRecord 表迁移 ═══════════════
//
//    public void migrateSalaryRecord() {
//        if (!encryptionUtil.isEnabled()) return;
//
//        int total = countNonNullPlaintext("salary_record", "basic_salary");
//        log.info("开始迁移 salary_record 表: 共 {} 条明文记录", total);
//
//        int offset = 0;
//        int migrated = 0;
//
//        String[] fields = {"basic_salary", "attendance_deduction", "leave_deduction",
//                "overtime_pay", "gross_pay", "social_insurance_personal", "housing_fund_personal",
//                "taxable_income", "tax", "other_deduction", "net_pay"};
//
//        while (offset < total) {
//            StringBuilder sql = new StringBuilder("SELECT id");
//            for (String f : fields) sql.append(", ").append(f);
//            sql.append(" FROM salary_record WHERE basic_salary IS NOT NULL AND basic_salary_encrypted IS NULL LIMIT ? OFFSET ?");
//
//            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                    sql.toString(), BATCH_SIZE, offset);
//
//            if (rows.isEmpty()) break;
//
//            for (Map<String, Object> row : rows) {
//                Long id = (Long) row.get("id");
//                StringBuilder updateSql = new StringBuilder("UPDATE salary_record SET ");
//                for (String f : fields) {
//                    if (!updateSql.toString().endsWith("SET ")) updateSql.append(", ");
//                    updateSql.append(f).append("_encrypted = ?");
//                }
//                updateSql.append(" WHERE id = ?");
//
//                Object[] params = new Object[fields.length + 1];
//                for (int i = 0; i < fields.length; i++) {
//                    params[i] = encryptBigDecimal((BigDecimal) row.get(fields[i]));
//                }
//                params[fields.length] = id;
//                jdbcTemplate.update(updateSql.toString(), params);
//                migrated++;
//            }
//            offset += rows.size();
//            log.info("salary_record 迁移进度: {}/{}", migrated, total);
//        }
//        log.info("salary_record 表迁移完成: 共加密 {} 条记录", migrated);
//    }
//
//    // ═══════════════ RegularizationApplication 表迁移 ═══════════════
//
//    public void migrateRegularizationApplication() {
//        if (!encryptionUtil.isEnabled()) return;
//
//        int total = countNonNullPlaintext("regularization_application", "formal_salary");
//        log.info("开始迁移 regularization_application 表: 共 {} 条明文记录", total);
//
//        int offset = 0;
//        int migrated = 0;
//
//        while (offset < total) {
//            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                    "SELECT id, formal_salary FROM regularization_application " +
//                    "WHERE formal_salary IS NOT NULL AND formal_salary_encrypted IS NULL " +
//                    "LIMIT ? OFFSET ?", BATCH_SIZE, offset);
//
//            if (rows.isEmpty()) break;
//
//            for (Map<String, Object> row : rows) {
//                Long id = (Long) row.get("id");
//                jdbcTemplate.update(
//                        "UPDATE regularization_application SET formal_salary_encrypted = ? WHERE id = ?",
//                        encryptBigDecimal((BigDecimal) row.get("formal_salary")), id);
//                migrated++;
//            }
//            offset += rows.size();
//            log.info("regularization_application 迁移进度: {}/{}", migrated, total);
//        }
//        log.info("regularization_application 表迁移完成: 共加密 {} 条记录", migrated);
//    }
//
//    // ═══════════════ 全量迁移 ═══════════════
//
//    /**
//     * 执行全部表的加密迁移
//     */
//    public void migrateAll() {
//        log.info("========== 开始全量数据加密迁移 ==========");
//        migrateEmployee();
//        migrateOnboardingApplication();
//        migrateSalaryAccount();
//        migrateSalaryRecord();
//        migrateRegularizationApplication();
//        log.info("========== 全量数据加密迁移完成 ==========");
//    }
//
//    // ═══════════════ 辅助方法 ═══════════════
//
//    private String encrypt(String plaintext) {
//        return encryptionUtil.encrypt(plaintext);
//    }
//
//    private String encryptBigDecimal(BigDecimal value) {
//        return encryptionUtil.encryptBigDecimal(value);
//    }
//
//    private String hash(String plaintext) {
//        return encryptionUtil.computeHash(plaintext);
//    }
//
//    private int countNonNullPlaintext(String table, String column) {
//        Integer count = jdbcTemplate.queryForObject(
//                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " IS NOT NULL",
//                Integer.class);
//        return count != null ? count : 0;
//    }
//}
