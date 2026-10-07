package com.wuyuhang.delivery.service;

import com.wuyuhang.delivery.common.enums.ParcelType;
import com.wuyuhang.delivery.mapper.StatMapper;
import com.wuyuhang.delivery.mapper.StationMapper;
import com.wuyuhang.delivery.vo.NameValueVO;
import com.wuyuhang.delivery.vo.StatOverviewVO;
import com.wuyuhang.delivery.vo.StationRankVO;
import com.wuyuhang.delivery.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务。
 *
 * @author 吴宇航
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter AXIS_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    /** 趋势查询允许的最大天数，避免前端传入过大的区间拖慢数据库 */
    private static final int MAX_TREND_DAYS = 90;

    private final StatMapper statMapper;
    private final StationMapper stationMapper;

    /**
     * 首页概览指标。
     */
    public StatOverviewVO overview(Long stationId) {
        StatOverviewVO vo = statMapper.selectOverview(stationId);
        return vo == null ? new StatOverviewVO() : vo;
    }

    /**
     * 近 N 日出入库趋势。
     * <p>
     * SQL 只会返回"有数据"的日期，这里在 Java 侧补齐缺失的日期并填 0，
     * 保证前端折线图的 X 轴是连续的，不会出现日期跳跃。
     */
    public TrendVO trend(Integer days, Long stationId) {
        int span = (days == null || days <= 0) ? 7 : Math.min(days, MAX_TREND_DAYS);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(span - 1L);

        Map<String, Integer> inMap = toDateCountMap(
                statMapper.selectInTrend(stationId, startDate, endDate));
        Map<String, Integer> pickupMap = toDateCountMap(
                statMapper.selectPickupTrend(stationId, startDate, endDate));

        TrendVO vo = new TrendVO();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String fullDate = date.format(DATE_FORMATTER);
            vo.getDates().add(date.format(AXIS_FORMATTER));
            vo.getInCounts().add(inMap.getOrDefault(fullDate, 0));
            vo.getPickupCounts().add(pickupMap.getOrDefault(fullDate, 0));
        }
        return vo;
    }

    /**
     * 快递公司分布。
     */
    public List<NameValueVO> companyStat(Long stationId) {
        return statMapper.selectCompanyStat(stationId);
    }

    /**
     * 快件类型分布（把编码翻译成中文名称）。
     */
    public List<NameValueVO> parcelTypeStat(Long stationId) {
        List<NameValueVO> list = statMapper.selectParcelTypeStat(stationId);
        list.forEach(item -> item.setName(ParcelType.labelOf(item.getName())));
        return list;
    }

    /**
     * 驿站业务量排行，仅管理员可调用。
     */
    public List<StationRankVO> stationRank() {
        return statMapper.selectStationRank();
    }

    /**
     * 把 [{d=2026-10-01, c=3}] 形式的查询结果转成 Map，便于按日期取值。
     */
    private Map<String, Integer> toDateCountMap(List<Map<String, Object>> rows) {
        Map<String, Integer> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object date = row.get("d");
            Object count = row.get("c");
            if (date == null) {
                continue;
            }
            // JDBC 驱动可能返回 java.sql.Date 或 String，统一转成 yyyy-MM-dd 字符串
            String key = date instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate().format(DATE_FORMATTER)
                    : date.toString().substring(0, Math.min(10, date.toString().length()));
            map.put(key, count == null ? 0 : Integer.parseInt(count.toString()));
        }
        return map;
    }

    /**
     * 校验驿站是否存在（供 Controller 层做参数校验）。
     */
    public boolean stationExists(Long stationId) {
        return stationId == null || stationMapper.selectById(stationId) != null;
    }

    /**
     * 空趋势结果，用于异常兜底。
     */
    public TrendVO emptyTrend() {
        return new TrendVO();
    }

    /**
     * 空列表兜底。
     */
    public List<NameValueVO> emptyNameValues() {
        return new ArrayList<>();
    }
}
