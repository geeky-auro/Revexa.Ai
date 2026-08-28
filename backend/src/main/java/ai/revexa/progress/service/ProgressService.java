package ai.revexa.progress.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.intelligence.heuristic.Complexity;
import ai.revexa.practice.domain.BookmarkRepository;
import ai.revexa.practice.domain.HintRepository;
import ai.revexa.practice.domain.HintSession;
import ai.revexa.practice.domain.HintSessionRepository;
import ai.revexa.practice.domain.Review;
import ai.revexa.practice.domain.ReviewRepository;
import ai.revexa.practice.domain.SubmissionRepository;
import ai.revexa.progress.dto.ProgressDtos;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns review history into the numbers a learner can act on.
 *
 * <p>Everything here is derived from stored reviews rather than a separate metrics pipeline, which
 * keeps the dashboard consistent with the review history by construction.
 */
@Service
public class ProgressService {

    private final ReviewRepository reviews;
    private final SubmissionRepository submissions;
    private final HintSessionRepository hintSessions;
    private final HintRepository hints;
    private final BookmarkRepository bookmarks;
    private final ProblemService problems;

    public ProgressService(
            ReviewRepository reviews,
            SubmissionRepository submissions,
            HintSessionRepository hintSessions,
            HintRepository hints,
            BookmarkRepository bookmarks,
            ProblemService problems) {
        this.reviews = reviews;
        this.submissions = submissions;
        this.hintSessions = hintSessions;
        this.hints = hints;
        this.bookmarks = bookmarks;
        this.problems = problems;
    }

    @Transactional(readOnly = true)
    public ProgressDtos.ProgressSummary summary(UUID userId) {
        List<Review> history = reviews.findByUserIdOrderByCreatedAtAsc(userId);

        return new ProgressDtos.ProgressSummary(
                headline(userId, history),
                topics(history, true),
                topics(history, false),
                mistakes(history),
                trend(history),
                complexityMix(history),
                hintDependency(userId, history),
                recentActivity(history));
    }

    private ProgressDtos.Headline headline(UUID userId, List<Review> history) {
        long attempted = history.stream().map(Review::getProblemId).distinct().count();
        long optimal = history.stream().filter(Review::isTimeOptimal).count();
        int averageScore =
                (int) Math.round(history.stream().mapToInt(Review::getScore).average().orElse(0));

        // Compare the most recent third against the oldest third, so improvement is measured rather
        // than asserted.
        int delta = 0;
        if (history.size() >= 6) {
            int window = Math.max(2, history.size() / 3);
            double early =
                    history.subList(0, window).stream().mapToInt(Review::getScore).average().orElse(0);
            double late =
                    history.subList(history.size() - window, history.size()).stream()
                            .mapToInt(Review::getScore)
                            .average()
                            .orElse(0);
            delta = (int) Math.round(late - early);
        }

        return new ProgressDtos.Headline(
                attempted,
                history.size(),
                optimal,
                history.isEmpty() ? 0 : (int) Math.round(100.0 * optimal / history.size()),
                averageScore,
                delta,
                streakDays(history),
                bookmarks.countByUserId(userId));
    }

    private long streakDays(List<Review> history) {
        Set<LocalDate> days =
                history.stream()
                        .map(r -> LocalDate.ofInstant(r.getCreatedAt(), ZoneOffset.UTC))
                        .collect(Collectors.toSet());
        if (days.isEmpty()) {
            return 0;
        }
        LocalDate cursor = LocalDate.now(ZoneOffset.UTC);
        if (!days.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!days.contains(cursor)) {
                return 0;
            }
        }
        long streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private List<ProgressDtos.TopicMastery> topics(List<Review> history, boolean strong) {
        Map<String, long[]> counters = new HashMap<>();
        for (Review review : history) {
            for (String topic : review.getTopics()) {
                long[] counter = counters.computeIfAbsent(topic, t -> new long[2]);
                counter[0]++;
                if (review.isTimeOptimal()) {
                    counter[1]++;
                }
            }
        }
        List<ProgressDtos.TopicMastery> mastery =
                counters.entrySet().stream()
                        .filter(e -> e.getValue()[0] >= 1)
                        .map(
                                e -> {
                                    long attempts = e.getValue()[0];
                                    long optimal = e.getValue()[1];
                                    int score = (int) Math.round(100.0 * optimal / attempts);
                                    return new ProgressDtos.TopicMastery(
                                            e.getKey(),
                                            attempts,
                                            optimal,
                                            score,
                                            verdictFor(score, attempts),
                                            adviceFor(e.getKey(), score, attempts));
                                })
                        .sorted(
                                strong
                                        ? Comparator.comparingInt(ProgressDtos.TopicMastery::mastery)
                                                .reversed()
                                                .thenComparing(Comparator.comparingLong(ProgressDtos.TopicMastery::attempts).reversed())
                                        : Comparator.comparingInt(ProgressDtos.TopicMastery::mastery)
                                                .thenComparing(Comparator.comparingLong(ProgressDtos.TopicMastery::attempts).reversed()))
                        .toList();

        // A single attempt is an anecdote, not a weakness — require two before calling a topic weak.
        return mastery.stream()
                .filter(m -> strong ? m.mastery() >= 60 : m.mastery() < 60 && m.attempts() >= 2)
                .limit(5)
                .toList();
    }

    private String verdictFor(int mastery, long attempts) {
        if (attempts < 2) {
            return "Not enough data";
        }
        if (mastery >= 80) {
            return "Strong";
        }
        if (mastery >= 60) {
            return "Solid";
        }
        if (mastery >= 30) {
            return "Developing";
        }
        return "Needs work";
    }

    private String adviceFor(String topic, int mastery, long attempts) {
        if (attempts < 2) {
            return "Only " + attempts + " attempt so far — one more will make this reading meaningful.";
        }
        if (mastery >= 80) {
            return "You reach the optimal approach on " + topic + " reliably. Use it as the anchor when a new problem looks unfamiliar.";
        }
        if (mastery >= 60) {
            return "You usually get there on " + topic + ", but not first time. Practise naming the pattern before you write any code.";
        }
        return "You keep landing on a slower approach for " + topic
                + ". Work through the observation step deliberately — that is where the gap is, not in the implementation.";
    }

    private List<ProgressDtos.MistakePattern> mistakes(List<Review> history) {
        Map<String, Long> counts =
                history.stream()
                        .flatMap(r -> r.getIssueTitles().stream())
                        .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(
                        e ->
                                new ProgressDtos.MistakePattern(
                                        e.getKey(),
                                        e.getValue(),
                                        e.getValue() >= 4 ? "HIGH" : e.getValue() >= 2 ? "MEDIUM" : "LOW",
                                        coachingFor(e.getKey(), e.getValue())))
                .toList();
    }

    private String coachingFor(String title, long occurrences) {
        String lower = title.toLowerCase();
        if (lower.contains("empty")) {
            return "This has come up " + occurrences + " times. Make the empty-input guard the first line you type, before the algorithm.";
        }
        if (lower.contains("overflow")) {
            return "Recurring overflow risk. Default to low + (high - low) / 2 and to a 64-bit accumulator in fixed-width languages.";
        }
        if (lower.contains("memo")) {
            return "You reach for recursion before you reach for the cache. Write the state signature down before the recursion.";
        }
        if (lower.contains("slow") || lower.contains("repeated work") || lower.contains("constraints")) {
            return "Read the constraints before writing code — they tell you the target complexity, and this has cost you " + occurrences + " reviews.";
        }
        return "Seen " + occurrences + " times. Add it to your pre-submit checklist until it stops appearing.";
    }

    private List<ProgressDtos.TrendPoint> trend(List<Review> history) {
        Map<LocalDate, List<Integer>> byDay = new TreeMap<>();
        for (Review review : history) {
            byDay.computeIfAbsent(LocalDate.ofInstant(review.getCreatedAt(), ZoneOffset.UTC), d -> new ArrayList<>())
                    .add(review.getScore());
        }
        return byDay.entrySet().stream()
                .map(
                        e ->
                                new ProgressDtos.TrendPoint(
                                        e.getKey(),
                                        (int) Math.round(e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0)),
                                        e.getValue().size()))
                .toList();
    }

    private List<ProgressDtos.ComplexityBucket> complexityMix(List<Review> history) {
        Map<String, long[]> buckets = new LinkedHashMap<>();
        for (Review review : history) {
            long[] counter = buckets.computeIfAbsent(Complexity.normalize(review.getTimeComplexity()), c -> new long[2]);
            counter[0]++;
            if (review.isTimeOptimal()) {
                counter[1]++;
            }
        }
        return buckets.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> Complexity.rank(e.getKey())))
                .map(e -> new ProgressDtos.ComplexityBucket(e.getKey(), e.getValue()[0], e.getValue()[1] == e.getValue()[0]))
                .toList();
    }

    private ProgressDtos.HintDependency hintDependency(UUID userId, List<Review> history) {
        List<HintSession> sessions = hintSessions.findByUserId(userId);
        long problemsTouched = Math.max(1, history.stream().map(Review::getProblemId).distinct().count());
        long totalHints = sessions.stream().mapToLong(s -> hints.findBySessionIdOrderByLevelAsc(s.getId()).size()).sum();
        int revealed = (int) sessions.stream().filter(HintSession::isSolutionRevealed).count();

        int perProblem = (int) Math.round((double) totalHints / problemsTouched);
        int independence = Math.max(0, Math.min(100, 100 - (perProblem * 12) - (revealed * 10)));

        String verdict;
        String advice;
        if (sessions.isEmpty()) {
            verdict = "Fully independent";
            advice = "You have not needed a hint yet. When you do get stuck, the ladder is there — using rung 1 or 2 is not cheating.";
        } else if (independence >= 75) {
            verdict = "Healthy";
            advice = "You take a nudge and then finish alone. That is exactly the pattern that builds intuition.";
        } else if (independence >= 45) {
            verdict = "Leaning on hints";
            advice = "You are climbing the ladder further than you need to. Try setting a 10-minute timer before the next rung.";
        } else {
            verdict = "Hint dependent";
            advice = "The solutions are being revealed before the thinking happens. Stop at rung 2 for the next five problems, even if you stall.";
        }
        return new ProgressDtos.HintDependency(perProblem, revealed, independence, verdict, advice);
    }

    private List<ProgressDtos.RecentActivity> recentActivity(List<Review> history) {
        List<Review> recent = new ArrayList<>(history);
        recent.sort(Comparator.comparing(Review::getCreatedAt).reversed());
        return recent.stream()
                .limit(8)
                .map(
                        r ->
                                new ProgressDtos.RecentActivity(
                                        r.getId(),
                                        r.getProblemId(),
                                        problems.titleOf(r.getProblemId()),
                                        r.getVerdict(),
                                        r.getScore(),
                                        r.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long submissionCount(UUID userId) {
        return submissions.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long reviewsSince(UUID userId, Instant since) {
        return reviews.countByUserIdAndCreatedAtAfter(userId, since);
    }
}
