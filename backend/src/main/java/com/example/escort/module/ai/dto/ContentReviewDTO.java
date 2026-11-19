package com.example.escort.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "AI内容人工复核请求")
public class ContentReviewDTO {

    @NotBlank(message = "复核状态不能为空")
    @Pattern(regexp = "APPROVED|REJECTED", message = "复核状态仅支持APPROVED或REJECTED")
    @Schema(description = "复核状态：APPROVED/REJECTED")
    private String reviewStatus;

    @Schema(description = "复核备注")
    private String reviewRemark;
}
