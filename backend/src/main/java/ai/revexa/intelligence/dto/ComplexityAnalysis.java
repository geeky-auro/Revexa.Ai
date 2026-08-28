package ai.revexa.intelligence.dto;

import java.util.List;

/** Stage 3 output: measured-vs-achievable cost, with the reasoning shown. */
public record ComplexityAnalysis(
        String time,
        String space,
        String timeExplanation,
        String spaceExplanation,
        String optimalTime,
        String optimalSpace,
        boolean timeOptimal,
        boolean spaceOptimal,
        int confidence,
        List<Contributor> contributors) {

    /** One line of the cost breakdown, e.g. "nested scan over nums → O(n^2)". */
    public record Contributor(String label, String complexity, String reason) {}
}
