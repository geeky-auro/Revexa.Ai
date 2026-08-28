package ai.revexa.intelligence.dto;

import java.util.List;

/**
 * Stage 4 output. Deliberately withholds the implementation: {@code keyInsight} and {@code nudges}
 * describe the missing observation, never the finished code.
 */
public record OptimizationInsight(
        boolean betterApproachExists,
        String keyInsight,
        String direction,
        List<String> nudges,
        String targetTime,
        String targetSpace,
        String patternName) {}
