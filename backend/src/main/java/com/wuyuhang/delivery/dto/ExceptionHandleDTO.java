package com.wuyuhang.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 异常件处理请求参数。
 *
 * @author 吴宇航
 */
@Data
@Schema(description = "异常件处理请求")
public class ExceptionHandleDTO implements Serializable {

    @NotBlank(message = "处理状态不能为空")
    @Schema(description = "处理状态：PENDING/HANDLING/RESOLVED")
    private String handleStatus;

    @NotBlank(message = "处理结果不能为空")
    @Schema(description = "处理结果")
    private String handleResult;

    @Schema(description = "同步更新的快件状态，可为空；如 RETURNED 表示退回")
    private String parcelStatus;
}
