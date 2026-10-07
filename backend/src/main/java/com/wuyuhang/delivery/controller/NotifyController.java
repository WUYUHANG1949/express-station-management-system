package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.NotifySendDTO;
import com.wuyuhang.delivery.dto.query.NotifyQuery;
import com.wuyuhang.delivery.entity.NotifyRecord;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 取件通知接口。
 *
 * @author 吴宇航
 */
@Tag(name = "10-通知管理", description = "到件通知、逾期催取与通知记录查询")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @Operation(summary = "分页查询通知记录")
    @GetMapping("/page")
    @RequiresPermission("notify:list")
    public Result<PageResult<NotifyRecord>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 NotifyQuery query) {
        return Result.success(notifyService.page(pageNum, pageSize, query));
    }

    @Operation(summary = "查询某快件的全部通知记录")
    @GetMapping("/parcel/{parcelId}")
    @RequiresPermission({"notify:list", "parcel:list"})
    public Result<List<NotifyRecord>> listByParcel(@PathVariable Long parcelId) {
        return Result.success(notifyService.listByParcel(parcelId));
    }

    @Operation(summary = "发送通知", description = "手动通知客户取件；不传内容时按类型自动生成文案")
    @PostMapping
    @RequiresPermission("notify:send")
    public Result<NotifyRecord> send(@Valid @RequestBody NotifySendDTO dto) {
        NotifyRecord record = notifyService.send(dto);
        return Result.success("通知已发送", record);
    }

    @Operation(summary = "批量催取逾期件", description = "给逾期未取的快件逐条发送催取通知，同一快件当天不重复发送")
    @PostMapping("/batch-overdue")
    @RequiresPermission("notify:send")
    public Result<Integer> batchOverdue(@RequestParam(required = false) Long stationId,
                                        @RequestParam(required = false, defaultValue = "1") Integer minDays) {
        int sent = notifyService.batchNotifyOverdue(stationId, minDays);
        return Result.success("本次共发送 " + sent + " 条催取通知", sent);
    }

    @Operation(summary = "统计待催取的逾期件数量")
    @GetMapping("/overdue-pending")
    @RequiresPermission("notify:list")
    public Result<Integer> overduePending(@RequestParam(required = false) Long stationId) {
        return Result.success(notifyService.countOverduePending(stationId));
    }
}
