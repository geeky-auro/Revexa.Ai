package ai.revexa.intelligence.llm;

import ai.revexa.core.config.RevexaProperties;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Chooses the active {@link LlmClient} and guarantees the pipeline always gets an answer.
 *
 * <p>If the configured provider is unavailable or errors, the request is retried against the offline
 * heuristic engine (when {@code revexa.ai.fallback-to-heuristic} is on). A degraded, deterministic
 * review beats a spinner that never resolves.
 */
@Service
public class LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(LlmGateway.class);

    private final Map<String, LlmClient> clients;
    private final LlmClient heuristic;
    private final RevexaProperties.Ai config;

    public LlmGateway(List<LlmClient> clients, RevexaProperties properties) {
        this.clients = clients.stream().collect(java.util.stream.Collectors.toMap(LlmClient::id, c -> c));
        this.config = properties.getAi();
        this.heuristic = this.clients.get("heuristic");
        if (heuristic == null) {
            throw new IllegalStateException("The heuristic LLM client must always be registered");
        }
    }

    public LlmClient active() {
        LlmClient configured = clients.get(config.getProvider());
        if (configured != null && configured.available()) {
            return configured;
        }
        if (configured != null) {
            log.warn(
                    "AI provider '{}' is configured but not available (missing credentials?) — using '{}'",
                    config.getProvider(),
                    heuristic.id());
        }
        return heuristic;
    }

    public List<ProviderStatus> providers() {
        return clients.values().stream()
                .map(c -> new ProviderStatus(c.id(), c.model(), c.available(), c.id().equals(active().id())))
                .sorted(java.util.Comparator.comparing(ProviderStatus::id))
                .toList();
    }

    /**
     * Runs a stage and maps the raw completion with {@code parser}. Any failure — transport, parse or
     * schema — degrades to the heuristic engine so the caller never has to handle a null.
     */
    public <T> Staged<T> run(LlmRequest request, Function<LlmResult, T> parser) {
        LlmClient client = active();
        if (!"heuristic".equals(client.id())) {
            try {
                LlmResult result = client.complete(request);
                T parsed = parser.apply(result);
                if (parsed != null) {
                    return new Staged<>(parsed, result.provider(), result.model(), false);
                }
                log.warn("Provider '{}' returned unusable output for stage '{}'", client.id(), request.stageId());
            } catch (RuntimeException e) {
                log.warn("Provider '{}' failed on stage '{}': {}", client.id(), request.stageId(), e.getMessage());
            }
            if (!config.isFallbackToHeuristic()) {
                throw new LlmException("AI provider '" + client.id() + "' failed and fallback is disabled");
            }
        }
        LlmResult result = heuristic.complete(request);
        T parsed = parser.apply(result);
        if (parsed == null) {
            throw new LlmException("Heuristic engine produced no output for stage " + request.stageId());
        }
        return new Staged<>(parsed, result.provider(), result.model(), !"heuristic".equals(client.id()));
    }

    /** A stage result plus the provenance the UI shows in the "analysed by" footer. */
    public record Staged<T>(T value, String provider, String model, boolean degraded) {}

    public record ProviderStatus(String id, String model, boolean available, boolean active) {}
}
