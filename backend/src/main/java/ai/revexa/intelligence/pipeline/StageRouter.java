package ai.revexa.intelligence.pipeline;

import ai.revexa.core.json.Json;
import ai.revexa.intelligence.llm.LlmGateway;
import ai.revexa.intelligence.llm.LlmMessage;
import ai.revexa.intelligence.llm.LlmRequest;
import ai.revexa.intelligence.prompt.PromptLibrary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * The single place a pipeline stage is turned into a provider call.
 *
 * <p>Each stage names a template, supplies its variables and declares the JSON type it expects back.
 * Structured output is what keeps the UI renderable: a stage either produces a valid typed object or
 * the gateway degrades to the offline engine, so panels never have to render half-parsed prose.
 */
@Component
public class StageRouter {

    private final LlmGateway gateway;
    private final PromptLibrary prompts;
    private final Json json;

    public StageRouter(LlmGateway gateway, PromptLibrary prompts, Json json) {
        this.gateway = gateway;
        this.prompts = prompts;
        this.json = json;
    }

    public <T> LlmGateway.Staged<T> run(String stageId, StageContext context, Class<T> type) {
        return run(stageId, context, type, Map.of());
    }

    public <T> LlmGateway.Staged<T> run(
            String stageId, StageContext context, Class<T> type, Map<String, String> extraVariables) {
        Map<String, String> variables = baseVariables(context);
        variables.putAll(extraVariables);

        LlmRequest request =
                LlmRequest.forStage(stageId)
                        .system(prompts.system())
                        .userMessage(prompts.render(stageId, variables))
                        .context(Map.of("context", context))
                        .jsonMode(true)
                        .build();

        return gateway.run(request, result -> json.readStructured(result.text(), type).orElse(null));
    }

    public LlmGateway gateway() {
        return gateway;
    }

    private Map<String, String> baseVariables(StageContext context) {
        Map<String, String> variables = new HashMap<>();
        variables.put("problemTitle", blankToDash(context.problemTitle()));
        variables.put("problemStatement", blankToDash(context.problemStatement()));
        variables.put("constraints", blankToDash(context.constraintsText()));
        variables.put("topics", String.join(", ", context.topics()));
        variables.put("difficulty", blankToDash(context.difficulty()));
        variables.put("code", context.hasCode() ? context.code() : "(no code submitted yet)");
        variables.put("language", blankToDash(context.language()));
        variables.put("question", blankToDash(context.question()));
        variables.put("allowSpoilers", String.valueOf(context.allowSpoilers()));
        variables.put("hintLevel", String.valueOf(context.hintLevel()));
        variables.put("history", renderHistory(context.history()));
        return variables;
    }

    private String renderHistory(List<LlmMessage> history) {
        if (history == null || history.isEmpty()) {
            return "(this is the first message)";
        }
        return history.stream()
                .map(m -> ("user".equals(m.role()) ? "Learner: " : "Mentor: ") + m.content())
                .collect(Collectors.joining("\n\n"));
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
