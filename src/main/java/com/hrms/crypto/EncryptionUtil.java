package com.hrms.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类
 * 提供 AES-256-GCM 加解密、确定性 IV 派生、SHA-256 哈希计算
 *
 * <h3>密文格式</h3>
 * <pre>
 * [version:1B][IV:12B][ciphertext+tag:variable] → Base64编码 → 存储
 * version: 0x01 = AES-256-GCM, 随机IV
 *          0x02 = AES-256-GCM, 确定性IV (HMAC派生)
 * </pre>
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final int GCM_IV_LENGTH = 12;        // 96 bits
    private static final int GCM_TAG_LENGTH = 128;      // 128 bits
    private static final byte VERSION_RANDOM = 0x01;    // 随机 IV
    private static final byte VERSION_DETERMINISTIC = 0x02; // 确定性 IV

    private final EncryptionConfig config;
    private final SecureRandom secureRandom;

    public EncryptionUtil(EncryptionConfig config) {
        this.config = config;
        this.secureRandom = new SecureRandom();
    }

    public EncryptionConfig getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    // ────────────────────── 加密 ──────────────────────

    /**
     * 加密字符串（随机 IV）
     */
    public String encrypt(String plaintext) {
        return encryptInternal(plaintext, null);
    }

    /**
     * 确定性加密（HMAC 派生 IV），相同明文产生相同密文
     */
    public String encryptDeterministic(String plaintext) {
        byte[] iv = deriveIV(plaintext);
        return encryptInternal(plaintext, iv);
    }

    /**
     * 加密 BigDecimal（先转字符串）
     */
    public String encryptBigDecimal(java.math.BigDecimal value) {
        if (value == null) return null;
        return encrypt(value.toPlainString());
    }

    private String encryptInternal(String plaintext, byte[] fixedIv) {
        if (!config.isEnabled()) return plaintext;
        if (plaintext == null) return null;

        try {
            byte[] iv = (fixedIv != null) ? fixedIv : generateRandomIV();
            byte version = (fixedIv != null) ? VERSION_DETERMINISTIC : VERSION_RANDOM;

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, config.getDataEncryptionKey(), spec);

            byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plainBytes);

            // 组装: version(1) + IV(12) + ciphertext+tag
            ByteBuffer buffer = ByteBuffer.allocate(1 + GCM_IV_LENGTH + ciphertext.length);
            buffer.put(version);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    // ────────────────────── 解密 ──────────────────────

    /**
     * 解密字符串
     */
    public String decrypt(String ciphertext) {
        if (!config.isEnabled()) return ciphertext;
        if (ciphertext == null || ciphertext.isBlank()) return ciphertext;

        try {
            byte[] data = Base64.getDecoder().decode(ciphertext);
            if (data.length < 1 + GCM_IV_LENGTH + 1) {
                // 数据格式异常，可能是未加密的明文，直接返回
                log.warn("解密发现异常数据格式，回退返回原文");
                return ciphertext;
            }

            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte version = buffer.get();

            // 仅支持 v1 和 v2
            if (version != VERSION_RANDOM && version != VERSION_DETERMINISTIC) {
                log.warn("不支持的加密版本: 0x{}，回退返回原文", Integer.toHexString(version));
                return ciphertext;
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, config.getDataEncryptionKey(), spec);

            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (javax.crypto.AEADBadTagException e) {
            log.error("GCM 认证失败 — 密文可能被篡改或密钥不匹配");
            throw new RuntimeException("Decryption authentication failed", e);
        } catch (IllegalArgumentException e) {
            // Base64 解码失败 → 可能是明文
            log.debug("非密文格式，当作明文返回");
            return ciphertext;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /**
     * 解密并还原为 BigDecimal
     */
    public java.math.BigDecimal decryptBigDecimal(String ciphertext) {
        if (!config.isEnabled()) {
            return ciphertext != null ? new java.math.BigDecimal(ciphertext) : null;
        }
        if (ciphertext == null || ciphertext.isBlank()) return null;
        String plain = decrypt(ciphertext);
        return new java.math.BigDecimal(plain);
    }

    // ────────────────────── 哈希索引 ──────────────────────

    /**
     * 计算 SHA-256 哈希（用于精确查询索引）
     * hash = SHA-256(plaintext || pepper)
     */
    public String computeHash(String plaintext) {
        if (!config.isEnabled()) return null;
        if (plaintext == null) return null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(plaintext.getBytes(StandardCharsets.UTF_8));
            md.update(config.getPepper().getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return bytesToHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hash computation failed", e);
        }
    }

    // ────────────────────── 内部方法 ──────────────────────

    /**
     * 生成随机 IV
     */
    private byte[] generateRandomIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * 派生确定性 IV (HMAC-SHA256 取前 12 字节)
     * IV = HMAC-SHA256(hmacKey, plaintext)[0:12]
     */
    private byte[] deriveIV(String plaintext) {
        try {
            SecretKey hmacKey = config.getHmacKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] result = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(result, 0, iv, 0, GCM_IV_LENGTH);
            return iv;
        } catch (Exception e) {
            throw new RuntimeException("IV derivation failed", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
