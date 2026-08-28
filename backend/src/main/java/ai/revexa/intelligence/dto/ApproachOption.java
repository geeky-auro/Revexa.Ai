package ai.revexa.intelligence.dto;

import java.util.List;

/** One column of the comparison table. */
public record ApproachOption(
        String id,
        String name,
        String summary,
        String timeComplexity,
        String spaceComplexity,
        List<String> pros,
        List<String> cons,
        String whenToPrefer,
        String keyInsight,
        boolean userApproach,
        boolean optimal,
        String pseudocode) {}
