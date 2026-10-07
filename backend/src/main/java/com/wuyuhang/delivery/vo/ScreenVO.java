package com.wuyuhang.delivery.vo;

import com.wuyuhang.delivery.entity.Parcel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据大屏聚合数据。
 * <p>
 * 大屏需要在一屏内展示多个维度的指标，若前端逐个接口拉取会产生 6~7 次请求，
 * 首屏又要求快速出现，因此后端用一次请求聚合返回全部数据。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "数据大屏聚合数据")
public class ScreenVO implements Serializable {

    @Schema(description = "核心指标概览")
    private StatOverviewVO overview = new StatOverviewVO();

    @Schema(description = "近 14 日出入库趋势")
    private TrendVO trend = new TrendVO();

    @Schema(description = "快递公司分布")
    private List<NameValueVO> company = new ArrayList<>();

    @Schema(description = "快件类型分布")
    private List<NameValueVO> parcelType = new ArrayList<>();

    @Schema(description = "驿站业务量排行（仅管理员有数据）")
    private List<StationRankVO> stationRank = new ArrayList<>();

    @Schema(description = "最近入库快件")
    private List<Parcel> recentInStore = new ArrayList<>();

    @Schema(description = "最近取件快件")
    private List<Parcel> recentPickup = new ArrayList<>();

    @Schema(description = "逾期最久的快件")
    private List<Parcel> topOverdue = new ArrayList<>();

    @Schema(description = "今日通知发送条数")
    private Integer notifyToday = 0;

    @Schema(description = "服务器时间，大屏右上角显示")
    private LocalDateTime serverTime;
}
