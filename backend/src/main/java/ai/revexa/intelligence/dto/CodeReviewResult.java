package ai.revexa.intelligence.dto;

import java.util.List;

/** The assembled review handed to the UI — one object, one render pass. */
public record CodeReviewResult(
        ProblemUnderstanding understanding,
        ApproachSummary approach,
        ComplexityAnalysis complexity,
        List<Finding> findings,
        OptimizationInsight optimization,
        int score,
        String verdict,
        String mentorNote,
        List<String> nextSteps,
        List<String> topics,
        String provider,
        String model) {

    public static final String VERDICT_OPTIMAL = "OPTIMAL";
    public static final String VERDICT_SOLID = "SOLID";
    public static final String VERDICT_IMPROVABLE = "IMPROVABLE";
    public static final String VERDICT_INEFFICIENT = "INEFFICIENT";
    public static final String VERDICT_RISKY = "RISKY";
}
