package ai.revexa.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import ai.revexa.intelligence.dto.ChatReply;
import ai.revexa.intelligence.dto.CodeReviewResult;
import ai.revexa.intelligence.dto.Finding;
import ai.revexa.intelligence.dto.GeneratedHint;
import ai.revexa.intelligence.dto.HintLevel;
import ai.revexa.intelligence.dto.SolutionComparison;
import ai.revexa.intelligence.heuristic.Complexity;
import ai.revexa.intelligence.pipeline.ReviewPipeline;
import ai.revexa.intelligence.pipeline.StageContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** End-to-end pipeline behaviour, running against the offline provider. */
@SpringBootTest
@ActiveProfiles("test")
class ReviewPipelineTest {

    private static final String PROBLEM =
            """
            Two Sum

            You are given an array of integers nums and an integer target. Return the indices of the two
            numbers that add up to target. Each input has exactly one valid answer.

            Constraints:
            2 <= nums.length <= 10^4
            -10^9 <= nums[i] <= 10^9
            """;

    private static final String BRUTE_FORCE =
            """
            def two_sum(nums, target):
                for i in range(len(nums)):
                    for j in range(i + 1, len(nums)):
                        if nums[i] + nums[j] == target:
                            return [i, j]
                return []
            """;

    private static final String OPTIMAL =
            """
            def two_sum(nums, target):
                if not nums:
                    return []
                seen = {}
                for i, x in enumerate(nums):
                    need = target - x
                    if need in seen:
                        return [seen[need], i]
                    seen[x] = i
                return []
            """;

    @Autowired private ReviewPipeline pipeline;

    private StageContext.Builder context() {
        return StageContext.builder().problemTitle("Two Sum").problemStatement(PROBLEM).language("python");
    }

    @Test
    @DisplayName("marks a quadratic solution as improvable and names the missing observation")
    void reviewsBruteForce() {
        CodeReviewResult result = pipeline.review(context().code(BRUTE_FORCE).build());

        assertThat(result.complexity().time()).isEqualTo(Complexity.QUADRATIC);
        assertThat(result.complexity().optimalTime()).isEqualTo(Complexity.LINEAR);
        assertThat(result.complexity().timeOptimal()).isFalse();
        assertThat(result.verdict()).isIn(CodeReviewResult.VERDICT_INEFFICIENT, CodeReviewResult.VERDICT_IMPROVABLE);
        assertThat(result.optimization().betterApproachExists()).isTrue();
        assertThat(result.optimization().keyInsight()).isNotBlank();
        assertThat(result.findings()).extracting(Finding::type).contains(Finding.TYPE_PERFORMANCE);
        assertThat(result.score()).isLessThan(80);
    }

    @Test
    @DisplayName("does not invent an improvement for an already-optimal solution")
    void reviewsOptimal() {
        CodeReviewResult result = pipeline.review(context().code(OPTIMAL).build());

        assertThat(result.complexity().time()).isEqualTo(Complexity.LINEAR);
        assertThat(result.complexity().timeOptimal()).isTrue();
        assertThat(result.verdict()).isEqualTo(CodeReviewResult.VERDICT_OPTIMAL);
        assertThat(result.optimization().betterApproachExists()).isFalse();
        assertThat(result.score()).isGreaterThan(80);
    }

    @Test
    @DisplayName("the optimal solution scores higher than the brute-force one")
    void ranksOptimalAboveBruteForce() {
        int brute = pipeline.review(context().code(BRUTE_FORCE).build()).score();
        int optimal = pipeline.review(context().code(OPTIMAL).build()).score();
        assertThat(optimal).isGreaterThan(brute);
    }

    @Test
    @DisplayName("early hint rungs contain no implementation")
    void earlyHintsDoNotSpoil() {
        for (int level = 1; level <= HintLevel.DIRECTION.order(); level++) {
            GeneratedHint hint = pipeline.hint(context().code(BRUTE_FORCE).hintLevel(level).build());
            assertThat(hint.level()).isEqualTo(level);
            assertThat(hint.spoiler()).isFalse();
            assertThat(hint.content()).doesNotContain("seen[x] = i");
        }
    }

    @Test
    @DisplayName("the pseudocode and solution rungs are marked as spoilers")
    void lateHintsAreMarkedSpoilers() {
        assertThat(pipeline.hint(context().hintLevel(HintLevel.PSEUDOCODE.order()).build()).spoiler()).isTrue();
        GeneratedHint solution = pipeline.hint(context().hintLevel(HintLevel.SOLUTION.order()).allowSpoilers(true).build());
        assertThat(solution.spoiler()).isTrue();
        assertThat(solution.lastLevel()).isTrue();
    }

    @Test
    @DisplayName("chat withholds the solution until the turn explicitly allows it")
    void chatWithholdsSolutionByDefault() {
        ChatReply guarded =
                pipeline.chat(context().code(BRUTE_FORCE).question("give me the full solution").allowSpoilers(false).build());
        assertThat(guarded.revealedSolution()).isFalse();
        assertThat(guarded.message()).contains("ask again");

        ChatReply revealed =
                pipeline.chat(context().code(BRUTE_FORCE).question("reveal the solution").allowSpoilers(true).build());
        assertThat(revealed.revealedSolution()).isTrue();
    }

    @Test
    @DisplayName("chat answers a complexity question with the measured cost")
    void chatAnswersComplexityQuestions() {
        ChatReply reply =
                pipeline.chat(context().code(BRUTE_FORCE).question("what is the time complexity of my code?").build());
        assertThat(reply.message()).contains("O(n^2)");
        assertThat(reply.revealedSolution()).isFalse();
    }

    @Test
    @DisplayName("comparison includes the optimal approach and withholds pseudocode by default")
    void comparesApproaches() {
        SolutionComparison comparison = pipeline.compare(context().code(BRUTE_FORCE).build());

        assertThat(comparison.userApproach().userApproach()).isTrue();
        assertThat(comparison.userApproach().optimal()).isFalse();
        assertThat(comparison.alternatives()).anyMatch(a -> a.optimal() && a.pseudocode() == null);
        assertThat(comparison.missingInsight()).isNotBlank();
        assertThat(comparison.pseudocodeRevealed()).isFalse();

        SolutionComparison revealed = pipeline.compare(context().code(BRUTE_FORCE).allowSpoilers(true).build());
        assertThat(revealed.alternatives()).anyMatch(a -> a.optimal() && a.pseudocode() != null);
    }

    @Test
    @DisplayName("grid DFS with a visited set is costed as linear, not exponential")
    void doesNotMisreadGraphTraversalAsExponential() {
        String islands =
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
                """;

        CodeReviewResult result =
                pipeline.review(
                        StageContext.builder()
                                .problemTitle("Number of Islands")
                                .problemStatement(
                                        "You are given an m x n grid of '1' land and '0' water. An island is a group of "
                                                + "land cells connected horizontally or vertically. Return the number of islands.\n\n"
                                                + "Constraints:\n1 <= m, n <= 300")
                                .language("python")
                                .code(islands)
                                .build());

        assertThat(result.complexity().time()).isEqualTo(Complexity.LINEAR);
        assertThat(result.complexity().timeOptimal()).isTrue();
        assertThat(result.findings())
                .noneMatch(finding -> finding.title().contains("Branching recursion"));
        assertThat(result.score()).isGreaterThan(70);
    }
}
