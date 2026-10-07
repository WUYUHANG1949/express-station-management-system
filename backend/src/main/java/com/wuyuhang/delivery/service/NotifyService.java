package com.wuyuhang.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.enums.NotifyDict;
import com.wuyuhang.delivery.common.enums.ParcelStatus;
import com.wuyuhang.delivery.dto.NotifySendDTO;
import com.wuyuhang.delivery.dto.query.NotifyQuery;
import com.wuyuhang.delivery.entity.NotifyRecord;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.entity.Station;
import com.wuyuhang.delivery.mapper.NotifyRecordMapper;
import com.wuyuhang.delivery.mapper.ParcelMapper;
import com.wuyuhang.delivery.mapper.StationMapper;
import com.wuyuhang.delivery.security.LoginUser;
import com.wuyuhang.delivery.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 取件通知服务。
 * <p>
 * 驿站的核心痛点之一是"件到了但客户不知道"，本服务负责把到件、逾期、取件完成、
 * 异常等事件通知到收件人，并落库形成可追溯的通知记录。
 * <p>
 * 关于真实短信：项目以「写入通知记录」的方式模拟发送过程（见 {@link #dispatch}），
 * 对接阿里云/腾讯云短信网关时只需替换该方法内部实现，上层业务无需改动。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 单个手机号一次催取最多处理的快件数，避免误操作给客户发几十条短信 */
    private static final int MAX_BATCH_SIZE = 50;

    private final NotifyRecordMapper notifyMapper;
    private final ParcelMapper parcelMapper;
    private final StationMapper stationMapper;

    // ==================================================================
    //  一、查询
    // ==================================================================

    /**
     * 分页条件查询通知记录。
     * <p>
     * 数据权限：非管理员一律被强制收敛到本人所属驿站，
     * **即使用户在前端伪造 stationId 也查不到其它驿站的通知记录**。
     */
    public PageResult<NotifyRecord> page(long pageNum, long pageSize, NotifyQuery query) {
        LoginUser loginUser = UserContext.require();
        if (!loginUser.isAdmin()) {
            if (!loginUser.hasRole("STAFF")) {
                throw new BusinessException("无操作权限，请联系管理员");
            }
            query.setStationId(loginUser.getStationId());
        }
        var result = notifyMapper.selectNotifyPage(new Page<>(pageNum, pageSize), query);
        result.getRecords().forEach(this::fillDisplayFields);
        return PageResult.of(result);
    }

    /**
     * 查询某快件的全部通知记录。
     */
    public List<NotifyRecord> listByParcel(Long parcelId) {
        List<NotifyRecord> list = notifyMapper.selectByParcelId(parcelId);
        list.forEach(this::fillDisplayFields);
        return list;
    }

    /**
     * 今日发送量统计。
     */
    public int countToday(Long stationId) {
        return notifyMapper.countToday(stationId);
    }

    // ==================================================================
    //  二、发送通知
    // ==================================================================

    /**
     * 手动发送一条通知（员工在页面上点"通知客户"时调用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public NotifyRecord send(NotifySendDTO dto) {
        if (!NotifyDict.NotifyType.isValid(dto.getNotifyType())) {
            throw new BusinessException("非法的通知类型：" + dto.getNotifyType());
        }
        if (!NotifyDict.Channel.isValid(dto.getChannel())) {
            throw new BusinessException("非法的通知渠道：" + dto.getChannel());
        }
        Parcel parcel = parcelMapper.selectById(dto.getParcelId());
        if (parcel == null) {
            throw new BusinessException("快件不存在");
        }
        LoginUser loginUser = UserContext.require();

        String phone = StringUtils.hasText(dto.getReceiverPhone())
                ? dto.getReceiverPhone().trim() : parcel.getReceiverPhone();
        String content = StringUtils.hasText(dto.getContent())
                ? dto.getContent().trim() : buildContent(parcel, dto.getNotifyType());

        return dispatch(parcel, dto.getNotifyType(), dto.getChannel(), phone, content,
                loginUser.getUserId(), loginUser.getRealName());
    }

    /**
     * 批量催取：把某驿站（或全部驿站）逾期未取快件逐条发送催取通知。
     *
     * @param stationId 驿站ID，为空且当前用户为员工时取本驿站
     * @param minDays   至少逾期天数，默认 1
     * @return 实际发送条数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchNotifyOverdue(Long stationId, Integer minDays) {
        LoginUser loginUser = UserContext.require();
        // 非管理员一律强制收敛到本人所属驿站，防止越站给其它驿站的客户发短信
        Long scopeStationId = loginUser.isAdmin() ? stationId : loginUser.getStationId();
        int days = (minDays == null || minDays < 1) ? 1 : minDays;

        List<Parcel> overdueList = parcelMapper.selectOverdueList(scopeStationId, days, MAX_BATCH_SIZE);
        int sent = 0;
        for (Parcel parcel : overdueList) {
            // 同一快件当天已催取过则跳过，避免重复打扰客户
            if (notifyMapper.countTodayByParcelAndType(parcel.getId(), NotifyDict.NotifyType.OVERDUE.name()) > 0) {
                continue;
            }
            dispatch(parcel, NotifyDict.NotifyType.OVERDUE.name(), NotifyDict.Channel.SMS.name(),
                    parcel.getReceiverPhone(), buildContent(parcel, NotifyDict.NotifyType.OVERDUE.name()),
                    loginUser.getUserId(), loginUser.getRealName());
            sent++;
        }
        log.info("批量催取完成：驿站 {}，逾期 {} 天以上共 {} 件，实际发送 {} 条",
                scopeStationId, days, overdueList.size(), sent);
        return sent;
    }

    /**
     * 自动发送（入库、取件核销等业务动作触发）。
     * <p>
     * 通知失败不影响主业务：这里捕获异常并记日志，避免"短信发不出去导致快件入不了库"。
     *
     * @return 发送成功的记录；失败返回 null
     */
    public NotifyRecord autoSend(Parcel parcel, NotifyDict.NotifyType type, NotifyDict.Channel channel) {
        try {
            return dispatch(parcel, type.name(), channel.name(), parcel.getReceiverPhone(),
                    buildContent(parcel, type.name()), null, "系统自动发送");
        } catch (Exception e) {
            log.warn("自动发送通知失败：运单号 {}，类型 {}，原因 {}", parcel.getWaybillNo(), type.name(), e.getMessage());
            return null;
        }
    }

    // ==================================================================
    //  三、私有方法
    // ==================================================================

    /**
     * 投递通知：写入通知记录表。
     * <p>
     * 【对接真实短信网关的位置】把下面构造实体并 insert 的逻辑，
     * 换成调用短信服务商 SDK（如阿里云 dysmsapi），再用返回结果设置 sendStatus 即可。
     * 目前模拟规则：手机号为空或不是 11 位则记为发送失败，便于演示失败场景。
     */
    private NotifyRecord dispatch(Parcel parcel, String notifyType, String channel, String phone,
                                  String content, Long operatorId, String operatorName) {
        NotifyRecord record = new NotifyRecord();
        record.setParcelId(parcel.getId());
        record.setWaybillNo(parcel.getWaybillNo());
        record.setPickupCode(parcel.getPickupCode());
        record.setNotifyType(notifyType);
        record.setChannel(channel);
        record.setReceiverPhone(phone);
        record.setContent(content);
        record.setStationId(parcel.getStationId());
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setSendTime(LocalDateTime.now());

        // 模拟网关返回：手机号不合法则判定为发送失败
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            record.setSendStatus(NotifyDict.SendStatus.FAILED.name());
            record.setFailReason("接收号码不合法或为空，短信网关拒绝发送");
        } else {
            record.setSendStatus(NotifyDict.SendStatus.SUCCESS.name());
        }

        notifyMapper.insert(record);
        fillDisplayFields(record);
        return record;
    }

    /**
     * 按通知类型生成通知文案。
     */
    private String buildContent(Parcel parcel, String notifyType) {
        String stationName = null;
        String stationPhone = null;
        if (parcel.getStationId() != null) {
            Station station = stationMapper.selectById(parcel.getStationId());
            if (station != null) {
                stationName = station.getStationName();
                stationPhone = station.getContactPhone();
            }
        }
        String station = stationName == null ? "本驿站" : stationName;

        NotifyDict.NotifyType type;
        try {
            type = NotifyDict.NotifyType.valueOf(notifyType);
        } catch (IllegalArgumentException e) {
            type = NotifyDict.NotifyType.IN_STORE;
        }

        return switch (type) {
            case IN_STORE -> "【快件驿站】您的快件（" + parcel.getExpressCompany() + " " + parcel.getWaybillNo()
                    + "）已到达" + station + "，取件码 " + parcel.getPickupCode() + "，请凭取件码及时取件。";
            case OVERDUE -> {
                int storageDays = parcel.getInTime() == null ? 0
                        : (int) ChronoUnit.DAYS.between(parcel.getInTime().toLocalDate(), LocalDate.now());
                int freeDays = parcel.getOverdueDays() == null ? 3 : parcel.getOverdueDays();
                int overdueDays = Math.max(storageDays - freeDays, 1);
                yield "【快件驿站】您的快件（" + parcel.getWaybillNo() + "）已超过免费保管期 " + overdueDays
                        + " 天，逾期保管费 2 元/天，请尽快凭取件码 " + parcel.getPickupCode() + " 到" + station + "取件。";
            }
            case PICKUP_DONE -> "【快件驿站】您的快件（" + parcel.getWaybillNo() + "）已于 "
                    + (parcel.getPickupTime() == null ? LocalDateTime.now().format(TIME_FORMATTER)
                    : parcel.getPickupTime().format(TIME_FORMATTER))
                    + " 完成取件，感谢使用。";
            case EXCEPTION -> "【快件驿站】您的快件（" + parcel.getWaybillNo()
                    + "）在驿站出现异常情况，请及时联系" + station
                    + (stationPhone == null ? "。" : "（" + stationPhone + "）。");
        };
    }

    /**
     * 填充中文名称与展示字段。
     */
    private void fillDisplayFields(NotifyRecord record) {
        if (record == null) {
            return;
        }
        record.setNotifyTypeName(NotifyDict.NotifyType.labelOf(record.getNotifyType()));
        record.setChannelName(NotifyDict.Channel.labelOf(record.getChannel()));
        record.setSendStatusName(NotifyDict.SendStatus.labelOf(record.getSendStatus()));
    }

    /**
     * 统计某驿站待发送催取通知的快件数（用于页面角标提示）。
     * <p>
     * 非管理员强制收敛到本人所属驿站。
     */
    public int countOverduePending(Long stationId) {
        LoginUser loginUser = UserContext.require();
        Long scopeStationId = loginUser.isAdmin() ? stationId : loginUser.getStationId();
        List<Parcel> list = parcelMapper.selectOverdueList(scopeStationId, 1, MAX_BATCH_SIZE);
        return list.size();
    }

    /**
     * 快件状态中文名，供页面展示。
     */
    public String parcelStatusLabel(String status) {
        return ParcelStatus.labelOf(status);
    }
}
