package ai.revexa.intelligence.llm;

/**
 * The seam between Revexa and any model vendor.
 *
 * <p>Everything above this interface — the pipeline, the hint ladder, the API — is written against
 * {@code LlmClient} only, so switching vendors is a configuration change ({@code revexa.ai.provider})
 * rather than a code change.
 */
public interface LlmClient {

    /** Stable id used in configuration and echoed back to clients for observability. */
    String id();

    /** Whether this client is usable right now (credentials present, endpoint configured). */
    boolean available();

    /** Model identifier this client will use, for display and audit. */
    String model();

    LlmResult complete(LlmRequest request);
}
