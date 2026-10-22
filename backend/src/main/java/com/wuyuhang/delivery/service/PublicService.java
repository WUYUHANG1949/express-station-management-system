package com.wuyuhang.delivery.service;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.enums.ParcelStatus;
import com.wuyuhang.delivery.common.enums.ParcelType;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.mapper.ParcelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * 面向收件人的公开服务（免登录）。
 * <p>
 * 收件人**不需要账号**，通过一个输入框自助查询：系统按入参格式自动识别三种查询方式。
 *
 * <table border="1">
 *   <caption>入参识别规则</caption>
 *   <tr><th>输入</th><th>识别为</th><th>返回</th></tr>
 *   <tr><td>11 位、1[3-9] 开头</td><td>手机号</td><td>该号码名下所有待取件（最多 20 条）</td></tr>
 *   <tr><td>8 位纯数字</td><td>取件码</td><td>对应的那一件</td></tr>
 *   <tr><td>2~4 位字母 + 6~18 位数字</td><td>运单号</td><td>对应的那一件</td></tr>
 * </table>
 *
 * <b>安全设计（三重收敛）：</b>
 * <ol>
 *   <li><b>只返回仍需来取的快件</b>：仅「在库待取」「派送中」，
 *       已取件与已退回的历史记录一律不返回，无法探测历史收件记录；</li>
 *   <li><b>限制返回条数与字段</b>：手机号查询最多 20 条；
 *       内部标识（驿站ID、库位ID、操作员、运费、备注）全部置空；</li>
 *   <li><b>非手机号查询时手机号打码</b>：取件码与运单号查询不该暴露完整手机号
 *       （运单号印在包裹上，任何人拿到包裹都能查），统一返回 {@code 139****0001}。
 *       用手机号查询时无需打码——号码本来就是查询者自己输入的。</li>
 * </ol>
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicService {

    /** 单次查询最多返回的快件条数 */
    private static final int MAX_RESULT = 20;

    /** 逾期保管费单价：元 / 天，与 ParcelService 保持一致 */
    private static final BigDecimal OVERDUE_FEE_PER_DAY = new BigDecimal("2.00");

    /** 手机号：11 位，1 开头，第二位 3-9 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    /** 取件码：8 位纯数字 */
    private static final String PICKUP_CODE_REGEX = "^\\d{8}$";
    /** 运单号：2~4 位字母前缀 + 6~18 位数字，例如 SF1234567890123 */
    private static final String WAYBILL_REGEX = "^[A-Za-z]{2,4}\\d{6,18}$";

    private final ParcelMapper parcelMapper;

    /**
     * 统一入口：按入参格式自动识别手机号 / 取件码 / 运单号并查询。
     *
     * @param keyword 手机号、取件码或运单号
     * @return 仍可自取的快件列表（未命中时返回空列表）
     */
    public List<Parcel> query(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            throw new BusinessException("请输入手机号、取件码或运单号");
        }

        // ① 手机号：返回该号码名下所有待取件，不打码
        if (kw.matches(PHONE_REGEX)) {
            List<Parcel> list = parcelMapper.selectPublicByPhone(kw, MAX_RESULT);
            list.forEach(this::fillPublicFields);
            log.info("公开查询[手机号] {} -> {} 件", maskPhone(kw), list.size());
            return list;
        }

        // ② 取件码：精确一件，手机号打码
        if (kw.matches(PICKUP_CODE_REGEX)) {
            Parcel parcel = parcelMapper.selectPublicByPickupCode(kw);
            log.info("公开查询[取件码] {} -> {}", kw, parcel == null ? "未命中" : "命中 1 件");
            return singleMasked(parcel);
        }

        // ③ 运单号：精确一件，手机号打码
        if (kw.matches(WAYBILL_REGEX)) {
            Parcel parcel = parcelMapper.selectPublicByWaybillNo(kw.toUpperCase());
            log.info("公开查询[运单号] {} -> {}", kw.toUpperCase(), parcel == null ? "未命中" : "命中 1 件");
            return singleMasked(parcel);
        }

        throw new BusinessException("请输入 11 位手机号、8 位取件码或运单号（如 SF1234567890123）");
    }

    /** 单条结果：填充展示字段并把手机号打码 */
    private List<Parcel> singleMasked(Parcel parcel) {
        if (parcel == null) {
            return Collections.emptyList();
        }
        fillPublicFields(parcel);
        parcel.setReceiverPhone(maskPhone(parcel.getReceiverPhone()));
        return List.of(parcel);
    }

    /**
     * 手机号打码：13900000001 -> 139****0001。
     * 非 11 位手机号原样返回（避免把异常数据截断成更奇怪的样子）。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 填充状态名、类型名、保管天数与应缴保管费，并隐藏与取件无关的内部标识。
     */
    private void fillPublicFields(Parcel parcel) {
        parcel.setStatusName(ParcelStatus.labelOf(parcel.getStatus()));
        parcel.setParcelTypeName(ParcelType.labelOf(parcel.getParcelType()));
        // 隐藏与取件无关的内部字段：驿站ID/库位ID/操作员ID/操作员姓名/运费/备注
        parcel.setOperatorId(null);
        parcel.setOperatorName(null);
        parcel.setStationId(null);
        parcel.setShelfId(null);
        parcel.setFreight(null);
        parcel.setRemark(null);

        int storageDays = parcel.getInTime() == null ? 0
                : (int) ChronoUnit.DAYS.between(parcel.getInTime().toLocalDate(), LocalDate.now());
        int freeDays = parcel.getOverdueDays() == null ? 3 : parcel.getOverdueDays();
        int overdueDays = Math.max(storageDays - freeDays, 0);
        parcel.setStorageDays(storageDays);
        parcel.setOverdueFee(OVERDUE_FEE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays)));
        // overdueDays 字段在公开接口中复用为「还可免费保管天数」，负数表示已逾期
        parcel.setOverdueDays(Math.max(freeDays - storageDays, 0));
    }
}
