package com.example.escort.module.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "就医档案请求")
public class MedicalRecordDTO {
    @NotNull(message = "老人档案ID不能为空")
    private Long elderlyId;
    /** 关联订单（可选）；创建时会校验归属与老人一致性 */
    private Long orderId;
    @NotNull(message = "医院ID不能为空")
    private Long hospitalId;
    @NotBlank(message = "科室不能为空")
    @Size(max = 50, message = "科室长度不能超过50个字符")
    private String department;
    @NotNull(message = "就诊日期不能为空")
    private LocalDate visitDate;
    @Size(max = 500, message = "主诉长度不能超过500个字符")
    private String chiefComplaint;
    @Size(max = 500, message = "诊断结论长度不能超过500个字符")
    private String diagnosis;
    @Size(max = 4000, message = "处方长度不能超过4000个字符")
    private String prescription;
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}