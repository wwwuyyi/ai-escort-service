package com.example.escort.module.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Schema(description = "OCR任务结果")
public class OcrTaskVO {
    private Long taskId;
    private String status;
    private String ocrResult;
    private BigDecimal ocrConfidence;
    private Integer manualConfirmed;
    /** 原图地址（/upload/...，访问时需携带登录令牌） */
    private String imageUrl;
    private String errorMsg;
    private LocalDateTime createTime;
}