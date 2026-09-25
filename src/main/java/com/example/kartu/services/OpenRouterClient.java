package com.example.kartu.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

/**
 * Thin client for OpenRouter's OpenAI-compatible chat completions API (SKPL-F20 usulan),
 * same approach as the SnapMart reference: plain HTTP, no SDK. Uses free models; the
 * fallback list is sent as OpenRouter's "models" parameter so a busy or removed free
 * model does not break the chat.
 */
@Service
@Slf4j
public class OpenRouterClient {

    private static final int MAX_TOKENS = 2048;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final List<String> fallbackModels;

    public OpenRouterClient(@Value("${openrouter.api-key:}") String apiKey,
                            @Value("${openrouter.model:}") String model,
                            @Value("${openrouter.fallback-models:}") String fallbackModels,
                            @Value("${app.base-url:http://localhost:8080}") String appUrl) {
        this.apiKey = apiKey.trim();
        this.model = model.trim();
        this.fallbackModels = Arrays.stream(fallbackModels.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();

        // Free models can take a while to answer.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(90_000);
        this.restClient = RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .requestFactory(factory)
                .defaultHeader("HTTP-Referer", appUrl)
                .defaultHeader("X-Title", "Zelatan Cell")
                .build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty() && !model.isEmpty();
    }

    /**
     * @param messages OpenAI-style messages: role system/user/assistant + content
     * @return the assistant text, never empty
     * @throws AiException with a user-facing Indonesian message
     */
    public String complete(List<Map<String, String>> messages, double temperature) {
        if (!isConfigured()) {
            throw new AiException(AiException.Kind.NOT_CONFIGURED);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        if (!fallbackModels.isEmpty()) {
            List<String> models = new ArrayList<>();
            models.add(model);
            models.addAll(fallbackModels);
            body.put("models", models);
        }
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", MAX_TOKENS);

        JsonNode json;
        try {
            json = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw fromStatus(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.warn("OpenRouter tidak bisa dihubungi: {}", e.getMessage());
            throw new AiException(AiException.Kind.UNAVAILABLE);
        }

        if (json == null) {
            throw new AiException(AiException.Kind.UNAVAILABLE);
        }
        // OpenRouter can report upstream errors inside a 200 body.
        if (json.hasNonNull("error")) {
            int code = json.path("error").path("code").asInt(500);
            throw fromStatus(HttpStatusCode.valueOf(code >= 400 && code < 600 ? code : 500), json.path("error").toString());
        }
        String content = json.path("choices").path(0).path("message").path("content").asText("");
        // Some reasoning models inline their thinking; it is not part of the answer.
        content = content.replaceAll("(?s)<think>.*?</think>", "").trim();
        if (content.isEmpty()) {
            log.warn("OpenRouter membalas tanpa isi (model={}, finish_reason={})",
                    json.path("model").asText(), json.path("choices").path(0).path("finish_reason").asText());
            throw new AiException(AiException.Kind.UNAVAILABLE);
        }
        log.info("OpenRouter menjawab dengan model {}", json.path("model").asText(model));
        return content;
    }

    /** Label, usage and limit of the API key (GET /key). Never returns the key itself. */
    public Map<String, Object> usage() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", isConfigured());
        result.put("model", model);
        result.put("fallbackModels", fallbackModels);
        if (!isConfigured()) {
            return result;
        }
        try {
            JsonNode data = restClient.get().uri("/key")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve().body(JsonNode.class)
                    .path("data");
            result.put("label", data.path("label").asText("-"));
            result.put("usage", data.path("usage").asDouble(0));
            result.put("limit", data.path("limit").isNumber() ? data.path("limit").asDouble() : null);
            result.put("isFreeTier", data.path("is_free_tier").asBoolean(true));
        } catch (RestClientException e) {
            result.put("error", "Info kuota tidak bisa diambil dari OpenRouter.");
        }
        return result;
    }

    private AiException fromStatus(HttpStatusCode status, String detail) {
        log.warn("OpenRouter menolak permintaan: HTTP {} {}", status.value(), detail);
        return switch (status.value()) {
            case 401, 403 -> new AiException(AiException.Kind.NOT_CONFIGURED);
            case 402, 429 -> new AiException(AiException.Kind.QUOTA);
            default -> new AiException(AiException.Kind.UNAVAILABLE);
        };
    }

    /** Failure talking to the model; {@link #getMessage()} is safe to show to the admin. */
    public static class AiException extends RuntimeException {
        public enum Kind { NOT_CONFIGURED, QUOTA, UNAVAILABLE }

        private final Kind kind;

        public AiException(Kind kind) {
            super(switch (kind) {
                case NOT_CONFIGURED -> "AI belum dikonfigurasi atau API key OpenRouter tidak valid. "
                        + "Isi OPENROUTER_API_KEY dan OPENROUTER_MODEL di file .env backend, lalu jalankan ulang aplikasi.";
                case QUOTA -> "Kuota AI gratis sedang habis atau permintaan terlalu sering. "
                        + "Coba lagi beberapa saat lagi; batas harian model gratis direset setiap hari.";
                case UNAVAILABLE -> "Layanan AI sedang tidak bisa dihubungi. Coba ulangi sebentar lagi.";
            });
            this.kind = kind;
        }

        public Kind getKind() {
            return kind;
        }
    }
}
