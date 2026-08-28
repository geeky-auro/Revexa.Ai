package ai.revexa.progress.service;

import ai.revexa.core.config.CacheConfig;
import ai.revexa.intelligence.heuristic.AlgorithmPattern;
import ai.revexa.intelligence.heuristic.PatternCatalog;
import ai.revexa.progress.dto.ProgressDtos;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Turns the progress picture into concrete next actions.
 *
 * <p>Recommendations are grounded in the same pattern catalogue the mentor teaches from, so "practise
 * sliding windows" always comes with the specific problems and the specific concept to read.
 */
@Service
public class RecommendationService {

    private final ProgressService progress;
    private final PatternCatalog catalog;

    public RecommendationService(ProgressService progress, PatternCatalog catalog) {
        this.progress = progress;
        this.catalog = catalog;
    }

    @Cacheable(cacheNames = CacheConfig.RECOMMENDATION_CACHE, key = "#userId")
    public ProgressDtos.RecommendationBundle recommend(UUID userId) {
        ProgressDtos.ProgressSummary summary = progress.summary(userId);
        return build(summary);
    }

    public ProgressDtos.RecommendationBundle build(ProgressDtos.ProgressSummary summary) {
        List<ProgressDtos.Recommendation> recommendations = new ArrayList<>();

        if (summary.headline().reviewsRun() == 0) {
            recommendations.add(
                    new ProgressDtos.Recommendation(
                            "getting-started",
                            "ONBOARDING",
                            "Run your first review",
                            "There is no history to learn from yet.",
                            "Paste a problem you have already solved and let the mentor read your solution back to you — starting with something you know makes the feedback easier to judge.",
                            List.of("Two Sum", "Valid Anagram", "Best Time to Buy and Sell Stock"),
                            List.of("Complexity analysis", "Pattern recognition"),
                            100));
            return new ProgressDtos.RecommendationBundle(
                    recommendations, "Get one solved problem reviewed — the dashboard fills in from there.");
        }

        for (ProgressDtos.TopicMastery weak : summary.weakTopics()) {
            Optional<AlgorithmPattern> pattern = patternForTopic(weak.topic());
            recommendations.add(
                    new ProgressDtos.Recommendation(
                            "topic-" + weak.topic().toLowerCase(Locale.ROOT).replace(' ', '-'),
                            "WEAK_TOPIC",
                            "Rebuild your " + weak.topic() + " intuition",
                            "You have reached the optimal approach on only "
                                    + weak.optimal()
                                    + " of "
                                    + weak.attempts()
                                    + " "
                                    + weak.topic()
                                    + " attempts.",
                            pattern.map(AlgorithmPattern::direction)
                                    .orElse("Work through the observation step before writing code."),
                            pattern.map(AlgorithmPattern::practice).orElse(List.of()),
                            pattern.map(AlgorithmPattern::topics).orElse(List.of(weak.topic())),
                            90 - recommendations.size() * 5));
        }

        summary.recurringMistakes().stream()
                .filter(m -> m.occurrences() >= 2)
                .limit(2)
                .forEach(
                        mistake ->
                                recommendations.add(
                                        new ProgressDtos.Recommendation(
                                                "mistake-" + Math.abs(mistake.title().hashCode()),
                                                "RECURRING_MISTAKE",
                                                "Stop repeating: " + mistake.title(),
                                                "This has appeared in " + mistake.occurrences() + " of your reviews.",
                                                mistake.coaching(),
                                                List.of(),
                                                List.of("Pre-submit checklist"),
                                                80)));

        if (summary.hintDependency().independenceScore() < 55) {
            recommendations.add(
                    new ProgressDtos.Recommendation(
                            "hint-dependency",
                            "HABIT",
                            "Sit with the problem longer before the next hint",
                            summary.hintDependency().verdict()
                                    + " — you average "
                                    + summary.hintDependency().hintsPerProblem()
                                    + " hints per problem.",
                            summary.hintDependency().advice(),
                            List.of(),
                            List.of("Problem-solving process"),
                            70));
        }

        boolean quadraticHabit =
                summary.complexityMix().stream()
                        .anyMatch(b -> b.complexity().contains("n^2") && b.count() >= 2 && !b.optimalForItsProblems());
        if (quadraticHabit) {
            recommendations.add(
                    new ProgressDtos.Recommendation(
                            "quadratic-habit",
                            "COMPLEXITY",
                            "Break the nested-loop reflex",
                            "Several of your solutions land on O(n^2) where a linear approach existed.",
                            "Before writing the inner loop, ask what single question it is re-asking — then pick the structure that answers it in O(1).",
                            List.of("Two Sum", "Longest Substring Without Repeating Characters", "Subarray Sum Equals K"),
                            List.of("Hash Table", "Sliding Window", "Prefix Sum"),
                            85));
        }

        if (summary.headline().optimalRate() >= 70 && summary.weakTopics().isEmpty()) {
            recommendations.add(
                    new ProgressDtos.Recommendation(
                            "level-up",
                            "STRETCH",
                            "Move up a difficulty band",
                            "You reach the optimal approach on "
                                    + summary.headline().optimalRate()
                                    + "% of reviews — the current set is no longer stretching you.",
                            "Take on problems where the observation is two steps deep rather than one: DP on subsequences, monotonic stacks, and binary search on the answer.",
                            List.of("Longest Increasing Subsequence", "Largest Rectangle in Histogram", "Koko Eating Bananas"),
                            List.of("Dynamic Programming", "Monotonic Stack", "Binary Search"),
                            75));
        }

        recommendations.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        List<ProgressDtos.Recommendation> top = recommendations.stream().limit(5).toList();

        String focus =
                top.isEmpty()
                        ? "Keep the streak going — consistency beats intensity."
                        : top.get(0).title() + ". " + top.get(0).reason();

        return new ProgressDtos.RecommendationBundle(top, focus);
    }

    private Optional<AlgorithmPattern> patternForTopic(String topic) {
        return catalog.all().stream()
                .filter(p -> p.topics().stream().anyMatch(t -> t.equalsIgnoreCase(topic)))
                .findFirst();
    }
}
