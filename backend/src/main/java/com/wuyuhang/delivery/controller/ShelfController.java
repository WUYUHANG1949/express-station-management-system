package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.ShelfSaveDTO;
import com.wuyuhang.delivery.entity.Shelf;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.ShelfService;
import com.wuyuhang.delivery.vo.ShelfMapVO;
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

import java.util.List;

/**
 * 货位管理接口。
 *
 * @author 吴宇航
 */
@Tag(name = "05-货位管理", description = "货架库位的维护与查询")
@RestController
@RequestMapping("/api/shelves")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;

    @Operation(summary = "货位地图", description = "按库位返回占用情况与该库位上的快件清单，供前端画货架网格图")
    @GetMapping("/map")
    @RequiresPermission("shelfmap:view")
    public Result<List<ShelfMapVO>> shelfMap(@RequestParam(required = false) Long stationId) {
        return Result.success(shelfService.shelfMap(stationId));
    }

    @Operation(summary = "查询某驿站的全部货位")
    @GetMapping("/list")
    public Result<List<Shelf>> list(@RequestParam(required = false) Long stationId) {
        return Result.success(shelfService.list(stationId));
    }

    @Operation(summary = "查询仍有剩余容量的货位", description = "收件登记时选择货位用")
    @GetMapping("/available")
    public Result<List<Shelf>> available(@RequestParam Long stationId) {
        return Result.success(shelfService.available(stationId));
    }

    @Operation(summary = "分页查询货位")
    @GetMapping("/page")
    @RequiresPermission("system:shelf:list")
    public Result<PageResult<Shelf>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Long stationId,
                                          @RequestParam(required = false) String shelfCode,
                                          @RequestParam(required = false) String area) {
        return Result.success(shelfService.page(pageNum, pageSize, stationId, shelfCode, area));
    }

    @Operation(summary = "新增货位")
    @PostMapping
    @RequiresPermission("system:shelf:edit")
    public Result<Long> create(@Valid @RequestBody ShelfSaveDTO dto) {
        return Result.success("新增成功", shelfService.create(dto));
    }

    @Operation(summary = "编辑货位")
    @PutMapping("/{id}")
    @RequiresPermission("system:shelf:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ShelfSaveDTO dto) {
        shelfService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除货位")
    @DeleteMapping("/{id}")
    @RequiresPermission("system:shelf:edit")
    public Result<Void> delete(@PathVariable Long id) {
        shelfService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "重算货位占用数量", description = "用于修复异常中断造成的库存不一致")
    @PutMapping("/recalculate")
    @RequiresPermission("system:shelf:edit")
    public Result<Integer> recalculate(@RequestParam(required = false) Long stationId) {
        int rows = shelfService.recalculate(stationId);
        return Result.success("重算完成，共更新 " + rows + " 个货位", rows);
    }
}
