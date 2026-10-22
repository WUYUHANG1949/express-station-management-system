package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.service.PublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面向收件人的公开接口（免登录）。
 * <p>
 * 收件人**不需要注册账号**，只需在自助查询页输入手机号、取件码或运单号即可查到取件码。
 * 本控制器下的路径已在 {@code WebMvcConfig} 中排除登录拦截，
 * 因此**不要在此暴露任何敏感字段**（内部 ID、驿站ID、备注、运费等已由
 * {@code PublicService} 主动置空；取件码与运单号查询时手机号还会打码）。
 *
 * @author 吴宇航
 */
@Tag(name = "11-公开接口", description = "收件人免登录自助查询取件码")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @Operation(summary = "自助查询取件码（免登录）",
            description = "入参自动识别三种格式：11 位手机号 / 8 位取件码 / 运单号。"
                    + "只返回仍在驿站（在库待取 / 派送中）的快件，已取件历史不返回；"
                    + "手机号查询最多返回 20 条，取件码与运单号查询时手机号打码")
    @GetMapping("/pickup-query")
    public Result<List<Parcel>> pickupQuery(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String phone) {
        // phone 为兼容旧调用方而保留，等同于 keyword
        String input = StringUtils.hasText(keyword) ? keyword : phone;
        List<Parcel> list = publicService.query(input);
        String message = list.isEmpty()
                ? "未查询到待取快件，请核对手机号 / 取件码 / 运单号"
                : "共查询到 " + list.size() + " 件待取快件";
        return Result.success(message, list);
    }
}
