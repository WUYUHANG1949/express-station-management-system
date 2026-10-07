package com.wuyuhang.delivery.controller;

import com.wuyuhang.delivery.common.Result;
import com.wuyuhang.delivery.entity.Parcel;
import com.wuyuhang.delivery.service.PublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面向收件人的公开接口（免登录）。
 * <p>
 * 对应真实驿站的 "输入手机号查取件码" 自助服务场景。
 * 本控制器下的路径已在 {@code WebMvcConfig} 中排除登录拦截，
 * 因此**不要在此暴露任何敏感字段**（内部 ID、驿站ID、备注、运费等已由
 * {@code PublicService} 主动置空）。
 *
 * @author 吴宇航
 */
@Tag(name = "11-公开接口", description = "收件人免登录自助查询取件码")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @Operation(summary = "按手机号查询取件码",
            description = "只返回仍在驿站（在库待取 / 派送中）的快件，已取件历史不返回，单次最多 20 条")
    @GetMapping("/pickup-query")
    public Result<List<Parcel>> pickupQuery(@RequestParam String phone) {
        List<Parcel> list = publicService.queryByPhone(phone);
        return Result.success("共查询到 " + list.size() + " 件待取快件", list);
    }
}
