package com.wuyuhang.delivery.common.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 快递公司运单号校验工具。
 * <p>
 * 不同快递公司的运单号编码规则不同（前缀字母 + 定长数字，或纯数字），
 * 收件登记时先按所选快递公司做正则校验，可以拦截掉大量手工录入错误。
 *
 * @author 吴宇航
 */
public final class ExpressCompanyUtil {

    private ExpressCompanyUtil() {
    }

    /** 支持的快递公司及其运单号正则（按录入顺序展示） */
    private static final Map<String, Pattern> COMPANY_PATTERNS = new LinkedHashMap<>();

    /** 兜底规则：未收录的快递公司，只做通用的字母数字校验 */
    private static final Pattern GENERIC_PATTERN = Pattern.compile("^[A-Za-z0-9]{8,40}$");

    static {
        // 顺丰速运：SF + 12~15 位数字，例如 SF1234567890123
        COMPANY_PATTERNS.put("顺丰速运", Pattern.compile("^SF\\d{12,15}$"));
        // 京东物流：JD + 10~16 位字母数字，例如 JD9988776655441
        COMPANY_PATTERNS.put("京东物流", Pattern.compile("^JD[A-Za-z0-9]{10,16}$"));
        // 中通快递：ZT 前缀或纯数字
        COMPANY_PATTERNS.put("中通快递", Pattern.compile("^(ZT\\d{12,15}|\\d{12,14})$"));
        // 圆通速递：YT 前缀或纯数字
        COMPANY_PATTERNS.put("圆通速递", Pattern.compile("^(YT\\d{12,15}|\\d{10,14})$"));
        // 申通快递：STO 前缀或纯数字，常见 13 位
        COMPANY_PATTERNS.put("申通快递", Pattern.compile("^(STO\\d{12,15}|\\d{12,15})$"));
        // 韵达快递：YD 前缀或纯数字，常见 13/15 位
        COMPANY_PATTERNS.put("韵达快递", Pattern.compile("^(YD\\d{12,15}|\\d{13,15})$"));
        // 邮政EMS：EMS + 数字，或国际件「2 位字母 + 9 位数字 + 2 位字母（CN）」
        COMPANY_PATTERNS.put("邮政EMS", Pattern.compile("^(EMS\\d{12,15}|[A-Z]{2}\\d{9}[A-Z]{2})$"));
        // 极兔速递：JT 前缀或纯数字
        COMPANY_PATTERNS.put("极兔速递", Pattern.compile("^(JT\\d{12,15}|\\d{12,15})$"));
    }

    /**
     * 获取系统支持的快递公司列表。
     */
    public static java.util.Set<String> supportedCompanies() {
        return COMPANY_PATTERNS.keySet();
    }

    /**
     * 校验运单号是否符合指定快递公司的编码规则。
     *
     * @param company    快递公司名称
     * @param waybillNo  运单号
     * @return true 表示格式正确
     */
    public static boolean isValid(String company, String waybillNo) {
        if (waybillNo == null || waybillNo.isBlank()) {
            return false;
        }
        String no = waybillNo.trim().toUpperCase();
        Pattern pattern = COMPANY_PATTERNS.get(company);
        if (pattern == null) {
            return GENERIC_PATTERN.matcher(no).matches();
        }
        return pattern.matcher(no).matches();
    }

    /**
     * 为指定快递公司生成运单号格式的提示语，用于前端和错误信息展示。
     */
    public static String ruleHint(String company) {
        return switch (company == null ? "" : company) {
            case "顺丰速运" -> "顺丰速运运单号以 SF 开头，后接 12~15 位数字，例如 SF1234567890123";
            case "京东物流" -> "京东物流运单号以 JD 开头，后接 10~16 位字母或数字，例如 JD9988776655441";
            case "中通快递" -> "中通快递运单号为 ZT 开头的 12~15 位编码，或 12~14 位纯数字";
            case "圆通速递" -> "圆通速递运单号为 YT 开头的 12~15 位编码，或 10~14 位纯数字";
            case "申通快递" -> "申通快递运单号为 STO 开头的 12~15 位编码，或 12~15 位纯数字";
            case "韵达快递" -> "韵达快递运单号为 YD 开头的 12~15 位编码，或 13~15 位纯数字";
            case "邮政EMS" -> "邮政 EMS 运单号为 EMS 加数字，或「2 位字母 + 9 位数字 + 2 位字母」的国际件格式";
            case "极兔速递" -> "极兔速递运单号为 JT 开头的 12~15 位编码，或 12~15 位纯数字";
            default -> "运单号为 8~40 位字母或数字组合";
        };
    }
}
