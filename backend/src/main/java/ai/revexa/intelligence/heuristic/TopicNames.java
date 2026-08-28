package ai.revexa.intelligence.heuristic;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One canonical spelling per topic.
 *
 * <p>The same idea reaches the progress dashboard under several labels — the catalogue says "Two
 * Pointers", the code inspector says "Two pointers", a pasted statement says "two-pointer". Without
 * canonicalisation the mastery breakdown silently splits one topic into three.
 */
public final class TopicNames {

    private static final Map<String, String> CANONICAL =
            Map.ofEntries(
                    Map.entry("two pointers", "Two Pointers"),
                    Map.entry("two pointer", "Two Pointers"),
                    Map.entry("sliding window", "Sliding Window"),
                    Map.entry("binary search", "Binary Search"),
                    Map.entry("prefix sums", "Prefix Sum"),
                    Map.entry("prefix sum", "Prefix Sum"),
                    Map.entry("hash table", "Hash Table"),
                    Map.entry("hash map", "Hash Table"),
                    Map.entry("hashmap", "Hash Table"),
                    Map.entry("dynamic programming", "Dynamic Programming"),
                    Map.entry("dp", "Dynamic Programming"),
                    Map.entry("dp table", "Dynamic Programming"),
                    Map.entry("memoisation", "Memoization"),
                    Map.entry("memoization", "Memoization"),
                    Map.entry("graph traversal", "Graph"),
                    Map.entry("bfs", "Graph"),
                    Map.entry("dfs", "Graph"),
                    Map.entry("monotonic stack", "Monotonic Stack"),
                    Map.entry("priority queue", "Heap"),
                    Map.entry("heap / priority queue", "Heap"),
                    Map.entry("bit manipulation", "Bit Manipulation"),
                    Map.entry("backtracking", "Backtracking"),
                    Map.entry("greedy", "Greedy"),
                    Map.entry("sorting", "Sorting"),
                    Map.entry("recursion", "Recursion"),
                    Map.entry("counting", "Counting"),
                    Map.entry("string", "String"),
                    Map.entry("array", "Array"),
                    Map.entry("matrix", "Matrix"),
                    Map.entry("2d grid", "Matrix"),
                    Map.entry("tree", "Tree"),
                    Map.entry("linked list", "Linked List"),
                    Map.entry("trie", "Trie"),
                    Map.entry("math", "Math"),
                    Map.entry("simulation", "Simulation"),
                    Map.entry("problem solving", "Problem Solving"));

    /**
     * Labels that describe the shape of a submission rather than a topic worth tracking mastery in.
     * Useful in a review panel, meaningless on a "weak topics" chart.
     */
    private static final Set<String> NOT_TOPICS =
            Set.of("direct iteration", "brute force", "plain arrays / scalars", "stack", "queue", "deque", "disjoint set");

    private TopicNames() {}

    public static String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String key = raw.strip().toLowerCase(Locale.ROOT);
        String mapped = CANONICAL.get(key);
        if (mapped != null) {
            return mapped;
        }
        return Arrays.stream(key.split("\\s+"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(raw);
    }

    public static boolean isTrackable(String raw) {
        return raw != null && !raw.isBlank() && !NOT_TOPICS.contains(raw.strip().toLowerCase(Locale.ROOT));
    }

    public static List<String> canonicalise(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        raw.stream().filter(TopicNames::isTrackable).map(TopicNames::canonical).forEach(seen::add);
        return List.copyOf(seen);
    }
}
