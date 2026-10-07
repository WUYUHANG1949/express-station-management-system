package com.wuyuhang.delivery.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务单号生成工具。
 * <p>
 * 取件码：8 位数字，便于人工念给客户，也便于扫码枪输入；
 * 寄件单号：S + 年月日时分秒 + 2 位随机数，保证可读且基本不重复。
 *
 * @author 吴宇航
 */
public final class BizNoGenerator {

    private static final DateTimeFormatter ORDER_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 取件码下界，保证固定 8 位且首位不为 0 */
    private static final int PICKUP_CODE_MIN = 10_000_000;

    /** 取件码上界 */
    private static final int PICKUP_CODE_MAX = 99_999_999;

    private BizNoGenerator() {
    }

    /**
     * 生成 8 位数字取件码。是否重复由调用方查库确认。
     */
    public static String generatePickupCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(PICKUP_CODE_MIN, PICKUP_CODE_MAX + 1));
    }

    /**
     * 生成寄件单号，例如 S2026100718394712。
     */
    public static String generateShipOrderNo() {
        String time = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        int suffix = ThreadLocalRandom.current().nextInt(10, 100);
        return "S" + time + suffix;
    }
}
