package ai.revexa.intelligence.dto;

import java.util.List;

/** Stage output for the comparison panel: the learner's approach beside the alternatives. */
public record SolutionComparison(
        ApproachOption userApproach,
        List<ApproachOption> alternatives,
        String missingInsight,
        String tradeoffSummary,
        String recommendation,
        boolean pseudocodeRevealed) {}
