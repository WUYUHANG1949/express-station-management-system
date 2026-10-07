package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 驿站 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface StationMapper extends BaseMapper<Station> {

    /**
     * 查询驿站列表，并带出货位数量与在库快件数，用于列表页展示。
     */
    @Select("""
            SELECT s.*,
                   (SELECT COUNT(*) FROM shelf sh WHERE sh.station_id = s.id)                        AS shelf_count,
                   (SELECT COUNT(*) FROM parcel p
                     WHERE p.station_id = s.id AND p.deleted = 0
                       AND p.status IN ('IN_STORE', 'DELIVERING'))                                   AS used_count
            FROM station s
            ORDER BY s.id
            """)
    List<Station> selectStationWithStat();

    /**
     * 查询某驿站在库快件数，删除驿站前做校验用。
     */
    @Select("""
            SELECT COUNT(*) FROM parcel
            WHERE station_id = #{stationId} AND deleted = 0 AND status IN ('IN_STORE', 'DELIVERING')
            """)
    int countInStoreParcel(@Param("stationId") Long stationId);
}
