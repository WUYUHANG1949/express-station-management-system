package com.wuyuhang.delivery.mapper;

import com.wuyuhang.delivery.vo.NameValueVO;
import com.wuyuhang.delivery.vo.StatOverviewVO;
import com.wuyuhang.delivery.vo.StationRankVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 数据统计 Mapper。
 * <p>
 * 本接口不绑定实体，专门用于看板统计查询，SQL 写在
 * {@code resources/mapper/StatMapper.xml} 中。
 *
 * @author 吴宇航
 */
@Mapper
public interface StatMapper {

    /**
     * 首页概览指标，一次查询取回全部计数。
     *
     * @param stationId 驿站ID，为空表示统计全部驿站
     */
    StatOverviewVO selectOverview(@Param("stationId") Long stationId);

    /**
     * 按天统计入库量。返回 { "stat_date": '2026-10-01', "cnt": 3 } 列表。
     */
    List<Map<String, Object>> selectInTrend(@Param("stationId") Long stationId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    /**
     * 按天统计取件量。
     */
    List<Map<String, Object>> selectPickupTrend(@Param("stationId") Long stationId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    /**
     * 按快递公司统计快件量。
     */
    List<NameValueVO> selectCompanyStat(@Param("stationId") Long stationId);

    /**
     * 按快件类型统计快件量。
     */
    List<NameValueVO> selectParcelTypeStat(@Param("stationId") Long stationId);

    /**
     * 各驿站业务量排行（收件量、取件量），仅管理员可见。
     */
    List<StationRankVO> selectStationRank();
}
