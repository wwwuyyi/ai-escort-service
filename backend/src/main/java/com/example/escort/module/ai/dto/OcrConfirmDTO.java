package com.example.escort.module.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * OCR人工确认请求。
 * correctedResult 同时兼容：
 * - JSON 对象（前端可编辑结果直接提交）
 * - JSON 字符串（自动化脚本/旧客户端）
 * 服务端统一序列化为字符串写入 medical_document.ocr_result。
 */
@Data
@Schema(description = "OCR确认请求")
public class OcrConfirmDTO {

    @Schema(description = "人工修正后的识别结果（JSON对象或JSON字符串）")
    private Object correctedResult;
}
