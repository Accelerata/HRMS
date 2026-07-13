package com.hrms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社保公积金配置
 * 按城市/地区维护的五险一金缴纳比例及基数上下限
 */
@Data
public class SocialInsuranceConfig {

    /** 主键ID */
    private Long id;

    /** 城市/地区名称 */
    private String cityName;

    // ────────────── 社保个人缴纳比例 ──────────────

    /** 养老保险个人比例（如 0.08 = 8%） */
    private BigDecimal pensionPersonalRatio;

    /** 医疗保险个人比例（如 0.02 = 2%） */
    private BigDecimal medicalPersonalRatio;

    /** 失业保险个人比例（如 0.005 = 0.5%） */
    private BigDecimal unemploymentPersonalRatio;

    // ────────────── 社保公司缴纳比例 ──────────────

    /** 养老保险公司比例 */
    private BigDecimal pensionCompanyRatio;

    /** 医疗保险公司比例 */
    private BigDecimal medicalCompanyRatio;

    /** 失业保险公司比例 */
    private BigDecimal unemploymentCompanyRatio;

    /** 工伤保险比例（仅公司） */
    private BigDecimal injuryCompanyRatio;

    /** 生育保险比例（仅公司） */
    private BigDecimal maternityCompanyRatio;

    // ────────────── 公积金 ──────────────

    /** 公积金个人比例（如 0.07 = 7%） */
    private BigDecimal housingFundPersonalRatio;

    /** 公积金公司比例 */
    private BigDecimal housingFundCompanyRatio;

    // ────────────── 基数上下限 ──────────────

    /** 社保基数上限 */
    private BigDecimal socialInsuranceCeiling;

    /** 社保基数下限 */
    private BigDecimal socialInsuranceFloor;

    /** 公积金基数上限 */
    private BigDecimal housingFundCeiling;

    /** 公积金基数下限 */
    private BigDecimal housingFundFloor;

    // ────────────── 个税 ──────────────

    /** 个税起征点（默认5000） */
    private BigDecimal taxThreshold;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
