package ai.revexa.intelligence.heuristic;

import java.util.List;
import java.util.Map;

/** One entry of the algorithmic knowledge base loaded from {@code knowledge/patterns.json}. */
public record AlgorithmPattern(
        String id,
        String name,
        List<String> topics,
        List<String> problemKeywords,
        List<String> codeSignals,
        String bruteForceTime,
        String bruteForceSpace,
        String optimalTime,
        String optimalSpace,
        String keyInsight,
        String direction,
        Map<String, String> ladder,
        Map<String, List<String>> questions,
        List<Alternative> alternatives,
        List<String> practice) {

    public record Alternative(
            String id,
            String name,
            String summary,
            String time,
            String space,
            List<String> pros,
            List<String> cons,
            String whenToPrefer,
            String keyInsight) {}

    public String rung(String key) {
        return ladder == null ? "" : ladder.getOrDefault(key, "");
    }

    public List<String> questionsFor(String key) {
        return questions == null ? List.of() : questions.getOrDefault(key, List.of());
    }
}
