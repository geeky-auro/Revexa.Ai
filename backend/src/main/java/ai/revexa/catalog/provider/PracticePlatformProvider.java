package ai.revexa.catalog.provider;

import ai.revexa.catalog.domain.Difficulty;
import ai.revexa.catalog.domain.PlatformSource;
import java.util.List;

/**
 * The seam between Revexa and the outside practice platforms.
 *
 * <p>Deliberate design constraint: no implementation here scrapes a site or calls a private API.
 * Problems arrive by manual paste, by a URL the user supplies, or through a user-authorised browser
 * extension that posts a structured payload. When a platform later offers an official API, it becomes
 * one more implementation of this interface and nothing above it changes.
 */
public interface PracticePlatformProvider {

    String id();

    String displayName();

    PlatformSource source();

    Capabilities capabilities();

    /** Whether this provider can make sense of the supplied payload. */
    boolean supports(ImportRequest request);

    ImportedProblem importProblem(ImportRequest request);

    /**
     * What this provider can and cannot do, surfaced in the UI so the limits are honest rather than
     * discovered by a user hitting a wall.
     */
    record Capabilities(
            boolean automaticImport,
            boolean requiresUserAuthorization,
            boolean submissionSync,
            List<String> acceptedInputs,
            String notes) {}

    /** Free-form import payload; each provider reads the parts it understands. */
    record ImportRequest(String rawText, String url, String title, String difficulty, String structuredPayload) {

        public String safeText() {
            return rawText == null ? "" : rawText;
        }

        public String safeUrl() {
            return url == null ? "" : url;
        }
    }

    record ImportedProblem(
            String title,
            String slug,
            String statement,
            String constraintsText,
            String examples,
            Difficulty difficulty,
            List<String> topics,
            PlatformSource source,
            String externalId,
            String url,
            List<String> warnings) {}
}
