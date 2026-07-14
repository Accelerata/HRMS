package com.hrms.crypto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptionUtil 单元测试
 * 验证 AES-256-GCM 加解密往返正确性、确定性加密、GCM 防篡改特性
 */
class EncryptionUtilTest {

    private static EncryptionUtil encryptionUtil;

    @BeforeAll
    static void setUp() {
        // 生成测试用密钥（生产环境通过环境变量注入）
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) keyBytes[i] = (byte) i;
        String testKey = Base64.getEncoder().encodeToString(keyBytes);
        System.setProperty("HRMS_ENCRYPTION_KEY", testKey);

        EncryptionConfig config = new EncryptionConfig(testKey);
        encryptionUtil = new EncryptionUtil(config);
    }

    @Test
    void testEncryptDecryptRoundTrip() {
        String plaintext = "13800138000";
        String ciphertext = encryptionUtil.encrypt(plaintext);

        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext, "密文应与明文不同");
        assertTrue(ciphertext.length() > plaintext.length(), "密文长度应大于明文");

        String decrypted = encryptionUtil.decrypt(ciphertext);
        assertEquals(plaintext, decrypted, "解密后应与原文一致");
    }

    @Test
    void testEncryptDecryptChinese() {
        String plaintext = "北京市朝阳区建国路88号";
        String ciphertext = encryptionUtil.encrypt(plaintext);
        String decrypted = encryptionUtil.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testNullEncryptDecrypt() {
        assertNull(encryptionUtil.encrypt(null));
        assertNull(encryptionUtil.decrypt(null));
    }

    @Test
    void testEmptyStringEncryptDecrypt() {
        String result = encryptionUtil.encrypt("");
        String decrypted = encryptionUtil.decrypt(result);
        assertEquals("", decrypted);
    }

    @Test
    void testRandomIVProducesDifferentCiphertexts() {
        String plaintext = "13800138000";
        String ct1 = encryptionUtil.encrypt(plaintext);
        String ct2 = encryptionUtil.encrypt(plaintext);

        assertNotEquals(ct1, ct2, "随机 IV 应产生不同密文");
        // 但解密结果应该一致
        assertEquals(plaintext, encryptionUtil.decrypt(ct1));
        assertEquals(plaintext, encryptionUtil.decrypt(ct2));
    }

    @Test
    void testDeterministicEncryptionProducesSameCiphertext() {
        String plaintext = "110101198005152536";
        String ct1 = encryptionUtil.encryptDeterministic(plaintext);
        String ct2 = encryptionUtil.encryptDeterministic(plaintext);

        assertEquals(ct1, ct2, "确定性加密应产生相同密文");
        assertEquals(plaintext, encryptionUtil.decrypt(ct1));
    }

    @Test
    void testGcmTamperDetection() {
        String plaintext = "sensitive-data";
        String ciphertext = encryptionUtil.encrypt(plaintext);

        // 篡改密文: version(1) + IV(12) + ciphertext+tag
        // 我们在 ciphertext 区域修改，保持 version 和 IV 不变，避免影响 Base64 编码
        byte[] data = Base64.getDecoder().decode(ciphertext);
        // 跳过 version(1) + IV(12) = 13 bytes，修改 ciphertext 区域
        int tamperIndex = 13 + (data.length - 13) / 2;
        if (tamperIndex < data.length) {
            data[tamperIndex] ^= 0x01; // flip a bit in ciphertext
        }
        String tampered = Base64.getEncoder().encodeToString(data);

        assertThrows(RuntimeException.class, () -> {
            encryptionUtil.decrypt(tampered);
        }, "篡改密文应导致 GCM 认证失败");
    }

    @Test
    void testBigDecimalEncryptDecrypt() {
        BigDecimal salary = new BigDecimal("12345.67");
        String encrypted = encryptionUtil.encryptBigDecimal(salary);
        assertNotNull(encrypted);

        BigDecimal decrypted = encryptionUtil.decryptBigDecimal(encrypted);
        assertEquals(0, salary.compareTo(decrypted));
    }

    @Test
    void testBigDecimalNull() {
        assertNull(encryptionUtil.encryptBigDecimal(null));
        assertNull(encryptionUtil.decryptBigDecimal(null));
    }

    @Test
    void testComputeHash() {
        String plaintext = "13800138000";
        String hash = encryptionUtil.computeHash(plaintext);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 哈希应为 64 个十六进制字符");

        // 相同输入产生相同哈希
        String hash2 = encryptionUtil.computeHash(plaintext);
        assertEquals(hash, hash2);

        // 不同输入产生不同哈希
        String hash3 = encryptionUtil.computeHash("13900139000");
        assertNotEquals(hash, hash3);
    }

    @Test
    void testComputeHashNull() {
        assertNull(encryptionUtil.computeHash(null));
    }

    @Test
    void testPlaintextFallbackOnDecrypt() {
        // 传入明文 → 应安全返回原文
        String plaintext = "this-is-not-encrypted";
        String result = encryptionUtil.decrypt(plaintext);
        assertEquals(plaintext, result, "明文应原样返回");
    }

    @Test
    void testLongPlaintext() {
        // 测试较长文本（如地址）
        String address = "北京市海淀区中关村大街1号院SOHO大厦A座15层1506室".repeat(5);
        String encrypted = encryptionUtil.encrypt(address);
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals(address, decrypted);
    }
}
