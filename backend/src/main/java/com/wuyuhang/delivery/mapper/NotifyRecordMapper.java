package com.wuyuhang.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wuyuhang.delivery.dto.query.NotifyQuery;
import com.wuyuhang.delivery.entity.NotifyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知记录 Mapper。
 *
 * @author 吴宇航
 */
@Mapper
public interface NotifyRecordMapper extends BaseMapper<NotifyRecord> {

    /**
     * 条件组合查询 + 分页（关联快件收件人与驿站名称）。
     */
    IPage<NotifyRecord> selectNotifyPage(IPage<NotifyRecord> page, @Param("q") NotifyQuery query);

    /**
     * 查询某快件的全部通知记录，时间倒序。
     */
    List<NotifyRecord> selectByParcelId(@Param("parcelId") Long parcelId);

    /**
     * 统计某快件今日已发送的指定类型通知数量，避免重复打扰客户。
     */
    int countTodayByParcelAndType(@Param("parcelId") Long parcelId,
                                  @Param("notifyType") String notifyType);

    /**
     * 统计今日发送量（供首页概览使用）。
     */
    int countToday(@Param("stationId") Long stationId);
}
