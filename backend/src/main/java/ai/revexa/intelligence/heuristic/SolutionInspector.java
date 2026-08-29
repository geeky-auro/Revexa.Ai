package ai.revexa.intelligence.heuristic;

import ai.revexa.intelligence.dto.ApproachSummary;
import ai.revexa.intelligence.dto.ComplexityAnalysis;
import ai.revexa.intelligence.dto.Finding;
import ai.revexa.intelligence.dto.SolutionAnalysis;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Turns raw {@link CodeFacts} into the two things a reviewer actually says: "here is what your code
 * does" and "here is what will bite you".
 */
@Component
public class SolutionInspector {

    private final StaticCodeAnalyzer analyzer;

    public SolutionInspector(StaticCodeAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public CodeFacts facts(String code, String language) {
        return analyzer.analyze(code, language);
    }

    // ------------------------------------------------------------- approach

    public SolutionAnalysis analyze(CodeFacts facts, ConstraintReader.Constraints constraints, PatternMatch match) {
        ApproachSummary approach =
                new ApproachSummary(
                        name(facts),
                        intuition(facts, match),
                        explanation(facts),
                        steps(facts),
                        structureLabels(facts),
                        patternLabels(facts),
                        facts.language());
        return new SolutionAnalysis(approach, findings(facts, constraints, match));
    }

    public String name(CodeFacts facts) {
        if (facts.lines() == 0) {
            return "No code submitted";
        }
        if (facts.recursive() && facts.memoized()) {
            return "Top-down recursion with memoisation";
        }
        if (facts.recursive() && facts.selfCallSites() >= 2) {
            return "Branching recursion (full search)";
        }
        if (facts.recursive() && facts.has("graph")) {
            return "Depth-first traversal";
        }
        if (facts.has("dp") && facts.maxLoopDepth() >= 1) {
            return "Bottom-up dynamic programming";
        }
        if (facts.has("graph") && facts.has("queue")) {
            return "Breadth-first traversal";
        }
        if (facts.has("unionFind")) {
            return "Disjoint-set union";
        }
        if (facts.binarySearch()) {
            return facts.maxLoopDepth() >= 2 ? "Binary search inside a scan" : "Binary search";
        }
        if (facts.has("heap")) {
            return "Heap-based selection";
        }
        if (facts.has("slidingWindow")) {
            return "Sliding window";
        }
        if (facts.has("twoPointers")) {
            return facts.sorts() ? "Sort then two pointers" : "Two pointers";
        }
        if (facts.has("stack") && facts.maxLoopDepth() == 1) {
            return "Single pass with a stack";
        }
        if (facts.has("prefixSum")) {
            return "Prefix sums";
        }
        if ((facts.has("hashMap") || facts.has("hashSet")) && facts.maxLoopDepth() <= 1) {
            return "Single pass with a hash table";
        }
        if (facts.nestedScanOverSameCollection()) {
            return "Brute-force pair scan";
        }
        if (facts.maxLoopDepth() >= 3) {
            return "Triple nested scan";
        }
        if (facts.maxLoopDepth() == 2) {
            return "Nested scan";
        }
        if (facts.sorts()) {
            return "Sort then single pass";
        }
        if (facts.maxLoopDepth() == 1) {
            return "Single linear pass";
        }
        return "Direct computation";
    }

    private String intuition(CodeFacts facts, PatternMatch match) {
        if (facts.lines() == 0) {
            return "There is nothing to infer yet — paste a solution and the mentor will read it back to you.";
        }
        StringBuilder sb = new StringBuilder();
        if (facts.nestedScanOverSameCollection() || facts.maxLoopDepth() >= 2) {
            sb.append("You reached for the definition of the problem directly: check every candidate, ")
                    .append("and trust that correctness comes from exhaustiveness. That instinct is right — it is ")
                    .append("the honest first draft, and it gives you something to compare against.");
        } else if (facts.recursive() && !facts.memoized()) {
            sb.append("You framed the problem recursively: solve a smaller version, then combine. ")
                    .append("The decomposition is the hard part and you already have it.");
        } else if (facts.memoized()) {
            sb.append("You spotted that the recursion revisits states and cached them, which is exactly the ")
                    .append("move that turns a search into a dynamic program.");
        } else if (facts.has("hashMap") || facts.has("hashSet")) {
            sb.append("You traded memory for time: remember what you have already seen so the lookup you need ")
                    .append("later is instant instead of another scan.");
        } else if (facts.has("slidingWindow") || facts.has("twoPointers")) {
            sb.append("You noticed the answer is a contiguous range and that both ends only ever move forward, ")
                    .append("so no candidate has to be rebuilt from scratch.");
        } else if (facts.sorts()) {
            sb.append("You imposed order first so that later decisions become local — once things are sorted, ")
                    .append("a single sweep can make choices it could not make on unordered data.");
        } else {
            sb.append("You went straight at the computation with a single pass and no auxiliary structure.");
        }
        if (match != null && match.confident()) {
            sb.append(" The problem itself looks like a ")
                    .append(match.pattern().name().toLowerCase(Locale.ROOT))
                    .append(" question.");
        }
        return sb.toString();
    }

    private String explanation(CodeFacts facts) {
        if (facts.lines() == 0) {
            return "No code to explain yet.";
        }
        List<String> parts = new ArrayList<>();
        if (facts.maxLoopDepth() == 0 && !facts.recursive()) {
            parts.add("The code runs straight through with no loops");
        } else if (facts.maxLoopDepth() == 1) {
            parts.add("A single loop walks the input once");
        } else if (facts.maxLoopDepth() >= 2) {
            parts.add(
                    "There are " + facts.maxLoopDepth() + " levels of nested loops, so the inner body runs once per combination");
        }
        if (facts.recursive()) {
            parts.add(
                    facts.selfCallSites() >= 2
                            ? "the function calls itself more than once per frame, which branches the search"
                            : "the function calls itself to reduce the problem one step at a time");
        }
        if (facts.memoized()) {
            parts.add("results are cached so repeated states are answered instantly");
        }
        if (facts.sorts()) {
            parts.add("the input is sorted before the main work");
        }
        if (facts.has("hashMap")) {
            parts.add("a hash map holds state discovered along the way");
        }
        if (facts.has("hashSet")) {
            parts.add("a set records what has already been seen");
        }
        if (facts.has("heap")) {
            parts.add("a heap keeps the extreme element cheap to reach");
        }
        if (facts.binarySearch()) {
            parts.add("a halving search narrows the range");
        }
        if (facts.has("graph")) {
            parts.add("neighbours are expanded as a traversal");
        }
        return capitalise(String.join(", ", parts)) + ".";
    }

    private List<String> steps(CodeFacts facts) {
        List<String> steps = new ArrayList<>();
        if (facts.guardsEmptyInput()) {
            steps.add("Guard the empty or degenerate input");
        }
        if (facts.sorts()) {
            steps.add("Sort the input to impose order");
        }
        if (facts.has("hashMap") || facts.has("hashSet")) {
            steps.add("Build or update a lookup structure while scanning");
        }
        if (facts.maxLoopDepth() >= 1) {
            steps.add(
                    facts.maxLoopDepth() == 1
                            ? "Scan the input once, updating running state"
                            : "For each outer element, scan the inner range again");
        }
        if (facts.recursive()) {
            steps.add(facts.memoized() ? "Recurse on smaller states, reading the cache first" : "Recurse on smaller states");
        }
        if (facts.has("dp")) {
            steps.add("Fill the table in dependency order");
        }
        if (steps.isEmpty()) {
            steps.add("Compute the result directly");
        }
        steps.add("Return the accumulated answer");
        return steps;
    }

    private List<String> structureLabels(CodeFacts facts) {
        List<String> labels = new ArrayList<>();
        if (facts.has("hashMap")) {
            labels.add("Hash map");
        }
        if (facts.has("hashSet")) {
            labels.add("Hash set");
        }
        if (facts.has("heap")) {
            labels.add("Heap / priority queue");
        }
        if (facts.has("deque")) {
            labels.add("Deque");
        }
        if (facts.has("stack")) {
            labels.add("Stack");
        }
        if (facts.has("queue")) {
            labels.add("Queue");
        }
        if (facts.has("dp")) {
            labels.add("DP table");
        }
        if (facts.has("trie")) {
            labels.add("Trie");
        }
        if (facts.has("matrix")) {
            labels.add("2D grid");
        }
        if (facts.has("unionFind")) {
            labels.add("Disjoint set");
        }
        if (labels.isEmpty()) {
            labels.add("Plain arrays / scalars");
        }
        return labels;
    }

    private List<String> patternLabels(CodeFacts facts) {
        List<String> labels = new ArrayList<>();
        if (facts.has("slidingWindow")) {
            labels.add("Sliding window");
        }
        if (facts.has("twoPointers")) {
            labels.add("Two pointers");
        }
        if (facts.binarySearch()) {
            labels.add("Binary search");
        }
        if (facts.has("prefixSum")) {
            labels.add("Prefix sums");
        }
        if (facts.memoized()) {
            labels.add("Memoisation");
        }
        if (facts.has("dp")) {
            labels.add("Dynamic programming");
        }
        if (facts.has("graph")) {
            labels.add("Graph traversal");
        }
        if (facts.has("backtracking")) {
            labels.add("Backtracking");
        }
        if (facts.has("bitmask")) {
            labels.add("Bit manipulation");
        }
        if (facts.sorts()) {
            labels.add("Sorting");
        }
        if (facts.nestedScanOverSameCollection()) {
            labels.add("Brute force");
        }
        if (labels.isEmpty()) {
            labels.add("Direct iteration");
        }
        return labels;
    }

    // ------------------------------------------------------------- findings

    public List<Finding> findings(
            CodeFacts facts, ConstraintReader.Constraints constraints, PatternMatch match) {
        List<Finding> findings = new ArrayList<>();
        if (facts.lines() == 0) {
            return findings;
        }

        if (facts.recursive() && !facts.hasBaseCase()) {
            findings.add(
                    new Finding(
                            Finding.TYPE_CORRECTNESS,
                            Finding.SEVERITY_CRITICAL,
                            "Recursion without an obvious base case",
                            "The function calls itself but no early return guarding the smallest input was detected. "
                                    + "That is the classic route to a stack overflow on the first large test.",
                            "Add an explicit termination check as the first statement, before any recursive call.",
                            null));
        }

        if (facts.selfCallSites() >= 2 && !facts.memoized() && !facts.guardedByVisitedSet()) {
            findings.add(
                    new Finding(
                            Finding.TYPE_PERFORMANCE,
                            Finding.SEVERITY_HIGH,
                            "Branching recursion without memoisation",
                            "The function calls itself more than once per frame and nothing caches the results, so "
                                    + "identical subproblems are recomputed along every path. That is exponential work.",
                            "Key the results by the arguments that actually change and read the cache before recursing.",
                            null));
        }

        if (facts.hasMidpointOverflowRisk()) {
            findings.add(
                    new Finding(
                            Finding.TYPE_CORRECTNESS,
                            Finding.SEVERITY_HIGH,
                            "Midpoint can overflow",
                            "In " + facts.language() + ", (low + high) / 2 overflows once both bounds are near the "
                                    + "maximum integer, and the midpoint silently goes negative.",
                            "Use low + (high - low) / 2, which cannot overflow for non-negative bounds.",
                            facts.signalLines().get("binarySearch")));
        }

        if (!facts.guardsEmptyInput()) {
            findings.add(
                    new Finding(
                            Finding.TYPE_EDGE_CASE,
                            Finding.SEVERITY_MEDIUM,
                            "No guard for empty or null input",
                            "Nothing in the code checks for an empty collection or a null reference before indexing. "
                                    + "Judges love feeding an empty array as the first hidden test.",
                            "Return the identity answer for an empty input as the first line of the function.",
                            null));
        }

        if (facts.buildsStringByConcatInLoop()) {
            findings.add(
                    new Finding(
                            Finding.TYPE_PERFORMANCE,
                            Finding.SEVERITY_MEDIUM,
                            "String concatenation inside a loop",
                            "Strings are immutable in most of these languages, so each concatenation copies everything "
                                    + "built so far, turning a linear loop into quadratic work.",
                            "Collect the pieces in a list or builder and join once at the end.",
                            null));
        }

        String estimated = estimateTime(facts);
        if (!constraints.budget().isBlank() && Complexity.isWorse(estimated, constraints.budget())) {
            findings.add(
                    new Finding(
                            Finding.TYPE_PERFORMANCE,
                            Finding.SEVERITY_CRITICAL,
                            "Too slow for the stated constraints",
                            "The submission looks like " + estimated + " but " + constraints.note().toLowerCase(Locale.ROOT)
                                    + " This is the difference between a correct solution and a time-limit verdict.",
                            "Aim for " + constraints.budget() + " or better — the review's optimisation panel points at the missing observation.",
                            null));
        }

        if (facts.maxLoopDepth() >= 2
                && match != null
                && Complexity.isBetter(match.pattern().optimalTime(), estimated)) {
            findings.add(
                    new Finding(
                            Finding.TYPE_PERFORMANCE,
                            Finding.SEVERITY_HIGH,
                            "Repeated work between iterations",
                            "The inner loop re-derives information the outer loop already had. Problems of this shape "
                                    + "usually collapse to " + match.pattern().optimalTime() + ".",
                            match.pattern().direction(),
                            null));
        }

        if (facts.lines() > 120) {
            findings.add(
                    new Finding(
                            Finding.TYPE_READABILITY,
                            Finding.SEVERITY_LOW,
                            "Long single unit of code",
                            "At " + facts.lines() + " lines this is hard to hold in your head during an interview, and "
                                    + "harder to debug under time pressure.",
                            "Extract the inner step into a named helper so the main flow reads as prose.",
                            null));
        }

        if ("unknown".equals(facts.language())) {
            findings.add(
                    new Finding(
                            Finding.TYPE_READABILITY,
                            Finding.SEVERITY_LOW,
                            "Language could not be detected",
                            "The analyser could not confidently identify the language, so structural findings are more "
                                    + "approximate than usual.",
                            "Pick the language explicitly in the workspace so the analysis can specialise.",
                            null));
        }

        return findings;
    }

    // ----------------------------------------------------------- complexity

    public ComplexityAnalysis complexity(
            CodeFacts facts, ConstraintReader.Constraints constraints, PatternMatch match) {
        List<ComplexityAnalysis.Contributor> contributors = new ArrayList<>();
        String time = estimateTime(facts, contributors);
        String space = estimateSpace(facts, contributors);

        String optimalTime = match != null ? Complexity.normalize(match.pattern().optimalTime()) : time;
        String optimalSpace = match != null ? Complexity.normalize(match.pattern().optimalSpace()) : space;
        if (!constraints.budget().isBlank() && Complexity.isBetter(constraints.budget(), optimalTime)) {
            optimalTime = constraints.budget();
        }

        int confidence = confidence(facts, match);

        String timeExplanation = timeExplanation(facts, time, constraints);
        String spaceExplanation = spaceExplanation(facts, space);

        return new ComplexityAnalysis(
                time,
                space,
                timeExplanation,
                spaceExplanation,
                optimalTime,
                optimalSpace,
                !Complexity.isWorse(time, optimalTime),
                !Complexity.isWorse(space, optimalSpace),
                confidence,
                contributors);
    }

    public String estimateTime(CodeFacts facts) {
        return estimateTime(facts, new ArrayList<>());
    }

    private String estimateTime(CodeFacts facts, List<ComplexityAnalysis.Contributor> contributors) {
        if (facts.lines() == 0) {
            return Complexity.CONSTANT;
        }
        String time = Complexity.forLoopDepth(facts.maxLoopDepth());
        if (facts.maxLoopDepth() > 0) {
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            facts.maxLoopDepth() == 1 ? "Single pass over the input" : facts.maxLoopDepth() + " nested loops",
                            time,
                            "The innermost statement runs once for every combination of the enclosing loop variables."));
        }

        // A lone halving loop is logarithmic, not linear.
        if (facts.binarySearch() && facts.maxLoopDepth() <= 1 && facts.loopCount() <= 2) {
            time = Complexity.LOG;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Halving search", Complexity.LOG, "Each iteration discards half of the remaining range."));
        } else if (facts.binarySearch() && facts.maxLoopDepth() >= 2) {
            time = Complexity.LINEARITHMIC;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Binary search inside a scan",
                            Complexity.LINEARITHMIC,
                            "A logarithmic search runs once per element of the outer loop."));
        }

        if (facts.sorts()) {
            String withSort = Complexity.rank(time) < Complexity.rank(Complexity.LINEARITHMIC) ? Complexity.LINEARITHMIC : time;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Sorting step", Complexity.LINEARITHMIC, "A comparison sort costs n log n and dominates any linear pass."));
            time = withSort;
        }

        if (facts.recursive()) {
            if (facts.guardedByVisitedSet()) {
                // Each vertex is entered once, so the traversal is linear in the graph, not
                // exponential in the branching factor — the visited set is what makes that true.
                time = Complexity.LINEAR;
                contributors.add(
                        new ComplexityAnalysis.Contributor(
                                "Traversal with a visited set",
                                Complexity.LINEAR,
                                "Every vertex is entered at most once and every edge examined at most twice, giving O(V + E)."));
            } else if (facts.selfCallSites() >= 2 && !facts.memoized()) {
                time = Complexity.EXPONENTIAL;
                contributors.add(
                        new ComplexityAnalysis.Contributor(
                                "Branching recursion without a cache",
                                Complexity.EXPONENTIAL,
                                "Each frame spawns more than one child and nothing is reused, so the call tree doubles with depth."));
            } else if (facts.memoized()) {
                String memoised = facts.allocatesMatrix() ? Complexity.QUADRATIC : Complexity.LINEAR;
                if (Complexity.isWorse(memoised, time)) {
                    time = memoised;
                }
                contributors.add(
                        new ComplexityAnalysis.Contributor(
                                "Memoised states",
                                memoised,
                                "With caching the cost is the number of distinct states times the work per transition."));
            } else if (Complexity.rank(time) < Complexity.rank(Complexity.LINEAR)) {
                time = Complexity.LINEAR;
                contributors.add(
                        new ComplexityAnalysis.Contributor(
                                "Linear recursion", Complexity.LINEAR, "One recursive call per frame, descending once through the input."));
            }
        }

        if (facts.buildsStringByConcatInLoop() && Complexity.rank(time) < Complexity.rank(Complexity.QUADRATIC)) {
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "String rebuilt each iteration",
                            Complexity.QUADRATIC,
                            "Immutable string concatenation copies the accumulated prefix on every step."));
            time = Complexity.QUADRATIC;
        }

        if (contributors.isEmpty()) {
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Straight-line work", Complexity.CONSTANT, "No loops or recursion were detected."));
        }
        return time;
    }

    private String estimateSpace(CodeFacts facts, List<ComplexityAnalysis.Contributor> contributors) {
        String space = Complexity.CONSTANT;
        if (facts.allocatesMatrix()) {
            space = Complexity.QUADRATIC;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "2D table", Complexity.QUADRATIC, "A matrix keyed by two indices scales with the product of both."));
        } else if (facts.has("hashMap") || facts.has("hashSet") || facts.has("dp") || facts.allocatesAuxiliaryArray()) {
            space = Complexity.LINEAR;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Auxiliary structure", Complexity.LINEAR, "The map, set or array can grow to hold one entry per element."));
        } else if (facts.has("heap") || facts.has("deque") || facts.has("stack") || facts.has("queue")) {
            space = Complexity.LINEAR;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Pending-items container", Complexity.LINEAR, "In the worst case every element is held at once."));
        }
        if (facts.recursive() && Complexity.rank(space) < Complexity.rank(Complexity.LINEAR)) {
            space = Complexity.LINEAR;
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Call stack", Complexity.LINEAR, "Recursion depth counts toward space, even with no explicit allocation."));
        }
        final String resolved = space;
        if (contributors.stream().noneMatch(c -> c.complexity().equals(resolved))) {
            contributors.add(
                    new ComplexityAnalysis.Contributor(
                            "Scalars only", Complexity.CONSTANT, "Only a fixed number of variables are kept alive."));
        }
        return resolved;
    }

    private String timeExplanation(CodeFacts facts, String time, ConstraintReader.Constraints constraints) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estimated ").append(time).append(" — ").append(Complexity.describe(time)).append(". ");
        if (facts.guardedByVisitedSet()) {
            sb.append("The nested scan starts a traversal from each cell, but the visited set means every cell is ")
                    .append("entered exactly once overall, so the total work is linear in the size of the grid. ");
        } else if (facts.maxLoopDepth() >= 2) {
            sb.append("The dominant cost is ")
                    .append(facts.maxLoopDepth())
                    .append(" levels of loop nesting; everything outside them is noise by comparison. ");
        } else if (facts.recursive() && facts.selfCallSites() >= 2 && !facts.memoized()) {
            sb.append("The recursion branches without reusing results, so the work doubles with each extra element. ");
        } else if (facts.sorts()) {
            sb.append("The sort dominates: the linear passes around it disappear into the constant factor. ");
        }
        if (!constraints.note().isBlank()) {
            sb.append(constraints.note());
        }
        return sb.toString().strip();
    }

    private String spaceExplanation(CodeFacts facts, String space) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estimated ").append(space).append(" of auxiliary space. ");
        if (facts.allocatesMatrix()) {
            sb.append("A two-dimensional table is the largest allocation. ");
        } else if (facts.has("hashMap") || facts.has("hashSet")) {
            sb.append("The hash structure is the largest allocation and can hold every distinct element. ");
        } else if (facts.recursive()) {
            sb.append("Even with no allocations, the recursion depth occupies stack frames. ");
        } else {
            sb.append("Only a fixed set of scalars is kept alive. ");
        }
        sb.append("Input storage is excluded, as is conventional.");
        return sb.toString().strip();
    }

    private int confidence(CodeFacts facts, PatternMatch match) {
        int confidence = 88;
        if ("unknown".equals(facts.language())) {
            confidence -= 25;
        }
        if (facts.recursive()) {
            confidence -= 12;
        }
        if (facts.lines() < 5) {
            confidence -= 15;
        }
        if (facts.lines() > 150) {
            confidence -= 10;
        }
        if (match == null || !match.confident()) {
            confidence -= 8;
        }
        return Math.max(35, Math.min(95, confidence));
    }

    private String capitalise(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
