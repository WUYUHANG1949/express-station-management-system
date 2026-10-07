package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.ExceptionCreateDTO;
import com.wuyuhang.delivery.dto.ExceptionHandleDTO;
import com.wuyuhang.delivery.dto.query.ExceptionQuery;
import com.wuyuhang.delivery.entity.ExceptionRecord;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常件管理接口。
 *
 * @author 吴宇航
 */
@Tag(name = "08-异常件管理", description = "异常件登记、查询与处理")
@RestController
@RequestMapping("/api/exceptions")
@RequiredArgsConstructor
public class ExceptionController {

    private final ExceptionService exceptionService;

    @Operation(summary = "分页条件查询异常件")
    @GetMapping("/page")
    @RequiresPermission("exception:list")
    public Result<PageResult<ExceptionRecord>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    ExceptionQuery query) {
        return Result.success(exceptionService.page(pageNum, pageSize, query));
    }

    @Operation(summary = "登记异常件", description = "登记后快件状态自动置为「异常件」并写入轨迹")
    @PostMapping
    @RequiresPermission("exception:add")
    public Result<Long> create(@Valid @RequestBody ExceptionCreateDTO dto) {
        return Result.success("异常件登记成功", exceptionService.create(dto));
    }

    @Operation(summary = "处理异常件", description = "可同步更新快件状态，例如退回")
    @PutMapping("/{id}/handle")
    @RequiresPermission("exception:handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody ExceptionHandleDTO dto) {
        exceptionService.handle(id, dto);
        return Result.success("处理完成", null);
    }
}
