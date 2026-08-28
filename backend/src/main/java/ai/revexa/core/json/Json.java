package ai.revexa.core.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JSON helpers shared across slices, plus the tolerant parser used on LLM output.
 *
 * <p>Models are asked for pure JSON but occasionally wrap it in prose or a fenced block. Rather
 * than failing the whole review, {@link #extractJson(String)} recovers the first balanced JSON
 * object or array it can find.
 */
@Component
public class Json {

    private static final Logger log = LoggerFactory.getLogger(Json.class);

    private final ObjectMapper mapper;

    public Json(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialise " + value.getClass(), e);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialise into " + type.getSimpleName(), e);
        }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to deserialise payload", e);
        }
    }

    public <T> Optional<T> readStructured(String raw, Class<T> type) {
        return extractJson(raw)
                .flatMap(
                        json -> {
                            try {
                                return Optional.of(mapper.readValue(json, type));
                            } catch (Exception e) {
                                log.warn("LLM payload did not match {}: {}", type.getSimpleName(), e.getMessage());
                                return Optional.empty();
                            }
                        });
    }

    public Optional<JsonNode> readTree(String raw) {
        return extractJson(raw)
                .flatMap(
                        json -> {
                            try {
                                return Optional.of(mapper.readTree(json));
                            } catch (Exception e) {
                                return Optional.empty();
                            }
                        });
    }

    /**
     * Pulls the first balanced JSON object/array out of arbitrary model text, ignoring braces that
     * appear inside string literals (very common when the payload embeds code snippets).
     */
    public static Optional<String> extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String text = stripFences(raw.trim());
        int start = -1;
        char open = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                open = c;
                break;
            }
        }
        if (start < 0) {
            return Optional.empty();
        }
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return Optional.of(text.substring(start, i + 1));
                }
            }
        }
        return Optional.empty();
    }

    private static String stripFences(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }
        String body = text.substring(firstNewline + 1);
        int fence = body.lastIndexOf("```");
        return fence >= 0 ? body.substring(0, fence) : body;
    }
}
