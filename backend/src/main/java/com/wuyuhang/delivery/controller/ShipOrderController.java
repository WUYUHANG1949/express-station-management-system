package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.ShipOrderDTO;
import com.wuyuhang.delivery.dto.query.ShipOrderQuery;
import com.wuyuhang.delivery.entity.ShipOrder;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.ShipOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 寄件管理接口。
 *
 * @author 吴宇航
 */
@Tag(name = "07-寄件管理", description = "寄件登记、查询与状态流转")
@RestController
@RequestMapping("/api/ship-orders")
@RequiredArgsConstructor
public class ShipOrderController {

    private final ShipOrderService shipOrderService;

    @Operation(summary = "分页条件查询寄件单")
    @GetMapping("/page")
    @RequiresPermission("ship:list")
    public Result<PageResult<ShipOrder>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              ShipOrderQuery query) {
        return Result.success(shipOrderService.page(pageNum, pageSize, query));
    }

    @Operation(summary = "寄件单详情")
    @GetMapping("/{id}")
    @RequiresPermission("ship:list")
    public Result<ShipOrder> detail(@PathVariable Long id) {
        return Result.success(shipOrderService.detail(id));
    }

    @Operation(summary = "寄件登记", description = "系统自动生成寄件单号，初始状态为待揽收")
    @PostMapping
    @RequiresPermission("ship:add")
    public Result<Long> create(@Valid @RequestBody ShipOrderDTO dto) {
        return Result.success("寄件登记成功", shipOrderService.create(dto));
    }

    @Operation(summary = "编辑寄件单", description = "仅「待揽收」状态可修改")
    @PutMapping("/{id}")
    @RequiresPermission("ship:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ShipOrderDTO dto) {
        shipOrderService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "更新寄件单状态", description = "可同时回填快递公司运单号")
    @PutMapping("/{id}/status")
    @RequiresPermission("ship:status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam String status,
                                     @RequestParam(required = false) String waybillNo) {
        shipOrderService.updateStatus(id, status, waybillNo);
        return Result.success("状态已更新", null);
    }

    @Operation(summary = "删除寄件单")
    @DeleteMapping("/{id}")
    @RequiresPermission("ship:delete")
    public Result<Void> delete(@PathVariable Long id) {
        shipOrderService.delete(id);
        return Result.success("删除成功", null);
    }
}
