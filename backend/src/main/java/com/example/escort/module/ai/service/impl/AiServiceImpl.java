package com.example.escort.module.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.escort.common.*;
import com.example.escort.common.SimpleCircuitBreaker;
import com.example.escort.config.AiProperties;
import com.example.escort.module.ai.dto.*;
import com.example.escort.module.ai.entity.AiGeneratedContent;
import com.example.escort.module.ai.entity.AiTask;
import com.example.escort.module.ai.mapper.AiGeneratedContentMapper;
import com.example.escort.module.ai.mapper.AiTaskMapper;
import com.example.escort.module.ai.service.AiService;
import com.example.escort.module.ai.vo.ContentVO;
import com.example.escort.module.ai.vo.OcrTaskVO;
import com.example.escort.module.elderly.entity.ElderlyProfile;
import com.example.escort.module.elderly.mapper.ElderlyProfileMapper;
import com.example.escort.module.order.entity.EscortOrder;
import com.example.escort.module.order.mapper.EscortOrderMapper;
import com.example.escort.module.record.entity.MedicalDocument;
import com.example.escort.module.record.entity.MedicalRecord;
import com.example.escort.module.record.mapper.MedicalDocumentMapper;
import com.example.escort.module.record.mapper.MedicalRecordMapper;
import com.example.escort.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j @Service
public class AiServiceImpl implements AiService {

    private final AiTaskMapper taskMapper;
    private final AiGeneratedContentMapper contentMapper;
    private final MedicalDocumentMapper docMapper;
    private final MedicalRecordMapper recordMapper;
    private final EscortOrderMapper orderMapper;
    private final ElderlyProfileMapper elderlyMapper;
    private final AiHttpClient httpClient;
    private final ThreadPoolTaskExecutor aiExecutor;
    private final SimpleCircuitBreaker ocrBreaker;
    private final SimpleCircuitBreaker llmBreaker;
    private final AiProperties aiProps;
    private static final String DISCLAIMER = "AI生成内容仅供参考，不构成任何诊疗建议";
    /** 告知书/解读生成：要求模型输出可直接渲染的HTML片段 */
    private static final String GEN_SYSTEM_PROMPT =
            "你是【陪诊助医AI健康助手】，服务对象是老年人及其家属。请根据用户提供的信息生成内容。\n"
            + "输出要求：\n"
            + "1. 只输出适合网页直接展示的HTML片段，可使用 <h3>、<p>、<ul>、<li>、<strong> 标签；"
            + "不要输出Markdown、代码块，也不要输出 <html> 或 <body> 标签。\n"
            + "2. 语言通俗、简洁，适合老年人阅读。\n"
            + "3. 内容仅为通用就医常识，不构成诊疗建议；结尾必须包含一句“AI生成内容仅供参考，不构成任何诊疗建议”。";

    public AiServiceImpl(AiTaskMapper tm, AiGeneratedContentMapper cm, MedicalDocumentMapper dm,
            MedicalRecordMapper rm, EscortOrderMapper om, ElderlyProfileMapper em, AiHttpClient hc,
            AiProperties ap,
            @Qualifier("aiTaskExecutor") ThreadPoolTaskExecutor ex,
            @Qualifier("ocrBreaker") SimpleCircuitBreaker ob,
            @Qualifier("llmBreaker") SimpleCircuitBreaker lb) {
        taskMapper=tm; contentMapper=cm; docMapper=dm; recordMapper=rm; orderMapper=om; elderlyMapper=em;
        httpClient=hc; aiProps=ap; aiExecutor=ex; ocrBreaker=ob; llmBreaker=lb;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public OcrTaskVO submitOcr(Long userId, OcrSubmitDTO dto) {
        MedicalDocument doc = docMapper.selectById(dto.getDocumentId());
        if (doc == null) throw new BizException(ResultCode.NOT_FOUND, "单据不存在");
        checkEscortDocAccess(userId, doc, true);
        AiTask exist = taskMapper.selectOne(new LambdaQueryWrapper<AiTask>()
                .eq(AiTask::getBizId, doc.getId()).eq(AiTask::getTaskType, "OCR"));
        if (exist != null && "RUNNING".equals(exist.getStatus())) return toOcrVO(exist, doc);
        if (exist != null && "SUCCESS".equals(exist.getStatus())) return toOcrVO(exist, doc);
        AiTask task = new AiTask(); task.setTaskType("OCR"); task.setBizId(doc.getId());
        task.setStatus("RUNNING"); task.setRetryCount(0); task.setStartTime(LocalDateTime.now());
        task.setRequestDigest("docId="+doc.getId()+",type="+doc.getDocType());
        taskMapper.insert(task); doc.setOcrStatus("PROCESSING"); docMapper.updateById(doc);
        // 必须等事务提交后再派发异步任务，否则异步线程读不到尚未提交的ai_task记录
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { aiExecutor.execute(() -> executeOcr(task.getId(), doc)); }
            });
        } else {
            aiExecutor.execute(() -> executeOcr(task.getId(), doc));
        }
        return toOcrVO(task, doc);
    }

    private void executeOcr(Long taskId, MedicalDocument doc) {
        AiTask task = taskMapper.selectById(taskId);
        if (task == null) { log.warn("OCR任务不存在，跳过 taskId={}", taskId); return; }
        if (!ocrBreaker.allowRequest()) { degrade(task, doc, "OCR熔断降级"); return; }
        LocalDateTime start = LocalDateTime.now();
        try {
            String resp = retryCall(
                    () -> httpClient.ocrPost("/recognize","{\"imageUrl\":\""+doc.getImageUrl()+"\"}"), task);
            OcrParsed p = parseOcr(resp);
            task.setStatus("SUCCESS"); task.setResponseDigest(sha256(resp));
            doc.setOcrResult(p.json); doc.setOcrConfidence(p.confidence);
            doc.setManualConfirmed(p.lowConf ? 0 : 1); doc.setOcrStatus("SUCCESS");
            ocrBreaker.onSuccess();
        } catch (Exception ex) {
            log.error("OCR失败 taskId={}", taskId, ex);
            task.setStatus("FAILED"); task.setErrorMsg(ex.getMessage());
            doc.setOcrStatus("FAILED"); ocrBreaker.onFailure();
        } finally {
            task.setEndTime(LocalDateTime.now());
            task.setCostMs((int)Duration.between(start,LocalDateTime.now()).toMillis());
            taskMapper.updateById(task); docMapper.updateById(doc);
        }
    }

    private OcrParsed parseOcr(String resp) {
        if (resp == null || resp.isBlank()) throw new BizException(ResultCode.AI_SERVICE_ERROR, "OCR返回为空");
        JSONObject obj;
        try { obj = JSONUtil.parseObj(resp); }
        catch (Exception e) { throw new BizException(ResultCode.AI_SERVICE_ERROR, "OCR响应非JSON"); }
        // 字段级校验规则需按所选API文档补充（以下为通用示例）
        if (!obj.containsKey("result")) throw new BizException(ResultCode.AI_SERVICE_ERROR, "OCR响应缺少result");
        BigDecimal conf = null;
        try { Object v = obj.get("confidence"); if (v instanceof Number) conf = BigDecimal.valueOf(((Number)v).doubleValue()); }
        catch (Exception ignored) {}
        boolean low = conf == null || conf.compareTo(new BigDecimal("0.85")) < 0;
        return new OcrParsed(obj.toStringPretty(), conf, low);
    }
    private static class OcrParsed { String json; BigDecimal confidence; boolean lowConf;
        OcrParsed(String j, BigDecimal c, boolean l) { json=j; confidence=c; lowConf=l; } }

    @Override public OcrTaskVO pollOcrTask(Long userId, Long taskId) {
        AiTask t = taskMapper.selectById(taskId);
        if (t == null || !"OCR".equals(t.getTaskType())) throw new BizException(ResultCode.NOT_FOUND);
        MedicalDocument d = docMapper.selectById(t.getBizId());
        if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        // 归属校验：仅该老人关联订单的陪诊师可轮询（与确认接口保持一致）
        checkEscortDocAccess(userId, d, false);
        OcrTaskVO vo = toOcrVO(t, d);
        if (d != null && d.getOcrConfidence() != null && d.getOcrConfidence().compareTo(new BigDecimal("0.85")) < 0)
            vo.setErrorMsg((vo.getErrorMsg()!=null?vo.getErrorMsg()+";":"")+"置信度<0.85需人工确认");
        return vo;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void confirmOcr(Long userId, Long taskId, OcrConfirmDTO dto) {
        AiTask t = taskMapper.selectById(taskId); if (t == null) throw new BizException(ResultCode.NOT_FOUND);
        MedicalDocument d = docMapper.selectById(t.getBizId()); if (d == null) throw new BizException(ResultCode.NOT_FOUND);
        checkEscortDocAccess(userId, d, true);
        if (dto.getCorrectedResult() != null) {
            Object corrected = dto.getCorrectedResult();
            d.setOcrResult(corrected instanceof String s ? s : JSONUtil.toJsonStr(corrected));
        }
        d.setManualConfirmed(1); d.setConfirmedBy(userId); d.setConfirmedTime(LocalDateTime.now());
        docMapper.updateById(d);
        // OCR确认后把诊断/检查结论、检验指标回写到健康档案
        writeBackToRecord(d);
    }

    /**
     * 将人工确认后的OCR结果映射到 medical_record。
     * 仅在字段为空时写入，已有内容时以“【OCR】”标记追加，保留人工录入痕迹。
     */
    private void writeBackToRecord(MedicalDocument d) {
        MedicalRecord r = recordMapper.selectById(d.getRecordId());
        if (r == null || d.getOcrResult() == null || d.getOcrResult().isBlank()) return;
        JSONObject obj;
        try {
            obj = JSONUtil.parseObj(d.getOcrResult());
        } catch (Exception e) {
            log.warn("OCR结果不是合法JSON，跳过档案回写 documentId={}", d.getId());
            return;
        }
        // Mock/第三方OCR常见外层包装 {"result":{...},"confidence":0.95}，优先取内层业务字段
        JSONObject nested = obj.getJSONObject("result");
        if (nested != null) obj = nested;
        boolean changed = false;
        String diagnosis = obj.getStr("diagnosis");
        if (diagnosis == null || diagnosis.isBlank()) diagnosis = obj.getStr("conclusion");
        if (diagnosis != null && !diagnosis.isBlank() && !contains(r.getDiagnosis(), diagnosis)) {
            r.setDiagnosis(truncate(appendWithMarker(r.getDiagnosis(),
                    String.format("【OCR】%s", truncate(diagnosis, 400))), 500));
            changed = true;
        }
        String prescription = obj.getStr("prescription");
        if (prescription != null && !prescription.isBlank() && !contains(r.getPrescription(), prescription)) {
            r.setPrescription(appendWithMarker(r.getPrescription(), String.format("【OCR】%s", truncate(prescription, 2000))));
            changed = true;
        }
        String department = obj.getStr("department");
        if ((r.getDepartment() == null || r.getDepartment().isBlank()) && department != null && !department.isBlank()) {
            r.setDepartment(truncate(department, 50));
            changed = true;
        }
        String date = obj.getStr("date");
        if (date != null && !date.isBlank()) {
            try {
                r.setVisitDate(java.time.LocalDate.parse(date));
                changed = true;
            } catch (Exception ignored) {
                // OCR日期格式不合法时保留原就诊日期
            }
        }
        JSONArray items = obj.getJSONArray("examItems");
        if (items != null && !items.isEmpty()) {
            StringBuilder summary = new StringBuilder("【OCR检验指标】");
            boolean firstItem = true;
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (item == null) continue;
                if (!firstItem) summary.append("；");
                firstItem = false;
                summary.append(item.getStr("name", "-")).append("=").append(item.getStr("value", "-"));
                String unit = item.getStr("unit");
                if (unit != null && !unit.isBlank()) summary.append(unit);
                if (Boolean.TRUE.equals(item.getBool("abnormal"))) summary.append("(异常)");
            }
            String text = truncate(summary.toString(), 400);
            if (!contains(r.getRemark(), text)) {
                r.setRemark(truncate(appendWithMarker(r.getRemark(), text), 500));
                changed = true;
            }
        }
        if (changed) {
            recordMapper.updateById(r);
        }
    }

    private String appendWithMarker(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + "\n" + addition;
    }

    private boolean contains(String existing, String text) {
        return existing != null && text != null && existing.contains(text);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    @Override public ContentVO generateNotice(Long userId, NoticeGenerateDTO dto) {
        EscortOrder o = orderMapper.selectById(dto.getOrderId());
        if (o == null || !o.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        ElderlyProfile e = elderlyMapper.selectById(o.getElderlyId());
        String prompt = String.format("生成诊前告知书：医院ID=%d,科室=%s,预约时间=%s,行动能力=%s,过敏史=%s",
                o.getHospitalId(), o.getDepartment(), o.getAppointmentTime(),
                e!=null&&e.getMobility()!=null?e.getMobility():"未填写",
                e!=null&&e.getAllergyHistory()!=null?e.getAllergyHistory():"无");
        return llmGen("NOTICE", dto.getOrderId(), prompt);
    }

    @Override public ContentVO generateExplain(Long userId, ExplainGenerateDTO dto) {
        MedicalRecord r = recordMapper.selectById(dto.getRecordId());
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
        if (e == null || !e.getFamilyId().equals(userId)) throw new BizException(ResultCode.FORBIDDEN);
        String prompt = String.format("通俗化解读医嘱：诊断=%s,用药建议=%s,备注=%s",
                r.getDiagnosis()!=null?r.getDiagnosis():"未填写",
                r.getPrescription()!=null?r.getPrescription():"未填写",
                r.getRemark()!=null?r.getRemark():"无");
        return llmGen("EXPLAIN", dto.getRecordId(), prompt);
    }

    private ContentVO llmGen(String type, Long bizId, String prompt) {
        // 熔断开启：跳过主模型，直接走本地Mock兜底
        if (!llmBreaker.allowRequest()) {
            log.warn("LLM熔断开启，使用本地Mock兜底 type={} bizId={}", type, bizId);
            ContentVO fb = fallbackGen(type, bizId, prompt);
            return fb != null ? fb : degraded(type, bizId);
        }
        AiTask t = new AiTask(); t.setTaskType(type); t.setBizId(bizId); t.setStatus("RUNNING");
        t.setRetryCount(0); t.setStartTime(LocalDateTime.now()); t.setRequestDigest("bizId="+bizId);
        taskMapper.insert(t);
        LocalDateTime st = LocalDateTime.now();
        try {
            // DeepSeek (OpenAI-compatible): POST /chat/completions
            String resp = retryCall(() -> callGenLlm(prompt, false), t);
            if (resp == null || resp.isBlank()) throw new BizException(ResultCode.AI_SERVICE_ERROR, "LLM返回空");
            if (resp.length() > 10000) resp = resp.substring(0,10000)+"...";
            t.setStatus("SUCCESS"); t.setResponseDigest(sha256(resp));
            t.setEndTime(LocalDateTime.now()); t.setCostMs((int)Duration.between(st,LocalDateTime.now()).toMillis());
            taskMapper.updateById(t);
            AiGeneratedContent c = new AiGeneratedContent();
            c.setTaskId(t.getId()); c.setContentType(type); c.setBizId(bizId);
            c.setContent(resp); c.setDisclaimer(DISCLAIMER); c.setReviewStatus("UNREVIEWED");
            contentMapper.insert(c); llmBreaker.onSuccess();
            return toContentVO(c);
        } catch (Exception ex) {
            log.error("LLM失败，准备切换本地Mock兜底 taskId={}", t.getId(), ex);
            t.setStatus("FAILED"); t.setErrorMsg(ex.getMessage());
            t.setEndTime(LocalDateTime.now()); t.setCostMs((int)Duration.between(st,LocalDateTime.now()).toMillis());
            taskMapper.updateById(t); llmBreaker.onFailure();
            ContentVO fb = fallbackGen(type, bizId, prompt);
            return fb != null ? fb : degraded(type, bizId);
        }
    }

    /**
     * 调用生成类LLM接口：主模型走OpenAI兼容协议，兜底走本地Mock的 /chat 模板。
     */
    private String callGenLlm(String prompt, boolean fallback) throws Exception {
        if (fallback) {
            JSONObject req = new JSONObject();
            req.set("prompt", prompt);
            return httpClient.llmFallbackPost("/chat", req.toString());
        }
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", GEN_SYSTEM_PROMPT));
        messages.add(new JSONObject().set("role", "user").set("content", prompt));
        JSONObject req = new JSONObject();
        req.set("model", aiProps.getLlm().getChatModel());
        req.set("messages", messages);
        String respJson = httpClient.llmPost("/chat/completions", req.toString());
        JSONObject respObj = JSONUtil.parseObj(respJson);
        JSONArray choices = respObj.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) throw new BizException(ResultCode.AI_SERVICE_ERROR, "LLM响应缺少choices");
        String content = choices.getJSONObject(0).getJSONObject("message").getStr("content");
        return stripHtmlFence(content);
    }

    /** 去掉模型可能包上的 ```html 代码块围栏 */
    private String stripHtmlFence(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t.trim();
    }

    /**
     * 主模型不可用时的本地Mock兜底（mock-ai-server 的 /chat 返回中文HTML模板）。
     * @return 兜底内容；未启用或调用失败返回 null
     */
    private ContentVO fallbackGen(String type, Long bizId, String prompt) {
        if (!httpClient.fallbackEnabled()) return null;
        AiTask t = new AiTask(); t.setTaskType(type); t.setBizId(bizId); t.setStatus("RUNNING");
        t.setRetryCount(0); t.setStartTime(LocalDateTime.now()); t.setRequestDigest("bizId="+bizId);
        taskMapper.insert(t);
        LocalDateTime st = LocalDateTime.now();
        try {
            JSONObject req = new JSONObject();
            req.set("prompt", prompt);
            String resp = httpClient.llmFallbackPost("/chat", req.toString());
            if (resp == null || resp.isBlank()) throw new BizException(ResultCode.AI_SERVICE_ERROR, "兜底返回空");
            if (resp.length() > 10000) resp = resp.substring(0,10000)+"...";
            t.setStatus("SUCCESS"); t.setResponseDigest(sha256(resp));
            t.setErrorMsg("主模型不可用，已使用本地Mock兜底");
            t.setEndTime(LocalDateTime.now()); t.setCostMs((int)Duration.between(st,LocalDateTime.now()).toMillis());
            taskMapper.updateById(t);
            AiGeneratedContent c = new AiGeneratedContent();
            c.setTaskId(t.getId()); c.setContentType(type); c.setBizId(bizId);
            c.setContent(resp); c.setDisclaimer(DISCLAIMER); c.setReviewStatus("UNREVIEWED");
            contentMapper.insert(c);
            log.warn("已使用本地Mock兜底 type={} bizId={}", type, bizId);
            return toContentVO(c);
        } catch (Exception ex) {
            log.error("本地Mock兜底失败 type={} bizId={}", type, bizId, ex);
            t.setStatus("FAILED"); t.setErrorMsg(ex.getMessage());
            t.setEndTime(LocalDateTime.now()); t.setCostMs((int)Duration.between(st,LocalDateTime.now()).toMillis());
            taskMapper.updateById(t);
            return null;
        }
    }

    private String retryCall(java.util.concurrent.Callable<String> c, AiTask t) throws Exception {
        long bo = aiProps.getRetryBackoffMs();
        int maxRetry = aiProps.getMaxRetry();
        for (int i = 0; i <= maxRetry; i++) {
            try { t.setRetryCount(i); taskMapper.updateById(t); return c.call(); }
            catch (Exception ex) {
                boolean retry = ex instanceof SocketTimeoutException;
                if (ex instanceof AiApiException) { int sc = ((AiApiException)ex).getStatusCode(); retry = sc >= 500 || sc == 429; }
                if (!retry || i == 3) throw ex;
                Thread.sleep(bo * (1L << i));
            }
        } throw new RuntimeException("重试耗尽");
    }

    @Override public ContentVO getContent(Long id) {
        AiGeneratedContent c = contentMapper.selectById(id);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND);
        checkContentOwner(UserContext.getUserId(), UserContext.getUserRole(), c);
        return toContentVO(c);
    }

    /**
     * AI 内容归属校验：
     * - ADMIN 可查看全部（人工复核场景）
     * - FAMILY 按订单/档案归属校验
     * - ESCORT 仅可查看已指派给自己的订单/老人相关内容，禁止直接放行
     */
    private void checkContentOwner(Long uid, String role, AiGeneratedContent c) {
        if ("ADMIN".equals(role)) return;
        if ("NOTICE".equals(c.getContentType())) {
            EscortOrder o = orderMapper.selectById(c.getBizId());
            if (o == null) throw new BizException(ResultCode.NOT_FOUND);
            boolean allowed = "FAMILY".equals(role)
                    ? o.getFamilyId().equals(uid)
                    : "ESCORT".equals(role) && uid.equals(o.getEscortId());
            if (!allowed) throw new BizException(ResultCode.FORBIDDEN);
        } else if ("EXPLAIN".equals(c.getContentType())) {
            MedicalRecord r = recordMapper.selectById(c.getBizId());
            if (r == null) throw new BizException(ResultCode.NOT_FOUND);
            if ("FAMILY".equals(role)) {
                ElderlyProfile e = elderlyMapper.selectById(r.getElderlyId());
                if (e == null || !e.getFamilyId().equals(uid)) throw new BizException(ResultCode.FORBIDDEN);
            } else if ("ESCORT".equals(role)) {
                Long count = orderMapper.selectCount(new LambdaQueryWrapper<EscortOrder>()
                        .eq(EscortOrder::getEscortId, uid)
                        .eq(EscortOrder::getElderlyId, r.getElderlyId()));
                if (count == 0) throw new BizException(ResultCode.FORBIDDEN);
            } else {
                throw new BizException(ResultCode.FORBIDDEN);
            }
        } else {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void reviewContent(Long uid, Long cid, ContentReviewDTO dto) {
        if (!"APPROVED".equals(dto.getReviewStatus()) && !"REJECTED".equals(dto.getReviewStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "复核状态仅支持APPROVED或REJECTED");
        }
        AiGeneratedContent c = contentMapper.selectById(cid);
        if (c == null) throw new BizException(ResultCode.NOT_FOUND);
        c.setReviewStatus(dto.getReviewStatus()); c.setReviewRemark(dto.getReviewRemark());
        c.setReviewerId(uid); c.setReviewTime(LocalDateTime.now());
        contentMapper.updateById(c);
    }

    private void checkEscortDocAccess(Long uid, MedicalDocument doc, boolean inService) {
        MedicalRecord r = recordMapper.selectById(doc.getRecordId());
        if (r == null) throw new BizException(ResultCode.NOT_FOUND);
        LambdaQueryWrapper<EscortOrder> w = new LambdaQueryWrapper<EscortOrder>()
                .eq(EscortOrder::getEscortId, uid).eq(EscortOrder::getElderlyId, r.getElderlyId());
        if (inService) w.eq(EscortOrder::getStatus, "IN_SERVICE");
        if (orderMapper.selectCount(w) == 0) throw new BizException(ResultCode.FORBIDDEN);
    }

    private void degrade(AiTask t, MedicalDocument d, String msg) {
        t.setStatus("DEGRADED"); t.setEndTime(LocalDateTime.now()); t.setErrorMsg(msg);
        taskMapper.updateById(t); d.setOcrStatus("DEGRADED"); docMapper.updateById(d);
    }

    private ContentVO degraded(String type, Long bizId) {
        // 先落ai_task记录（保证ai_generated_content.task_id非空约束）
        AiTask t = new AiTask();
        t.setTaskType(type); t.setBizId(bizId); t.setStatus("DEGRADED");
        t.setRetryCount(0); t.setStartTime(LocalDateTime.now());
        t.setEndTime(LocalDateTime.now()); t.setCostMs(0);
        t.setErrorMsg("熔断降级或LLM服务不可用");
        taskMapper.insert(t);
        // 降级内容持久化，设置真实task_id
        AiGeneratedContent c = new AiGeneratedContent();
        c.setTaskId(t.getId()); c.setContentType(type); c.setBizId(bizId);
        c.setContent("【系统提示】AI服务暂时不可用。AI生成内容仅供参考，不构成任何诊疗建议。");
        c.setDisclaimer(DISCLAIMER); c.setReviewStatus("UNREVIEWED");
        contentMapper.insert(c);
        return toContentVO(c);
    }

    private OcrTaskVO toOcrVO(AiTask t, MedicalDocument d) {
        OcrTaskVO v = new OcrTaskVO();
        v.setTaskId(t.getId()); v.setStatus(t.getStatus()); v.setErrorMsg(t.getErrorMsg());
        v.setCreateTime(t.getCreateTime());
        if (d != null) {
            v.setOcrResult(d.getOcrResult());
            v.setOcrConfidence(d.getOcrConfidence());
            v.setManualConfirmed(d.getManualConfirmed());
            v.setImageUrl(d.getImageUrl());
        }
        return v;
    }

    private ContentVO toContentVO(AiGeneratedContent c) {
        ContentVO v = new ContentVO();
        v.setId(c.getId()); v.setContentType(c.getContentType()); v.setContent(c.getContent());
        v.setDisclaimer(c.getDisclaimer()); v.setReviewStatus(c.getReviewStatus());
        v.setReviewRemark(c.getReviewRemark()); v.setCreateTime(c.getCreateTime());
        return v;
    }

    private String sha256(String s) { return DigestUtil.sha256Hex(s).substring(0,16)+"..."; }
}