package com.example.escort.common;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.escort.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;

@Slf4j @Component
public class AiHttpClient {
    private final OkHttpClient ocrClient;
    private final OkHttpClient llmClient;
    private final AiProperties props;

    public AiHttpClient(AiProperties props) {
        this.props = props;
        this.ocrClient = build(props.getConnectTimeoutMs(), props.getOcr().getReadTimeoutMs());
        this.llmClient = build(props.getConnectTimeoutMs(), props.getLlm().getReadTimeoutMs());
    }
    private OkHttpClient build(int c, int r) {
        return new OkHttpClient.Builder().connectTimeout(Duration.ofMillis(c)).readTimeout(Duration.ofMillis(r)).build();
    }
    public String ocrPost(String path, String json) throws IOException {
        return doPost(ocrClient, props.getOcr().getBaseUrl() + path, json, props.getOcr().getApiKey());
    }
    public String llmPost(String path, String json) throws IOException {
        return doPost(llmClient, props.getLlm().getBaseUrl() + path, json, props.getLlm().getApiKey());
    }

    /**
     * 调用兜底LLM（本地Mock）。未配置或与主地址相同时视为未启用。
     */
    public String llmFallbackPost(String path, String json) throws IOException {
        String base = props.getLlm().getFallbackBaseUrl();
        if (!fallbackEnabled()) throw new IOException("未配置LLM兜底地址");
        return doPost(llmClient, base + path, json, props.getLlm().getFallbackApiKey());
    }

    /** 兜底是否可用：已配置且不等于主地址（避免 mock 模式下自己兜自己） */
    public boolean fallbackEnabled() {
        String base = props.getLlm().getFallbackBaseUrl();
        return base != null && !base.isBlank()
                && !base.equals(props.getLlm().getBaseUrl());
    }

    /** 流式增量回调（reasoning/content 任一可能为 null） */
    @FunctionalInterface
    public interface StreamDeltaHandler {
        void onDelta(String reasoning, String content) throws IOException;
    }

    /**
     * 流式调用主LLM（OpenAI兼容SSE）。逐行解析 data: 事件并回调增量。
     * 仅用于主模型；兜底Mock走非流式 llmFallbackPost。
     */
    public void llmStreamPost(String path, String json, StreamDeltaHandler handler) throws IOException {
        RequestBody b = RequestBody.create(json, MediaType.parse("application/json"));
        Request req = new Request.Builder()
                .url(props.getLlm().getBaseUrl() + path)
                .post(b)
                .header("Authorization", "Bearer " + props.getLlm().getApiKey())
                .header("Accept", "text/event-stream")
                .build();
        try (Response r = llmClient.newCall(req).execute()) {
            if (!r.isSuccessful()) throw new AiApiException(r.code(), "HTTP " + r.code());
            if (r.body() == null) throw new IOException("流式响应体为空");
            BufferedSource source = r.body().source();
            String line;
            while ((line = source.readUtf8Line()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty()) continue;
                if ("[DONE]".equals(data)) break;
                JSONObject obj;
                try { obj = JSONUtil.parseObj(data); } catch (Exception ignored) { continue; }
                JSONArray choices = obj.getJSONArray("choices");
                if (choices == null || choices.isEmpty()) continue;
                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                if (delta == null) continue;
                handler.onDelta(delta.getStr("reasoning_content"), delta.getStr("content"));
            }
        }
    }
    private String doPost(OkHttpClient cl, String url, String json, String key) throws IOException {
        RequestBody b = RequestBody.create(json, MediaType.parse("application/json"));
        Request req = new Request.Builder().url(url).post(b).header("Authorization", "Bearer " + key).build();
        try (Response r = cl.newCall(req).execute()) {
            if (!r.isSuccessful()) throw new AiApiException(r.code(), "HTTP " + r.code());
            return r.body() != null ? r.body().string() : "";
        }
    }
    @PreDestroy public void destroy() {
        ocrClient.dispatcher().executorService().shutdown();
        llmClient.dispatcher().executorService().shutdown();
    }
}