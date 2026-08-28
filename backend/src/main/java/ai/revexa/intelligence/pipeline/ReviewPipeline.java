package ai.revexa.intelligence.pipeline;

import ai.revexa.intelligence.dto.ApproachSummary;
import ai.revexa.intelligence.dto.ChatReply;
import ai.revexa.intelligence.dto.CodeReviewResult;
import ai.revexa.intelligence.dto.ComplexityAnalysis;
import ai.revexa.intelligence.dto.Finding;
import ai.revexa.intelligence.dto.GeneratedHint;
import ai.revexa.intelligence.dto.OptimizationInsight;
import ai.revexa.intelligence.dto.ProblemUnderstanding;
import ai.revexa.intelligence.dto.SolutionAnalysis;
import ai.revexa.intelligence.dto.SolutionComparison;
import ai.revexa.intelligence.heuristic.Complexity;
import ai.revexa.intelligence.heuristic.TopicNames;
import ai.revexa.intelligence.llm.LlmGateway;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the analysis stages into a single review.
 *
 * <p>The independent stages (understanding, solution analysis, complexity) are fanned out in
 * parallel; optimisation detection depends on the measured complexity, so it runs after. Splitting
 * the work this way keeps each prompt small and focused, which is both cheaper and markedly more
 * reliable than asking one prompt to do everything.
 */
@Service
public class ReviewPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipeline.class);

    private final StageRouter router;
    private final Executor executor;

    public ReviewPipeline(StageRouter router, @Qualifier("aiExecutor") Executor executor) {
        this.router = router;
        this.executor = executor;
    }

    public CodeReviewResult review(StageContext context) {
        long started = System.currentTimeMillis();

        CompletableFuture<LlmGateway.Staged<ProblemUnderstanding>> understanding =
                CompletableFuture.supplyAsync(
                        () -> router.run(Stages.PROBLEM_UNDERSTANDING, context, ProblemUnderstanding.class), executor);
        CompletableFuture<LlmGateway.Staged<SolutionAnalysis>> analysis =
                CompletableFuture.supplyAsync(
                        () -> router.run(Stages.SOLUTION_ANALYSIS, context, SolutionAnalysis.class), executor);
        CompletableFuture<LlmGateway.Staged<ComplexityAnalysis>> complexity =
                CompletableFuture.supplyAsync(
                        () -> router.run(Stages.COMPLEXITY_ANALYSIS, context, ComplexityAnalysis.class), executor);

        CompletableFuture.allOf(understanding, analysis, complexity).join();

        ProblemUnderstanding understandingResult = understanding.join().value();
        SolutionAnalysis analysisResult = analysis.join().value();
        ComplexityAnalysis complexityResult = complexity.join().value();

        LlmGateway.Staged<OptimizationInsight> optimization =
                router.run(
                        Stages.OPTIMIZATION_DETECTION,
                        context,
                        OptimizationInsight.class,
                        Map.of(
                                "measuredTime", complexityResult.time(),
                                "measuredSpace", complexityResult.space()));

        ApproachSummary approach = analysisResult.approach();
        List<Finding> findings = analysisResult.findings() == null ? List.of() : analysisResult.findings();
        OptimizationInsight insight = optimization.value();

        int score = score(complexityResult, findings);
        String verdict = verdict(score, complexityResult, findings);

        CodeReviewResult result =
                new CodeReviewResult(
                        understandingResult,
                        approach,
                        complexityResult,
                        findings,
                        insight,
                        score,
                        verdict,
                        mentorNote(verdict, complexityResult, insight, findings),
                        nextSteps(complexityResult, insight, findings),
                        topics(understandingResult, approach),
                        optimization.provider(),
                        optimization.model());

        log.debug("Review assembled in {} ms via provider {}", System.currentTimeMillis() - started, optimization.provider());
        return result;
    }

    public GeneratedHint hint(StageContext context) {
        var level = ai.revexa.intelligence.dto.HintLevel.ofOrder(context.hintLevel());
        return router
                .run(
                        Stages.HINT_GENERATION,
                        context,
                        GeneratedHint.class,
                        Map.of(
                                "hintLevelName", level.name(),
                                "hintIntent", level.intent()))
                .value();
    }

    public ChatReply chat(StageContext context) {
        return router.run(Stages.CHAT_ASSISTANCE, context, ChatReply.class).value();
    }

    public SolutionComparison compare(StageContext context) {
        LlmGateway.Staged<ComplexityAnalysis> complexity =
                router.run(Stages.COMPLEXITY_ANALYSIS, context, ComplexityAnalysis.class);
        return router
                .run(
                        Stages.SOLUTION_COMPARISON,
                        context,
                        SolutionComparison.class,
                        Map.of(
                                "measuredTime", complexity.value().time(),
                                "measuredSpace", complexity.value().space()))
                .value();
    }

    public ComplexityAnalysis complexity(StageContext context) {
        return router.run(Stages.COMPLEXITY_ANALYSIS, context, ComplexityAnalysis.class).value();
    }

    public ProblemUnderstanding understand(StageContext context) {
        return router.run(Stages.PROBLEM_UNDERSTANDING, context, ProblemUnderstanding.class).value();
    }

    // --------------------------------------------------------------- scoring

    private int score(ComplexityAnalysis complexity, List<Finding> findings) {
        int score = 100;
        int timeGap = Complexity.rank(complexity.time()) - Complexity.rank(complexity.optimalTime());
        if (timeGap > 0) {
            score -= Math.min(45, 12 + timeGap / 2);
        }
        int spaceGap = Complexity.rank(complexity.space()) - Complexity.rank(complexity.optimalSpace());
        if (spaceGap > 0) {
            score -= Math.min(12, 4 + spaceGap / 4);
        }
        for (Finding finding : findings) {
            score -=
                    switch (finding.severity() == null ? "" : finding.severity()) {
                        case Finding.SEVERITY_CRITICAL -> 18;
                        case Finding.SEVERITY_HIGH -> 10;
                        case Finding.SEVERITY_MEDIUM -> 5;
                        default -> 2;
                    };
        }
        return Math.max(12, Math.min(100, score));
    }

    private String verdict(int score, ComplexityAnalysis complexity, List<Finding> findings) {
        boolean critical =
                findings.stream()
                        .anyMatch(
                                f ->
                                        Finding.SEVERITY_CRITICAL.equals(f.severity())
                                                && Finding.TYPE_CORRECTNESS.equals(f.type()));
        if (critical) {
            return CodeReviewResult.VERDICT_RISKY;
        }
        if (complexity.timeOptimal() && score >= 85) {
            return CodeReviewResult.VERDICT_OPTIMAL;
        }
        int gap = Complexity.rank(complexity.time()) - Complexity.rank(complexity.optimalTime());
        if (gap >= 20) {
            return CodeReviewResult.VERDICT_INEFFICIENT;
        }
        return score >= 70 ? CodeReviewResult.VERDICT_SOLID : CodeReviewResult.VERDICT_IMPROVABLE;
    }

    private String mentorNote(
            String verdict, ComplexityAnalysis complexity, OptimizationInsight insight, List<Finding> findings) {
        long blocking =
                findings.stream()
                        .filter(f -> Finding.SEVERITY_CRITICAL.equals(f.severity()) || Finding.SEVERITY_HIGH.equals(f.severity()))
                        .count();
        return switch (verdict) {
            case CodeReviewResult.VERDICT_OPTIMAL ->
                    "This is the solution you want to be able to write in an interview: "
                            + complexity.time()
                            + " time, "
                            + complexity.space()
                            + " space, and the reasoning is visible in the code. Spend your remaining time on edge cases.";
            case CodeReviewResult.VERDICT_RISKY ->
                    "Correctness first — there "
                            + (blocking == 1 ? "is 1 blocking issue" : "are " + blocking + " blocking issues")
                            + " that will fail on a hidden test before performance ever matters.";
            case CodeReviewResult.VERDICT_INEFFICIENT ->
                    "The logic holds, but at "
                            + complexity.time()
                            + " against an achievable "
                            + complexity.optimalTime()
                            + " this will time out. You are one observation short, and it is in the optimisation panel.";
            case CodeReviewResult.VERDICT_SOLID ->
                    "Solid work. The approach is sound; the gap to the best version is narrow and worth closing before you move on.";
            default ->
                    "There is a real idea in here. "
                            + (insight != null && insight.betterApproachExists()
                                    ? "The next step is the observation in the optimisation panel — try it before reading the comparison."
                                    : "Tighten the edge cases and it will hold up.");
        };
    }

    private List<String> nextSteps(
            ComplexityAnalysis complexity, OptimizationInsight insight, List<Finding> findings) {
        List<String> steps = new ArrayList<>();
        findings.stream()
                .filter(f -> Finding.SEVERITY_CRITICAL.equals(f.severity()) || Finding.SEVERITY_HIGH.equals(f.severity()))
                .limit(2)
                .forEach(f -> steps.add(f.suggestion() == null ? f.title() : f.suggestion()));
        if (insight != null && insight.betterApproachExists()) {
            steps.add("Re-derive the key observation yourself before opening the comparison panel.");
            steps.add("Target " + insight.targetTime() + " time and " + insight.targetSpace() + " space on the rewrite.");
        } else {
            steps.add("Write out why no faster algorithm exists — that argument is what interviews actually probe.");
        }
        if (!complexity.spaceOptimal()) {
            steps.add("See whether the auxiliary structure can be reduced to " + complexity.optimalSpace() + ".");
        }
        return steps.stream().distinct().limit(4).toList();
    }

    /**
     * Topics reach the progress dashboard from two directions, so they are canonicalised here rather
     * than at each call site — otherwise "Two Pointers" and "Two pointers" become two weak topics.
     */
    private List<String> topics(ProblemUnderstanding understanding, ApproachSummary approach) {
        Set<String> topics = new LinkedHashSet<>();
        if (understanding != null && understanding.topics() != null) {
            topics.addAll(TopicNames.canonicalise(understanding.topics()));
        }
        if (approach != null && approach.patterns() != null) {
            topics.addAll(TopicNames.canonicalise(approach.patterns()));
        }
        return new ArrayList<>(topics);
    }
}
