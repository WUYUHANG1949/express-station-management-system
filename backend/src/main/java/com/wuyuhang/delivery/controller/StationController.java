package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.StationSaveDTO;
import com.wuyuhang.delivery.entity.Station;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.StationService;
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
 * 驿站管理接口。
 *
 * @author 吴宇航
 */
@Tag(name = "04-驿站管理", description = "驿站信息的维护")
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @Operation(summary = "查询全部启用驿站", description = "供下拉框使用，所有登录用户可读")
    @GetMapping("/list")
    public Result<List<Station>> list() {
        return Result.success(stationService.list());
    }

    @Operation(summary = "分页查询驿站")
    @GetMapping("/page")
    @RequiresPermission("system:station:list")
    public Result<PageResult<Station>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String stationName,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(stationService.page(pageNum, pageSize, stationName, status));
    }

    @Operation(summary = "新增驿站")
    @PostMapping
    @RequiresPermission("system:station:edit")
    public Result<Long> create(@Valid @RequestBody StationSaveDTO dto) {
        return Result.success("新增成功", stationService.create(dto));
    }

    @Operation(summary = "编辑驿站")
    @PutMapping("/{id}")
    @RequiresPermission("system:station:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StationSaveDTO dto) {
        stationService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除驿站")
    @DeleteMapping("/{id}")
    @RequiresPermission("system:station:edit")
    public Result<Void> delete(@PathVariable Long id) {
        stationService.delete(id);
        return Result.success("删除成功", null);
    }
}
