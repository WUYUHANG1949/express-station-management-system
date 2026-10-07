package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.BusinessException;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.common.ResultCode;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.security.UserContext;
import com.wuyuhang.delivery.service.StatService;
import com.wuyuhang.delivery.vo.NameValueVO;
import com.wuyuhang.delivery.vo.ScreenVO;
import com.wuyuhang.delivery.vo.StatOverviewVO;
import com.wuyuhang.delivery.vo.StationRankVO;
import com.wuyuhang.delivery.vo.TrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据统计接口。
 *
 * @author 吴宇航
 */
@Tag(name = "09-数据统计", description = "首页概览与各维度统计图表数据")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @Operation(summary = "首页概览指标")
    @GetMapping("/overview")
    @RequiresPermission("dashboard")
    public Result<StatOverviewVO> overview(@RequestParam(required = false) Long stationId) {
        return Result.success(statService.overview(resolveStationId(stationId)));
    }

    @Operation(summary = "近 N 日出入库趋势")
    @GetMapping("/trend")
    @RequiresPermission("dashboard")
    public Result<TrendVO> trend(@RequestParam(defaultValue = "7") Integer days,
                                 @RequestParam(required = false) Long stationId) {
        return Result.success(statService.trend(days, resolveStationId(stationId)));
    }

    @Operation(summary = "快递公司分布")
    @GetMapping("/company")
    @RequiresPermission("dashboard")
    public Result<List<NameValueVO>> company(@RequestParam(required = false) Long stationId) {
        return Result.success(statService.companyStat(resolveStationId(stationId)));
    }

    @Operation(summary = "快件类型分布")
    @GetMapping("/parcel-type")
    @RequiresPermission("dashboard")
    public Result<List<NameValueVO>> parcelType(@RequestParam(required = false) Long stationId) {
        return Result.success(statService.parcelTypeStat(resolveStationId(stationId)));
    }

    @Operation(summary = "数据大屏聚合数据",
            description = "一次请求返回大屏所需的全部指标，避免前端并发 7 个请求")
    @GetMapping("/screen")
    @RequiresPermission("screen")
    public Result<ScreenVO> screen(@RequestParam(required = false) Long stationId) {
        return Result.success(statService.screen(resolveStationId(stationId)));
    }

    @Operation(summary = "驿站业务量排行", description = "仅系统管理员可查看")
    @GetMapping("/station-rank")
    @RequiresPermission("stats:view")
    public Result<List<StationRankVO>> stationRank() {
        if (!UserContext.require().isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return Result.success(statService.stationRank());
    }

    /**
     * 数据范围收敛：员工只看本驿站，管理员可指定或查看全部。
     */
    private Long resolveStationId(Long stationId) {
        var loginUser = UserContext.require();
        if (loginUser.isAdmin()) {
            return stationId;
        }
        return loginUser.getStationId();
    }
}
