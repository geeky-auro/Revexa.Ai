package ai.revexa.core.seed;

import ai.revexa.catalog.domain.Problem;
import ai.revexa.catalog.domain.ProblemRepository;
import ai.revexa.core.config.RevexaProperties;
import ai.revexa.identity.domain.UserAccount;
import ai.revexa.identity.domain.UserAccountRepository;
import ai.revexa.practice.dto.PracticeDtos;
import ai.revexa.practice.service.BookmarkService;
import ai.revexa.practice.service.ChatService;
import ai.revexa.practice.service.HintService;
import ai.revexa.practice.service.ReviewService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a demo account with a realistic practice history.
 *
 * <p>An empty dashboard cannot demonstrate progress tracking, so the seeder runs a real spread of
 * submissions — brute force early, optimal later — through the actual review pipeline. Nothing here
 * is faked: the scores, complexities and weak topics on the demo dashboard are produced by the same
 * code path a real submission takes. Disable with {@code revexa.seed-demo-data=false}.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    public static final String DEMO_EMAIL = "demo@revexa.ai";
    public static final String DEMO_PASSWORD = "revexa-demo";

    private final RevexaProperties properties;
    private final UserAccountRepository users;
    private final ProblemRepository problems;
    private final ReviewService reviews;
    private final HintService hints;
    private final ChatService chat;
    private final BookmarkService bookmarks;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DemoDataSeeder(
            RevexaProperties properties,
            UserAccountRepository users,
            ProblemRepository problems,
            ReviewService reviews,
            HintService hints,
            ChatService chat,
            BookmarkService bookmarks,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc) {
        this.properties = properties;
        this.users = users;
        this.problems = problems;
        this.reviews = reviews;
        this.hints = hints;
        this.chat = chat;
        this.bookmarks = bookmarks;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    private record Attempt(String slug, String language, int daysAgo, String code) {}

    /**
     * Not transactional on purpose: each service call commits on its own so the JDBC backdating below
     * lands on committed rows instead of being overwritten when the JPA context flushes.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSeedDemoData() || users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }
        long started = System.currentTimeMillis();

        UserAccount demo = new UserAccount();
        demo.setEmail(DEMO_EMAIL);
        demo.setDisplayName("Demo Learner");
        demo.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        demo.setPreferredLanguage("python");
        demo.setLastActiveAt(Instant.now());
        UUID userId = users.save(demo).getId();

        for (Attempt attempt : attempts()) {
            Optional<Problem> problem = problems.findBySlugAndCuratedTrue(attempt.slug());
            if (problem.isEmpty()) {
                continue;
            }
            try {
                PracticeDtos.ReviewView review =
                        reviews.review(
                                new PracticeDtos.CreateReviewRequest(
                                        null, problem.get().getId(), attempt.code(), attempt.language(), true),
                                userId);
                backdate(review.submissionId(), review.id(), attempt.daysAgo());
            } catch (RuntimeException e) {
                log.warn("Skipping demo attempt for {}: {}", attempt.slug(), e.getMessage());
            }
        }

        seedMentoring(userId);

        log.info(
                "Seeded demo account {} with {} reviews in {} ms",
                DEMO_EMAIL,
                attempts().size(),
                System.currentTimeMillis() - started);
    }

    /** Moves a seeded review back in time so the trend chart has a real time axis. */
    private void backdate(UUID submissionId, UUID reviewId, int daysAgo) {
        Instant at = Instant.now().minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours(daysAgo % 7));
        jdbc.update("update submission set created_at = ?, updated_at = ? where id = ?", at, at, submissionId);
        jdbc.update("update review set created_at = ?, updated_at = ? where id = ?", at, at, reviewId);
    }

    private void seedMentoring(UUID userId) {
        problems
                .findBySlugAndCuratedTrue("subarray-sum-equals-k")
                .ifPresent(
                        problem -> {
                            var session =
                                    hints.start(new PracticeDtos.StartHintSessionRequest(problem.getId(), null), userId);
                            hints.next(session.id(), new PracticeDtos.NextHintRequest(false, null, null), userId);
                            hints.next(session.id(), new PracticeDtos.NextHintRequest(false, null, null), userId);
                        });

        problems
                .findBySlugAndCuratedTrue("longest-substring-without-repeating-characters")
                .ifPresent(
                        problem -> {
                            var thread =
                                    chat.start(
                                            new PracticeDtos.StartThreadRequest(
                                                    problem.getId(), null, "Why is my nested loop too slow?"),
                                            userId);
                            chat.send(
                                    thread.id(),
                                    new PracticeDtos.SendMessageRequest(
                                            "I check every substring for duplicates. Is my approach on the right track?",
                                            false,
                                            null,
                                            null),
                                    userId);
                            chat.send(
                                    thread.id(),
                                    new PracticeDtos.SendMessageRequest(
                                            "What is the time complexity of what I have now?", false, null, null),
                                    userId);
                        });

        bookmarks.create(
                new PracticeDtos.CreateBookmarkRequest(
                        "INSIGHT",
                        problems.findBySlugAndCuratedTrue("two-sum").map(Problem::getId).orElse(null),
                        null,
                        "The complement trick",
                        "While scanning left to right you already know everything to your left. Instead of searching "
                                + "for the partner, ask whether the partner has already been seen.",
                        "This same reframing shows up in Subarray Sum Equals K and in 4Sum II."),
                userId);

        bookmarks.create(
                new PracticeDtos.CreateBookmarkRequest(
                        "COMPLEXITY",
                        problems.findBySlugAndCuratedTrue("koko-eating-bananas").map(Problem::getId).orElse(null),
                        null,
                        "Binary search needs monotonicity, not sortedness",
                        "If feasible(x) is false below the answer and true above it, you can binary search the answer "
                                + "space itself even though nothing was ever sorted.",
                        "Read again before any 'minimum k such that...' problem."),
                userId);
    }

    /** A believable arc: brute force early, the same problems solved properly later. */
    private List<Attempt> attempts() {
        return List.of(
                new Attempt(
                        "two-sum",
                        "python",
                        21,
                        """
                        class Solution:
                            def twoSum(self, nums, target):
                                for i in range(len(nums)):
                                    for j in range(i + 1, len(nums)):
                                        if nums[i] + nums[j] == target:
                                            return [i, j]
                                return []
                        """),
                new Attempt(
                        "valid-anagram",
                        "javascript",
                        19,
                        """
                        function isAnagram(s, t) {
                            if (s.length !== t.length) return false;
                            return s.split('').sort().join('') === t.split('').sort().join('');
                        }
                        """),
                new Attempt(
                        "longest-substring-without-repeating-characters",
                        "java",
                        17,
                        """
                        class Solution {
                            public int lengthOfLongestSubstring(String s) {
                                int best = 0;
                                for (int i = 0; i < s.length(); i++) {
                                    for (int j = i; j < s.length(); j++) {
                                        if (allUnique(s, i, j)) {
                                            best = Math.max(best, j - i + 1);
                                        }
                                    }
                                }
                                return best;
                            }

                            private boolean allUnique(String s, int start, int end) {
                                boolean[] seen = new boolean[128];
                                for (int k = start; k <= end; k++) {
                                    if (seen[s.charAt(k)]) return false;
                                    seen[s.charAt(k)] = true;
                                }
                                return true;
                            }
                        }
                        """),
                new Attempt(
                        "subarray-sum-equals-k",
                        "cpp",
                        14,
                        """
                        class Solution {
                        public:
                            int subarraySum(vector<int>& nums, int k) {
                                int count = 0;
                                for (int i = 0; i < nums.size(); i++) {
                                    int sum = 0;
                                    for (int j = i; j < nums.size(); j++) {
                                        sum += nums[j];
                                        if (sum == k) count++;
                                    }
                                }
                                return count;
                            }
                        };
                        """),
                new Attempt(
                        "coin-change",
                        "python",
                        12,
                        """
                        class Solution:
                            def coinChange(self, coins, amount):
                                def best(remaining):
                                    if remaining == 0:
                                        return 0
                                    if remaining < 0:
                                        return float('inf')
                                    return min(best(remaining - c) + 1 for c in coins)

                                answer = best(amount)
                                return -1 if answer == float('inf') else answer
                        """),
                new Attempt(
                        "daily-temperatures",
                        "python",
                        10,
                        """
                        class Solution:
                            def dailyTemperatures(self, temperatures):
                                n = len(temperatures)
                                answer = [0] * n
                                for i in range(n):
                                    for j in range(i + 1, n):
                                        if temperatures[j] > temperatures[i]:
                                            answer[i] = j - i
                                            break
                                return answer
                        """),
                new Attempt(
                        "kth-largest-element-in-an-array",
                        "python",
                        8,
                        """
                        class Solution:
                            def findKthLargest(self, nums, k):
                                nums.sort()
                                return nums[len(nums) - k]
                        """),
                new Attempt(
                        "two-sum",
                        "python",
                        6,
                        """
                        class Solution:
                            def twoSum(self, nums, target):
                                if not nums:
                                    return []
                                seen = {}
                                for i, x in enumerate(nums):
                                    need = target - x
                                    if need in seen:
                                        return [seen[need], i]
                                    seen[x] = i
                                return []
                        """),
                new Attempt(
                        "container-with-most-water",
                        "python",
                        4,
                        """
                        class Solution:
                            def maxArea(self, height):
                                if not height:
                                    return 0
                                left, right = 0, len(height) - 1
                                best = 0
                                while left < right:
                                    best = max(best, (right - left) * min(height[left], height[right]))
                                    if height[left] < height[right]:
                                        left += 1
                                    else:
                                        right -= 1
                                return best
                        """),
                new Attempt(
                        "number-of-islands",
                        "python",
                        2,
                        """
                        class Solution:
                            def numIslands(self, grid):
                                if not grid:
                                    return 0
                                rows, cols = len(grid), len(grid[0])
                                visited = set()

                                def dfs(r, c):
                                    if r < 0 or c < 0 or r >= rows or c >= cols:
                                        return
                                    if (r, c) in visited or grid[r][c] != '1':
                                        return
                                    visited.add((r, c))
                                    dfs(r + 1, c)
                                    dfs(r - 1, c)
                                    dfs(r, c + 1)
                                    dfs(r, c - 1)

                                islands = 0
                                for r in range(rows):
                                    for c in range(cols):
                                        if grid[r][c] == '1' and (r, c) not in visited:
                                            islands += 1
                                            dfs(r, c)
                                return islands
                        """));
    }
}
