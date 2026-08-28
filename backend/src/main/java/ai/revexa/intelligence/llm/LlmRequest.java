package ai.revexa.intelligence.llm;

import java.util.List;
import java.util.Map;

/**
 * A single provider-neutral completion request.
 *
 * <p>{@code prompt} carries the rendered text a hosted model sees. {@code context} carries the same
 * information in structured form; the offline heuristic provider reads that instead of re-parsing
 * prose, which is what lets Revexa run end-to-end with no API key configured.
 */
public record LlmRequest(
        String stageId,
        String system,
        List<LlmMessage> messages,
        Map<String, Object> context,
        Double temperature,
        Integer maxTokens,
        boolean jsonMode) {

    public static Builder forStage(String stageId) {
        return new Builder(stageId);
    }

    public static final class Builder {
        private final String stageId;
        private String system = "";
        private List<LlmMessage> messages = List.of();
        private Map<String, Object> context = Map.of();
        private Double temperature;
        private Integer maxTokens;
        private boolean jsonMode = true;

        private Builder(String stageId) {
            this.stageId = stageId;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder messages(List<LlmMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder userMessage(String content) {
            this.messages = List.of(LlmMessage.user(content));
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = context;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder jsonMode(boolean jsonMode) {
            this.jsonMode = jsonMode;
            return this;
        }

        public LlmRequest build() {
            return new LlmRequest(stageId, system, messages, context, temperature, maxTokens, jsonMode);
        }
    }
}
