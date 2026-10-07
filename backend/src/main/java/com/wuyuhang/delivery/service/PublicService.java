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
import java.util.List;

/**
 * 面向收件人的公开服务（免登录）。
 * <p>
 * 提供「输入手机号自助查询取件码」能力，对应真实驿站的取件码自助查询场景。
 * <p>
 * 安全设计说明：接口只返回**仍在驿站**（在库待取 / 派送中）的快件，
 * 已取件与已退回的历史记录不返回；同时限制单次最多返回 20 条。
 * 这样即使他人知道手机号，也只能看到"当前确实需要来取"的件，
 * 无法探测该号码的历史收件记录。该接口不需要登录，但仅暴露取件必要字段。
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

    private final ParcelMapper parcelMapper;

    /**
     * 按手机号查询可自取的快件（含取件码）。
     *
     * @param phone 收件人手机号
     * @return 在库待取 / 派送中的快件列表，附带中文状态名与应缴保管费
     */
    public List<Parcel> queryByPhone(String phone) {
        if (phone == null || !phone.trim().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的 11 位手机号");
        }
        List<Parcel> list = parcelMapper.selectPublicByPhone(phone.trim(), MAX_RESULT);
        list.forEach(this::fillPublicFields);
        log.info("公开查询：手机号 {} 查询到 {} 件可自取快件", phone, list.size());
        return list;
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
