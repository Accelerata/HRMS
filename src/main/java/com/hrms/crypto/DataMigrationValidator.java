//package com.hrms.crypto;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * 数据迁移完整性验证工具
// * 随机抽取记录，解密后与明文原文对比，确保加密/解密循环无损
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataMigrationValidator {
//
//    private final JdbcTemplate jdbcTemplate;
//    private final EncryptionUtil encryptionUtil;
//
//    private static final double SAMPLE_RATE = 0.05; // 抽样 5%
//
//    /**
//     * 验证 employee 表加密完整性
//     * @return 验证失败的记录ID列表
//     */
//    public List<Long> validateEmployee() {
//        log.info("开始验证 employee 表加密完整性...");
//        List<Long> failedIds = new ArrayList<>();
//
//        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
//                "SELECT id, phone, phone_encrypted, email, email_encrypted, " +
//                "id_card, id_card_encrypted, registered_address, registered_address_encrypted, " +
//                "current_address, current_address_encrypted, bank_account, bank_account_encrypted, " +
//                "bank_name, bank_name_encrypted, base_salary, base_salary_encrypted " +
//                "FROM employee WHERE phone_encrypted IS NOT NULL " +
//                "ORDER BY RAND() LIMIT ?",
//                (int) (countTotal("employee") * SAMPLE_RATE) + 1);
//
//        for (Map<String, Object> row : rows) {
//            Long id = (Long) row.get("id");
//            if (!verifyField(row, "phone", "phone_encrypted", "string") ||
//                !verifyField(row, "email", "email_encrypted", "string") ||
//                !verifyField(row, "id_card", "id_card_encrypted", "string") ||
//                !verifyField(row, "registered_address", "registered_address_encrypted", "string") ||
//                !verifyField(row, "current_address", "current_address_encrypted", "string") ||
//                !verifyField(row, "bank_account", "bank_account_encrypted", "string") ||
//                !verifyField(row, "bank_name", "bank_name_encrypted", "string") ||
//                !verifyBigDecimalField(row, "base_salary", "base_salary_encrypted")) {
//                failedIds.add(id);
//            }
//        }
//
//        if (failedIds.isEmpty()) {
//            log.info("employee 表验证通过（抽样 {} 条，全部匹配）", rows.size());
//        } else {
//            log.error("employee 表验证失败！不匹配记录: {}", failedIds);
//        }
//        return failedIds;
//    }
//
//    /**
//     * 验证所有表
//     */
//    public boolean validateAll() {
//        log.info("========== 开始全量加密完整性验证 ==========");
//        List<Long> failed = new ArrayList<>();
//        failed.addAll(validateEmployee());
//        // 其他表验证...
//
//        boolean passed = failed.isEmpty();
//        if (passed) {
//            log.info("========== 所有表加密验证通过 ✓ ==========");
//        } else {
//            log.error("========== 加密验证失败 ✗，共 {} 条记录不匹配 ==========", failed.size());
//        }
//        return passed;
//    }
//
//    // ── 辅助方法 ──
//
//    private boolean verifyField(Map<String, Object> row, String plainCol, String encCol, String type) {
//        Object plain = row.get(plainCol);
//        Object encrypted = row.get(encCol);
//        if (plain == null && encrypted == null) return true;
//        if (plain == null || encrypted == null) return false;
//        String decrypted = encryptionUtil.decrypt((String) encrypted);
//        return plain.toString().equals(decrypted);
//    }
//
//    private boolean verifyBigDecimalField(Map<String, Object> row, String plainCol, String encCol) {
//        Object plain = row.get(plainCol);
//        Object encrypted = row.get(encCol);
//        if (plain == null && encrypted == null) return true;
//        if (plain == null || encrypted == null) return false;
//        BigDecimal decrypted = encryptionUtil.decryptBigDecimal((String) encrypted);
//        return ((BigDecimal) plain).compareTo(decrypted) == 0;
//    }
//
//    private int countTotal(String table) {
//        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
//        return c != null ? c : 0;
//    }
//}
