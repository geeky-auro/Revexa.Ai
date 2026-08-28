package ai.revexa.intelligence.llm;

public record LlmResult(
        String text, String provider, String model, int inputTokens, int outputTokens, long latencyMs) {

    public static LlmResult of(String text, String provider, String model, long latencyMs) {
        return new LlmResult(text, provider, model, 0, 0, latencyMs);
    }
}
