package ai.revexa.intelligence.dto;

import java.util.List;

/** Stage 2 output: the reviewer's reading of what the learner built and why. */
public record ApproachSummary(
        String name,
        String inferredIntuition,
        String plainExplanation,
        List<String> steps,
        List<String> dataStructures,
        List<String> patterns,
        String language) {}
