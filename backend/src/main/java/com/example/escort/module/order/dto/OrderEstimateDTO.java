package com.example.escort.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "费用试算请求")
public class OrderEstimateDTO {
    @NotBlank(message = "服务类型不能为空")
    @Pattern(regexp = "BASIC|EXAM", message = "服务类型仅支持BASIC或EXAM")
    @Schema(description = "服务类型：BASIC/EXAM")
    private String serviceType;
    @NotNull(message = "服务时长不能为空")
    @DecimalMin(value = "1", message = "服务时长不能少于1小时")
    @DecimalMax(value = "24", message = "服务时长不能超过24小时")
    @Schema(description = "服务时长（小时）")
    private BigDecimal duration;
}