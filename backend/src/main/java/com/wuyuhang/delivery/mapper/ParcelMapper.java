package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wuyuhang.delivery.dto.query.ParcelQuery;
import com.wuyuhang.delivery.entity.Parcel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 快件 Mapper。
 * <p>
 * 快件列表需要同时关联驿站名称、库位编号、操作员姓名，
 * 因此查询语句写在 {@code resources/mapper/ParcelMapper.xml} 中，便于维护动态 SQL。
 *
 * @author 吴宇航
 */
@Mapper
public interface ParcelMapper extends BaseMapper<Parcel> {

    /**
     * 条件组合查询 + 分页。所有条件均为可选，为空时不参与拼接。
     */
    IPage<Parcel> selectParcelPage(IPage<Parcel> page, @Param("q") ParcelQuery query);

    /**
     * 条件组合查询（不分页），用于导出 Excel 台账。
     */
    List<Parcel> selectParcelList(@Param("q") ParcelQuery query);

    /**
     * 查询快件详情（含驿站名称、库位编号、操作员姓名）。
     */
    Parcel selectParcelDetail(@Param("id") Long id);

    /**
     * 取件核销页的模糊查询：支持取件码、运单号、收件人手机号、收件人姓名。
     *
     * @param keyword   查询关键字
     * @param stationId 限定驿站，可为空
     * @param onlyPhone 限定手机号（普通用户只能查自己的快件），可为空
     * @param limit     最多返回条数
     */
    List<Parcel> selectByKeyword(@Param("keyword") String keyword,
                                 @Param("stationId") Long stationId,
                                 @Param("onlyPhone") String onlyPhone,
                                 @Param("limit") Integer limit);

    /**
     * 统计逾期未取快件数量（在库且入库时间超过免费保管天数）。
     */
    int countOverdue(@Param("stationId") Long stationId);

    /**
     * 把某库位下的在库快件全部置为空库位（删除货位前调用）。
     */
    int clearShelf(@Param("shelfId") Long shelfId);
}
