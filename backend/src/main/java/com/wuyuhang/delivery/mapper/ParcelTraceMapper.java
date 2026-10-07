package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyuhang.delivery.entity.ParcelTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 快件轨迹 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface ParcelTraceMapper extends BaseMapper<ParcelTrace> {

    /**
     * 按快件ID查询轨迹，时间正序（最早的入库记录在最上面）。
     */
    @Select("""
            SELECT * FROM parcel_trace
            WHERE parcel_id = #{parcelId}
            ORDER BY operate_time ASC, id ASC
            """)
    List<ParcelTrace> selectByParcelId(@Param("parcelId") Long parcelId);
}
