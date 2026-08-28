package ai.revexa.intelligence.dto;

import java.util.List;

/** Stage 2 bundle: what the code does, plus what is wrong or risky about it. */
public record SolutionAnalysis(ApproachSummary approach, List<Finding> findings) {}
