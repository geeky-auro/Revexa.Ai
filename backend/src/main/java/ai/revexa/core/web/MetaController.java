package ai.revexa.core.web;

import ai.revexa.intelligence.dto.HintLevel;
import ai.revexa.intelligence.heuristic.PatternCatalog;
import ai.revexa.intelligence.llm.LlmGateway;
import ai.revexa.practice.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public metadata the client needs before a user has signed in. */
@RestController
@RequestMapping("/api/v1/meta")
@Tag(name = "Meta")
public class MetaController {

    private final LlmGateway gateway;
    private final PatternCatalog catalog;

    public MetaController(LlmGateway gateway, PatternCatalog catalog) {
        this.gateway = gateway;
        this.catalog = catalog;
    }

    public record HintLevelInfo(int level, String name, String title, String intent, boolean spoiler) {}

    public record PatternInfo(String id, String name, List<String> topics, String optimalTime, String optimalSpace) {}

    public record MetaResponse(
            String product,
            String version,
            List<String> languages,
            List<HintLevelInfo> hintLadder,
            List<LlmGateway.ProviderStatus> aiProviders,
            List<PatternInfo> patterns) {}

    @GetMapping
    @Operation(summary = "Languages, the hint ladder, AI provider status and the pattern catalogue")
    public MetaResponse meta() {
        return new MetaResponse(
                "Revexa.Ai",
                "0.1.0",
                SubmissionService.supportedLanguages(),
                Arrays.stream(HintLevel.values())
                        .map(l -> new HintLevelInfo(l.order(), l.name(), l.title(), l.intent(), l.isSpoiler()))
                        .toList(),
                gateway.providers(),
                catalog.all().stream()
                        .map(p -> new PatternInfo(p.id(), p.name(), p.topics(), p.optimalTime(), p.optimalSpace()))
                        .toList());
    }
}
