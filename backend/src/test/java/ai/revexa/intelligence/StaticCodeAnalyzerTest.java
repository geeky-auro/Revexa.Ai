package ai.revexa.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import ai.revexa.intelligence.heuristic.CodeFacts;
import ai.revexa.intelligence.heuristic.StaticCodeAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StaticCodeAnalyzerTest {

    private final StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer();

    @Test
    @DisplayName("counts nested loop depth in an indentation-scoped language")
    void detectsPythonNesting() {
        CodeFacts facts =
                analyzer.analyze(
                        """
                        def two_sum(nums, target):
                            for i in range(len(nums)):
                                for j in range(i + 1, len(nums)):
                                    if nums[i] + nums[j] == target:
                                        return [i, j]
                            return []
                        """,
                        null);

        assertThat(facts.language()).isEqualTo("python");
        assertThat(facts.maxLoopDepth()).isEqualTo(2);
        assertThat(facts.nestedScanOverSameCollection()).isTrue();
        assertThat(facts.recursive()).isFalse();
    }

    @Test
    @DisplayName("counts nested loop depth in a brace-scoped language")
    void detectsBraceNesting() {
        CodeFacts facts =
                analyzer.analyze(
                        """
                        class Solution {
                            public int[] twoSum(int[] nums, int target) {
                                for (int i = 0; i < nums.length; i++) {
                                    for (int j = i + 1; j < nums.length; j++) {
                                        if (nums[i] + nums[j] == target) {
                                            return new int[]{i, j};
                                        }
                                    }
                                }
                                return new int[0];
                            }
                        }
                        """,
                        "java");

        assertThat(facts.maxLoopDepth()).isEqualTo(2);
        assertThat(facts.nestedScanOverSameCollection()).isTrue();
        // The returned array is the answer, not auxiliary storage.
        assertThat(facts.allocatesAuxiliaryArray()).isFalse();
    }

    @Test
    @DisplayName("recognises a single pass with a hash map as depth 1")
    void detectsSinglePass() {
        CodeFacts facts =
                analyzer.analyze(
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
                        """,
                        "python");

        assertThat(facts.maxLoopDepth()).isEqualTo(1);
        assertThat(facts.has("hashMap")).isTrue();
        assertThat(facts.guardsEmptyInput()).isTrue();
    }

    @Test
    @DisplayName("spots branching recursion and the absence of a cache")
    void detectsBranchingRecursion() {
        CodeFacts facts =
                analyzer.analyze(
                        """
                        def fib(n):
                            if n < 2:
                                return n
                            return fib(n - 1) + fib(n - 2)
                        """,
                        "python");

        assertThat(facts.recursive()).isTrue();
        assertThat(facts.selfCallSites()).isGreaterThanOrEqualTo(2);
        assertThat(facts.memoized()).isFalse();
        assertThat(facts.hasBaseCase()).isTrue();
    }

    @Test
    @DisplayName("flags the overflow-prone midpoint only in fixed-width languages")
    void detectsMidpointOverflow() {
        String java =
                """
                int search(int[] a, int t) {
                    int low = 0, high = a.length - 1;
                    while (low <= high) {
                        int mid = (low + high) / 2;
                        if (a[mid] == t) return mid;
                        if (a[mid] < t) low = mid + 1; else high = mid - 1;
                    }
                    return -1;
                }
                """;
        assertThat(analyzer.analyze(java, "java").hasMidpointOverflowRisk()).isTrue();

        String python =
                """
                def search(a, t):
                    low, high = 0, len(a) - 1
                    while low <= high:
                        mid = (low + high) / 2
                        if a[mid] == t:
                            return mid
                    return -1
                """;
        assertThat(analyzer.analyze(python, "python").hasMidpointOverflowRisk()).isFalse();
    }

    @Test
    @DisplayName("ignores loop keywords that only appear in comments or string literals")
    void ignoresCommentsAndStrings() {
        CodeFacts facts =
                analyzer.analyze(
                        """
                        # for each item, for every other item, compare them
                        def solve(nums):
                            note = "for x in nums: for y in nums: pass"
                            return len(nums)
                        """,
                        "python");

        assertThat(facts.maxLoopDepth()).isZero();
    }

    @Test
    @DisplayName("detects the language when none is declared")
    void detectsLanguage() {
        assertThat(analyzer.detectLanguage("#include <vector>\nusing namespace std;")).isEqualTo("cpp");
        assertThat(analyzer.detectLanguage("public class Solution { public static void main(String[] a) {} }"))
                .isEqualTo("java");
        assertThat(analyzer.detectLanguage("const f = (x) => x + 1;\nconsole.log(f(1));")).isEqualTo("javascript");
    }
}
