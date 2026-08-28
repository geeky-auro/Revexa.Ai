package ai.revexa.progress.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProgressDtos {

    private ProgressDtos() {}

    public record ProgressSummary(
            Headline headline,
            List<TopicMastery> strongTopics,
            List<TopicMastery> weakTopics,
            List<MistakePattern> recurringMistakes,
            List<TrendPoint> qualityTrend,
            List<ComplexityBucket> complexityMix,
            HintDependency hintDependency,
            List<RecentActivity> recentActivity) {}

    public record Headline(
            long problemsAttempted,
            long reviewsRun,
            long optimalSolutions,
            int optimalRate,
            int averageScore,
            int scoreDelta,
            long currentStreakDays,
            long bookmarks) {}

    public record TopicMastery(
            String topic, long attempts, long optimal, int mastery, String verdict, String advice) {}

    public record MistakePattern(String title, long occurrences, String severity, String coaching) {}

    public record TrendPoint(LocalDate date, int averageScore, int reviews) {}

    public record ComplexityBucket(String complexity, long count, boolean optimalForItsProblems) {}

    public record HintDependency(
            int hintsPerProblem, int solutionsRevealed, int independenceScore, String verdict, String advice) {}

    public record RecentActivity(
            UUID reviewId, UUID problemId, String problemTitle, String verdict, int score, Instant at) {}

    // ------------------------------------------------------- recommendations

    public record Recommendation(
            String id,
            String kind,
            String title,
            String reason,
            String action,
            List<String> practiceProblems,
            List<String> concepts,
            int priority) {}

    public record RecommendationBundle(List<Recommendation> recommendations, String focusOfTheWeek) {}

    // ---------------------------------------------------------------- report

    public record LearningReport(
            String generatedFor,
            Instant generatedAt,
            ProgressSummary summary,
            List<Recommendation> recommendations,
            String markdown) {}
}
