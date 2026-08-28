package ai.revexa.intelligence.prompt;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Loads the prompt templates from {@code resources/prompts} and renders {@code {{placeholders}}}.
 *
 * <p>Prompts live outside the code so they can be reviewed, diffed and tuned without a rebuild —
 * prompt changes are product changes, and they deserve to show up in a pull request as such.
 */
@Component
public class PromptLibrary {

    private final Map<String, String> templates = new ConcurrentHashMap<>();
    private String system = "";

    @PostConstruct
    void load() {
        system = read("prompts/system.md");
    }

    public String system() {
        return system;
    }

    public String render(String stageId, Map<String, String> variables) {
        String template = templates.computeIfAbsent(stageId, id -> read("prompts/" + id + ".md"));
        String rendered = template;
        Map<String, String> safe = new HashMap<>(variables);
        for (Map.Entry<String, String> entry : safe.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        // Any placeholder the caller did not supply becomes an explicit "not provided" marker rather
        // than leaking template syntax into the model's context.
        return rendered.replaceAll("\\{\\{[a-zA-Z]+}}", "(not provided)");
    }

    private String read(String path) {
        try {
            return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing prompt template: " + path, e);
        }
    }
}
