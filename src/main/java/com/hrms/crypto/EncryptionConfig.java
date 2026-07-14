package com.hrms.crypto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 加密配置
 * 从环境变量读取主密钥，通过 HKDF 派生数据加密密钥和 HMAC 密钥
 */
@Slf4j
@Configuration
public class EncryptionConfig {

    /** 是否启用加密（默认 true） */
    @Getter
    @Value("${hrms.encryption.enabled:true}")
    private boolean enabled = true;

    /** HMAC 密钥（用于确定性 IV 派生和哈希索引） */
    @Getter
    private SecretKey hmacKey;

    /** AES 数据加密密钥 */
    @Getter
    private SecretKey dataEncryptionKey;

    /** HMAC pepper 字符串（用于哈希索引加盐） */
    @Getter
    private String pepper;

    public EncryptionConfig(@Value("${HRMS_ENCRYPTION_KEY:}") String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            if (enabled) {
                throw new IllegalStateException(
                        "HRMS_ENCRYPTION_KEY environment variable is required when hrms.encryption.enabled=true");
            }
            log.warn("加密已禁用 (hrms.encryption.enabled=false)，敏感数据将以明文存储！");
            return;
        }

        byte[] masterKeyBytes;
        try {
            masterKeyBytes = Base64.getDecoder().decode(masterKeyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("HRMS_ENCRYPTION_KEY must be a valid Base64-encoded 256-bit key", e);
        }

        if (masterKeyBytes.length != 32) {
            throw new IllegalStateException(
                    "HRMS_ENCRYPTION_KEY must be 32 bytes (256 bits) when Base64-decoded, got " + masterKeyBytes.length);
        }

        SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "HmacSHA256");

        // HKDF 派生: DEK = HKDF-Expand(masterKey, "hrms-dek", 32)
        this.dataEncryptionKey = new SecretKeySpec(
                hkdfExpand(masterKey, "hrms-dek", 32), "AES");

        // HKDF 派生: HMAC Key = HKDF-Expand(masterKey, "hrms-hmac", 32)
        this.hmacKey = new SecretKeySpec(
                hkdfExpand(masterKey, "hrms-hmac", 32), "HmacSHA256");

        // HKDF 派生: Pepper = Base64(HKDF-Expand(masterKey, "hrms-pepper", 16))
        this.pepper = Base64.getEncoder().encodeToString(
                hkdfExpand(masterKey, "hrms-pepper", 16));

        log.info("加密模块初始化完成 (AES-256-GCM, 密钥派生: HKDF-HMAC-SHA256)");
    }

    /**
     * 简化的 HKDF-Expand（RFC 5869 Section 2.3）
     * OKM = HMAC-SHA256(PRK, info || 0x01)
     */
    private byte[] hkdfExpand(SecretKey prk, String info, int length) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(prk);
            mac.update(info.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            mac.update((byte) 0x01);
            byte[] result = mac.doFinal();
            if (result.length < length) {
                throw new IllegalStateException("HKDF-Expand output too short");
            }
            byte[] truncated = new byte[length];
            System.arraycopy(result, 0, truncated, 0, length);
            return truncated;
        } catch (Exception e) {
            throw new RuntimeException("HKDF-Expand failed", e);
        }
    }
}
