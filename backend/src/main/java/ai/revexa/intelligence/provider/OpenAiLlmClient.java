package ai.revexa.intelligence.provider;

import ai.revexa.core.config.RevexaProperties;
import ai.revexa.intelligence.llm.LlmClient;
import ai.revexa.intelligence.llm.LlmException;
import ai.revexa.intelligence.llm.LlmMessage;
import ai.revexa.intelligence.llm.LlmRequest;
import ai.revexa.intelligence.llm.LlmResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** OpenAI-compatible chat completions — also works against Azure OpenAI, Ollama or vLLM gateways. */
@Component
public class OpenAiLlmClient implements LlmClient {

    public static final String ID = "openai";

    private final RevexaProperties.OpenAi config;
    private final RevexaProperties.Ai aiConfig;
    private final RestClient client;

    public OpenAiLlmClient(RevexaProperties properties) {
        this.config = properties.getAi().getOpenai();
        this.aiConfig = properties.getAi();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) properties.getAi().getTimeout().toMillis());
        this.client = RestClient.builder().baseUrl(config.getBaseUrl()).requestFactory(factory).build();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean available() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public String model() {
        return config.getModel();
    }

    @Override
    public LlmResult complete(LlmRequest request) {
        if (!available()) {
            throw new LlmException("OpenAI API key is not configured");
        }
        long started = System.nanoTime();

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", request.system()));
        for (LlmMessage message : request.messages()) {
            messages.add(Map.of("role", message.role(), "content", message.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("messages", messages);
        body.put("temperature", request.temperature() == null ? aiConfig.getTemperature() : request.temperature());
        body.put("max_tokens", request.maxTokens() == null ? aiConfig.getMaxOutputTokens() : request.maxTokens());
        if (request.jsonMode()) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        try {
            JsonNode response =
                    client.post()
                            .uri("/v1/chat/completions")
                            .header("Authorization", "Bearer " + config.getApiKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);

            if (response == null) {
                throw new LlmException("OpenAI returned an empty response");
            }
            String text = response.path("choices").path(0).path("message").path("content").asText("");
            long latency = (System.nanoTime() - started) / 1_000_000;
            return new LlmResult(
                    text,
                    ID,
                    response.path("model").asText(config.getModel()),
                    response.path("usage").path("prompt_tokens").asInt(0),
                    response.path("usage").path("completion_tokens").asInt(0),
                    latency);
        } catch (LlmException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LlmException("OpenAI request failed: " + e.getMessage(), e);
        }
    }
}
