package ai.revexa.intelligence.provider;

import ai.revexa.core.json.Json;
import ai.revexa.intelligence.heuristic.HeuristicEngine;
import ai.revexa.intelligence.llm.LlmClient;
import ai.revexa.intelligence.llm.LlmException;
import ai.revexa.intelligence.llm.LlmRequest;
import ai.revexa.intelligence.llm.LlmResult;
import ai.revexa.intelligence.pipeline.StageContext;
import ai.revexa.intelligence.pipeline.Stages;
import org.springframework.stereotype.Component;

/**
 * The always-available provider.
 *
 * <p>It implements {@link LlmClient} so the pipeline cannot tell it apart from a hosted model: it
 * answers the same stage ids and emits the same JSON shapes. The difference is that its answers come
 * from static analysis and a curated pattern catalogue, so they are deterministic, free and offline.
 */
@Component
public class HeuristicLlmClient implements LlmClient {

    public static final String ID = "heuristic";
    private static final String MODEL = "revexa-mentor-rules-v1";

    private final HeuristicEngine engine;
    private final Json json;

    public HeuristicLlmClient(HeuristicEngine engine, Json json) {
        this.engine = engine;
        this.json = json;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String model() {
        return MODEL;
    }

    @Override
    public LlmResult complete(LlmRequest request) {
        long started = System.nanoTime();
        StageContext context = contextOf(request);
        Object payload =
                switch (request.stageId()) {
                    case Stages.PROBLEM_UNDERSTANDING -> engine.understand(context);
                    case Stages.SOLUTION_ANALYSIS -> engine.analyzeSolution(context);
                    case Stages.COMPLEXITY_ANALYSIS -> engine.complexity(context);
                    case Stages.OPTIMIZATION_DETECTION -> engine.optimize(context);
                    case Stages.HINT_GENERATION -> engine.hint(context);
                    case Stages.CHAT_ASSISTANCE -> engine.chat(context);
                    case Stages.SOLUTION_COMPARISON -> engine.compare(context);
                    default -> throw new LlmException("Unknown pipeline stage: " + request.stageId());
                };
        long latency = (System.nanoTime() - started) / 1_000_000;
        return LlmResult.of(json.write(payload), ID, MODEL, latency);
    }

    private StageContext contextOf(LlmRequest request) {
        Object raw = request.context().get("context");
        if (raw instanceof StageContext context) {
            return context;
        }
        throw new LlmException("The heuristic provider requires a StageContext on the request");
    }
}
