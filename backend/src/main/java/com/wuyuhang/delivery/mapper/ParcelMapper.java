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
     *
     * @param stationId  驿站ID，为空表示全部驿站（仅管理员可用）
     * @param limitPhone 数据权限：仅统计该手机号名下的快件（普通用户），可为空
     */
    int countOverdue(@Param("stationId") Long stationId, @Param("limitPhone") String limitPhone);

    /**
     * 分页查询逾期未取快件。
     * <p>
     * 「逾期」判定：已保管天数（DATEDIFF(NOW(), in_time)）超过免费保管天数 overdue_days，
     * 超出部分达到 minDays 天才算入结果，便于按「逾期 1 天 / 3 天 / 7 天以上」分层催取。
     *
     * @param stationId  驿站ID，为空表示全部驿站
     * @param minDays    至少逾期天数，默认 1
     * @param limitPhone 数据权限：仅查询该手机号的快件（普通用户），可为空
     */
    IPage<Parcel> selectOverduePage(IPage<Parcel> page,
                                    @Param("stationId") Long stationId,
                                    @Param("minDays") Integer minDays,
                                    @Param("limitPhone") String limitPhone);

    /**
     * 查询逾期未取快件列表（不分页），供批量催取使用。
     */
    List<Parcel> selectOverdueList(@Param("stationId") Long stationId,
                                   @Param("minDays") Integer minDays,
                                   @Param("limit") Integer limit);

    /**
     * 公开取件码查询：按手机号查询仍在驿站的快件（免登录页面使用）。
     * <p>
     * 只返回在库待取与派送中的快件，且限制返回条数，
     * 避免通过手机号探测到历史快件信息。
     */
    List<Parcel> selectPublicByPhone(@Param("phone") String phone, @Param("limit") Integer limit);

    /**
     * 查询仍占用货位的快件，供货位地图按库位分组展示。
     *
     * @param stationId 驿站ID，为空表示全部驿站
     */
    List<Parcel> selectOnShelfParcels(@Param("stationId") Long stationId);

    /**
     * 最近入库的快件（数据大屏滚动展示用）。
     */
    List<Parcel> selectRecentInStore(@Param("stationId") Long stationId, @Param("limit") Integer limit);

    /**
     * 最近取件的快件（数据大屏滚动展示用）。
     */
    List<Parcel> selectRecentPickup(@Param("stationId") Long stationId, @Param("limit") Integer limit);

    /**
     * 把某库位下的在库快件全部置为空库位（删除货位前调用）。
     */
    int clearShelf(@Param("shelfId") Long shelfId);
}
