package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.PageResult;
import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.dto.ParcelInStoreDTO;
import com.wuyuhang.delivery.dto.ParcelPickupDTO;
import com.wuyuhang.delivery.dto.ParcelUpdateDTO;
import com.wuyuhang.delivery.dto.query.ParcelQuery;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.entity.ParcelTrace;
import com.wuyuhang.delivery.entity.PickupRecord;
import com.wuyuhang.delivery.security.RequiresPermission;
import com.wuyuhang.delivery.service.ParcelService;
import com.wuyuhang.delivery.vo.OverdueFeeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 快件核心业务接口：收件登记、取件核销、查询、派送、导出。
 *
 * @author 吴宇航
 */
@Tag(name = "06-快件管理", description = "收件登记、取件核销、派送、查询与台账导出")
@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
public class ParcelController {

    private final ParcelService parcelService;

    @Operation(summary = "收件登记（入库）",
            description = "校验运单号格式、生成取件码、分配货位并写入轨迹；未指定货位时自动分配")
    @PostMapping("/in-store")
    @RequiresPermission("parcel:in")
    public Result<Parcel> inStore(@Valid @RequestBody ParcelInStoreDTO dto) {
        Parcel parcel = parcelService.inStore(dto);
        return Result.success("入库成功，取件码：" + parcel.getPickupCode(), parcel);
    }

    @Operation(summary = "取件核销（出库）",
            description = "校验取件码、更新快件状态、释放货位、写入取件记录与轨迹，全过程同一事务")
    @PostMapping("/pickup")
    @RequiresPermission("parcel:pickup")
    public Result<PickupRecord> pickup(@Valid @RequestBody ParcelPickupDTO dto) {
        PickupRecord record = parcelService.pickup(dto);
        return Result.success("核销成功，快件已交付", record);
    }

    @Operation(summary = "按关键字查询快件", description = "支持取件码、运单号、手机号、收件人姓名")
    @GetMapping("/query")
    @RequiresPermission("parcel:list")
    public Result<List<Parcel>> query(@RequestParam String keyword,
                                      @RequestParam(required = false) Long stationId) {
        return Result.success(parcelService.queryByKeyword(keyword, stationId));
    }

    @Operation(summary = "分页条件查询")
    @GetMapping("/page")
    @RequiresPermission("parcel:list")
    public Result<PageResult<Parcel>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           ParcelQuery query) {
        return Result.success(parcelService.page(pageNum, pageSize, query));
    }

    @Operation(summary = "快件详情")
    @GetMapping("/{id}")
    @RequiresPermission("parcel:list")
    public Result<Parcel> detail(@PathVariable Long id) {
        return Result.success(parcelService.detail(id));
    }

    @Operation(summary = "快件轨迹")
    @GetMapping("/{id}/traces")
    @RequiresPermission("parcel:list")
    public Result<List<ParcelTrace>> traces(@PathVariable Long id) {
        return Result.success(parcelService.traces(id));
    }

    @Operation(summary = "逾期保管费试算")
    @GetMapping("/{id}/overdue-fee")
    @RequiresPermission("parcel:list")
    public Result<OverdueFeeVO> overdueFee(@PathVariable Long id) {
        return Result.success(parcelService.overdueFee(id));
    }

    @Operation(summary = "编辑快件")
    @PutMapping("/{id}")
    @RequiresPermission("parcel:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ParcelUpdateDTO dto) {
        parcelService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除快件", description = "逻辑删除，同时释放占用的货位")
    @DeleteMapping("/{id}")
    @RequiresPermission("parcel:delete")
    public Result<Void> delete(@PathVariable Long id) {
        parcelService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "派送出库", description = "把在库快件改为派送中")
    @PutMapping("/{id}/deliver")
    @RequiresPermission("parcel:deliver")
    public Result<Void> deliver(@PathVariable Long id) {
        parcelService.deliver(id);
        return Result.success("已发起派送", null);
    }

    @Operation(summary = "导出台账", description = "按当前筛选条件导出 Excel，直接返回文件流")
    @GetMapping("/export")
    @RequiresPermission("parcel:export")
    public ResponseEntity<byte[]> export(ParcelQuery query) {
        byte[] bytes = parcelService.exportLedger(query);
        String fileName = URLEncoder.encode(parcelService.exportFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
