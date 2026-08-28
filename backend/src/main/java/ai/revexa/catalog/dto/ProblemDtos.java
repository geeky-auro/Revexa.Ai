package ai.revexa.catalog.dto;

import ai.revexa.catalog.domain.Difficulty;
import ai.revexa.catalog.domain.PlatformSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Wire shapes for the problem catalogue. */
public final class ProblemDtos {

    private ProblemDtos() {}

    public record ProblemSummary(
            UUID id,
            String title,
            String slug,
            Difficulty difficulty,
            PlatformSource source,
            List<String> topics,
            String url,
            boolean curated,
            Instant createdAt) {}

    public record ProblemDetail(
            UUID id,
            String title,
            String slug,
            Difficulty difficulty,
            PlatformSource source,
            String statement,
            String constraintsText,
            List<Example> examples,
            List<String> topics,
            String url,
            boolean curated,
            Instant createdAt) {}

    public record Example(String input, String output, String explanation) {}

    public record CreateProblemRequest(
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 40_000) String statement,
            @Size(max = 8_000) String constraintsText,
            String difficulty,
            List<String> topics,
            @Size(max = 500) String url) {}

    /**
     * A platform-agnostic import. {@code providerId} is optional — leave it out and the registry picks
     * the most specific provider that can read the payload.
     */
    public record ImportProblemRequest(
            String providerId,
            @Size(max = 40_000) String rawText,
            @Size(max = 500) String url,
            @Size(max = 300) String title,
            String difficulty,
            @Size(max = 60_000) String structuredPayload) {}

    public record ImportResult(ProblemDetail problem, String providerId, String providerName, List<String> warnings) {}

    public record PlatformProviderInfo(
            String id,
            String displayName,
            PlatformSource source,
            boolean automaticImport,
            boolean requiresUserAuthorization,
            boolean submissionSync,
            List<String> acceptedInputs,
            String notes) {}
}
