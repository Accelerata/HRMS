package com.hrms.crypto;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BigDecimal 类型加密 TypeHandler
 * 写入: BigDecimal → String → AES加密 → Base64密文
 * 读取: Base64密文 → AES解密 → String → BigDecimal
 */
@MappedTypes(BigDecimal.class)
public class EncryptedBigDecimalTypeHandler extends BaseTypeHandler<BigDecimal> {

    private static volatile EncryptionUtil encryptionUtil;

    public EncryptedBigDecimalTypeHandler() {
        super();
    }

    public static void setEncryptionUtil(EncryptionUtil util) {
        encryptionUtil = util;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, BigDecimal parameter, JdbcType jdbcType)
            throws SQLException {
        if (encryptionUtil != null) {
            ps.setString(i, encryptionUtil.encryptBigDecimal(parameter));
        } else {
            ps.setBigDecimal(i, parameter);
        }
    }

    @Override
    public BigDecimal getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decryptIfNeeded(value);
    }

    @Override
    public BigDecimal getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decryptIfNeeded(value);
    }

    @Override
    public BigDecimal getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decryptIfNeeded(value);
    }

    private BigDecimal decryptIfNeeded(String value) {
        if (value == null || encryptionUtil == null) return null;
        return encryptionUtil.decryptBigDecimal(value);
    }
}
