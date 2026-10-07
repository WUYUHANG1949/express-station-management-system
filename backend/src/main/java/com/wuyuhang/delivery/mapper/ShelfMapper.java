package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.Shelf;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 货架库位 Mapper。
 * <p>
 * 库位占用/释放使用「带条件的 UPDATE」而不是「先查后改」，
 * 例如 {@code used_count < capacity} 作为更新条件，可以避免并发入库时超卖，
 * 影响行数为 0 即说明库位已满，由 Service 层抛出业务异常。
 *
 * @author 吴宇航
 */
@Mapper
public interface ShelfMapper extends BaseMapper<Shelf> {

    /**
     * 占用一个库位。返回 0 表示该库位已满。
     */
    @Update("""
            UPDATE shelf
            SET used_count = used_count + 1, update_time = NOW()
            WHERE id = #{shelfId} AND used_count < capacity
            """)
    int occupy(@Param("shelfId") Long shelfId);

    /**
     * 释放一个库位。返回 0 表示库位本就没有占用记录。
     */
    @Update("""
            UPDATE shelf
            SET used_count = CASE WHEN used_count > 0 THEN used_count - 1 ELSE 0 END, update_time = NOW()
            WHERE id = #{shelfId}
            """)
    int release(@Param("shelfId") Long shelfId);

    /**
     * 自动挑选一个剩余容量最大的可用库位。
     */
    @Select("""
            SELECT * FROM shelf
            WHERE station_id = #{stationId} AND status = 1 AND used_count < capacity
            ORDER BY (capacity - used_count) DESC, id ASC
            LIMIT 1
            """)
    Shelf selectBestAvailable(@Param("stationId") Long stationId);

    /**
     * 查询某驿站的货位列表，并带出驿站名称。
     */
    @Select("""
            SELECT sh.*, st.station_name
            FROM shelf sh
                     LEFT JOIN station st ON st.id = sh.station_id
            WHERE (#{stationId} IS NULL OR sh.station_id = #{stationId})
            ORDER BY sh.station_id, sh.shelf_code
            """)
    List<Shelf> selectShelfWithStation(@Param("stationId") Long stationId);

    /**
     * 按快件表重算某驿站全部库位的占用数量，用于修复历史数据不一致。
     * <p>
     * 占用口径：快件实体仍在货架上，即状态为 IN_STORE（在库待取）、
     * DELIVERING（派送中，货位仍为其保留）或 EXCEPTION（异常件，件还在驿站）。
     * 已取件与已退回的快件不再占用货位。
     */
    @Update("""
            UPDATE shelf sh
            SET sh.used_count = (SELECT COUNT(*)
                                 FROM parcel p
                                 WHERE p.shelf_id = sh.id
                                   AND p.deleted = 0
                                   AND p.status IN ('IN_STORE', 'DELIVERING', 'EXCEPTION'))
            WHERE (#{stationId} IS NULL OR sh.station_id = #{stationId})
            """)
    int recalcUsedCount(@Param("stationId") Long stationId);
}
