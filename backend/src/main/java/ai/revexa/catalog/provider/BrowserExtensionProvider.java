package ai.revexa.catalog.provider;

import ai.revexa.catalog.domain.Difficulty;
import ai.revexa.catalog.domain.PlatformSource;
import ai.revexa.core.json.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Receives a structured capture from the Revexa browser extension.
 *
 * <p>The extension runs in the user's own authenticated session, on a page they have already opened,
 * and posts a normalised payload here. That keeps the integration inside what the user is entitled to
 * see, with no server-side scraping and no credential sharing. The payload shape is the contract:
 * {@code {title, url, difficulty, statement, constraints, topics[], examples[], platform}}.
 */
@Component
public class BrowserExtensionProvider implements PracticePlatformProvider {

    private final Json json;
    private final ProblemTextParser parser;

    public BrowserExtensionProvider(Json json, ProblemTextParser parser) {
        this.json = json;
        this.parser = parser;
    }

    @Override
    public String id() {
        return "browser-extension";
    }

    @Override
    public String displayName() {
        return "Revexa browser extension";
    }

    @Override
    public PlatformSource source() {
        return PlatformSource.CUSTOM;
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities(
                true,
                true,
                false,
                List.of("Structured JSON captured from the page the user has open"),
                "Runs in the user's own browser session on a page they already have open, so nothing is scraped "
                        + "server-side and no credentials are shared. Ships alongside the web app.");
    }

    @Override
    public boolean supports(ImportRequest request) {
        return request.structuredPayload() != null && !request.structuredPayload().isBlank();
    }

    @Override
    public ImportedProblem importProblem(ImportRequest request) {
        JsonNode payload =
                json.readTree(request.structuredPayload())
                        .orElseThrow(() -> new IllegalArgumentException("The extension payload was not valid JSON"));

        String title = text(payload, "title", "Untitled problem");
        String statement = text(payload, "statement", "");
        String constraints = text(payload, "constraints", "");
        String url = text(payload, "url", request.safeUrl());

        List<String> topics = new ArrayList<>();
        payload.path("topics").forEach(node -> topics.add(node.asText()));
        if (topics.isEmpty()) {
            topics.addAll(parser.inferTopics(statement));
        }

        PlatformSource source = platform(text(payload, "platform", "CUSTOM"));
        Difficulty difficulty = difficulty(text(payload, "difficulty", ""));

        String examples =
                payload.has("examples")
                        ? payload.path("examples").toString()
                        : json.write(parser.extractExamples(statement));

        List<String> warnings = new ArrayList<>();
        if (statement.isBlank()) {
            warnings.add("The capture contained no statement text — the extension may need to be updated for this page.");
        }

        return new ImportedProblem(
                title,
                parser.slugify(title),
                statement,
                constraints,
                examples,
                difficulty,
                topics,
                source,
                text(payload, "externalId", null),
                url,
                warnings);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText(fallback);
    }

    private PlatformSource platform(String raw) {
        try {
            return PlatformSource.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PlatformSource.CUSTOM;
        }
    }

    private Difficulty difficulty(String raw) {
        try {
            return Difficulty.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Difficulty.UNKNOWN;
        }
    }
}
