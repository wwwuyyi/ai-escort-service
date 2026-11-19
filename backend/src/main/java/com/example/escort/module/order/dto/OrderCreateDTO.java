package com.example.escort.module.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 下单请求DTO
 * 含幂等请求号（客户端生成，防重复提交）
 */
@Data
@Schema(description = "下单请求")
public class OrderCreateDTO {

    @NotBlank(message = "幂等请求号不能为空")
    @Size(max = 64, message = "幂等请求号长度不能超过64个字符")
    @Schema(description = "客户端请求号（幂等防重，同一请求号只处理一次）", example = "REQ20261027120000")
    private String requestId;

    @NotNull(message = "老人档案ID不能为空")
    @Schema(description = "老人档案ID")
    private Long elderlyId;

    @NotNull(message = "医院ID不能为空")
    @Schema(description = "就诊医院ID")
    private Long hospitalId;

    @NotBlank(message = "就诊科室不能为空")
    @Size(max = 50, message = "就诊科室长度不能超过50个字符")
    @Schema(description = "就诊科室")
    private String department;

    @NotBlank(message = "服务类型不能为空")
    @Pattern(regexp = "BASIC|EXAM", message = "服务类型仅支持BASIC或EXAM")
    @Schema(description = "服务类型：BASIC/EXAM")
    private String serviceType;

    @NotNull(message = "预约时间不能为空")
    @Future(message = "预约时间必须晚于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "预约就诊时间", example = "2026-11-01 09:00:00")
    private LocalDateTime appointmentTime;

    @NotNull(message = "服务时长不能为空")
    @DecimalMin(value = "1", message = "服务时长不能少于1小时")
    @DecimalMax(value = "24", message = "服务时长不能超过24小时")
    @Schema(description = "服务时长（小时）")
    private BigDecimal duration;

    @NotNull(message = "服务金额不能为空")
    @DecimalMin(value = "0.01", message = "服务金额必须大于0")
    @Schema(description = "确认金额（须与试算结果一致）")
    private BigDecimal amount;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "订单备注（如轮椅、家属陪同等）")
    private String remark;
}