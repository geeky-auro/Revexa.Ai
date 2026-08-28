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

/** Claude via the Anthropic Messages API. */
@Component
public class AnthropicLlmClient implements LlmClient {

    public static final String ID = "anthropic";

    private final RevexaProperties.Anthropic config;
    private final RevexaProperties.Ai aiConfig;
    private final RestClient client;

    public AnthropicLlmClient(RevexaProperties properties) {
        this.config = properties.getAi().getAnthropic();
        this.aiConfig = properties.getAi();
        this.client =
                RestClient.builder()
                        .baseUrl(config.getBaseUrl())
                        .requestFactory(requestFactory(properties.getAi().getTimeout()))
                        .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return factory;
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
            throw new LlmException("Anthropic API key is not configured");
        }
        long started = System.nanoTime();

        List<Map<String, Object>> messages = new ArrayList<>();
        for (LlmMessage message : request.messages()) {
            messages.add(Map.of("role", message.role(), "content", message.content()));
        }
        if (request.jsonMode()) {
            // Prefilling the assistant turn with "{" is the reliable way to force a bare JSON object.
            messages.add(Map.of("role", "assistant", "content", "{"));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("max_tokens", request.maxTokens() == null ? aiConfig.getMaxOutputTokens() : request.maxTokens());
        body.put("temperature", request.temperature() == null ? aiConfig.getTemperature() : request.temperature());
        body.put("system", request.system());
        body.put("messages", messages);

        try {
            JsonNode response =
                    client.post()
                            .uri("/v1/messages")
                            .header("x-api-key", config.getApiKey())
                            .header("anthropic-version", config.getVersion())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);

            if (response == null) {
                throw new LlmException("Anthropic returned an empty response");
            }
            StringBuilder text = new StringBuilder(request.jsonMode() ? "{" : "");
            for (JsonNode block : response.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }
            long latency = (System.nanoTime() - started) / 1_000_000;
            return new LlmResult(
                    text.toString(),
                    ID,
                    response.path("model").asText(config.getModel()),
                    response.path("usage").path("input_tokens").asInt(0),
                    response.path("usage").path("output_tokens").asInt(0),
                    latency);
        } catch (LlmException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new LlmException("Anthropic request failed: " + e.getMessage(), e);
        }
    }
}
