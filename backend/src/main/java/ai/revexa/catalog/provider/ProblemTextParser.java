package ai.revexa.catalog.provider;

import ai.revexa.catalog.domain.Difficulty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Pulls structure out of a pasted problem statement: title, difficulty, constraints and examples.
 *
 * <p>Shared by every provider, because a paste from LeetCode, a screenshot transcription and a
 * hand-written problem all end up in the same shape once parsed.
 */
@Component
public class ProblemTextParser {

    private static final Pattern EXAMPLE_HEADER = Pattern.compile("(?im)^\\s*example\\s*\\d*\\s*:?\\s*$");
    private static final Pattern INPUT_LINE = Pattern.compile("(?im)^\\s*input\\s*:?\\s*(.*)$");
    private static final Pattern OUTPUT_LINE = Pattern.compile("(?im)^\\s*output\\s*:?\\s*(.*)$");
    private static final Pattern EXPLANATION_LINE = Pattern.compile("(?im)^\\s*explanation\\s*:?\\s*(.*)$");
    private static final Pattern DIFFICULTY = Pattern.compile("(?i)\\b(easy|medium|hard)\\b");

    private static final Map<String, List<String>> TOPIC_KEYWORDS = new LinkedHashMap<>();

    static {
        TOPIC_KEYWORDS.put("Array", List.of("array", "nums[", "subarray", "elements"));
        TOPIC_KEYWORDS.put("String", List.of("string", "substring", "characters", "word"));
        TOPIC_KEYWORDS.put("Hash Table", List.of("frequency", "count of each", "duplicate", "occurrences"));
        TOPIC_KEYWORDS.put("Two Pointers", List.of("two pointers", "palindrome", "sorted array", "container"));
        TOPIC_KEYWORDS.put("Sliding Window", List.of("contiguous", "window", "longest substring", "at most k"));
        TOPIC_KEYWORDS.put("Binary Search", List.of("sorted", "log(n)", "rotated", "minimum such that"));
        TOPIC_KEYWORDS.put("Dynamic Programming", List.of("number of ways", "minimum cost", "maximum profit", "longest increasing"));
        TOPIC_KEYWORDS.put("Graph", List.of("graph", "island", "connected", "edges", "grid", "neighbors", "neighbours"));
        TOPIC_KEYWORDS.put("Tree", List.of("binary tree", "root", "node", "leaf", "bst"));
        TOPIC_KEYWORDS.put("Linked List", List.of("linked list", "listnode", "head of the list"));
        TOPIC_KEYWORDS.put("Heap", List.of("kth largest", "top k", "priority", "median"));
        TOPIC_KEYWORDS.put("Stack", List.of("parentheses", "next greater", "stack", "histogram"));
        TOPIC_KEYWORDS.put("Greedy", List.of("minimum number of", "intervals", "schedule", "jump"));
        TOPIC_KEYWORDS.put("Matrix", List.of("matrix", "grid", "rows and columns", "spiral"));
        TOPIC_KEYWORDS.put("Backtracking", List.of("all possible", "permutations", "combinations", "subsets"));
        TOPIC_KEYWORDS.put("Math", List.of("modulo", "prime", "gcd", "digits"));
    }

    private final ObjectMapper mapper;

    public ProblemTextParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public record Parsed(
            String title,
            String statement,
            String constraints,
            String examplesJson,
            Difficulty difficulty,
            List<String> topics) {}

    public Parsed parse(String rawText, String explicitTitle, String explicitDifficulty) {
        String text = rawText == null ? "" : rawText.replace("\r\n", "\n").strip();
        String title = explicitTitle != null && !explicitTitle.isBlank() ? explicitTitle.strip() : inferTitle(text);
        String constraints = extractConstraints(text);
        String examples = mapper.valueToTree(extractExamples(text)).toString();
        Difficulty difficulty =
                explicitDifficulty != null && !explicitDifficulty.isBlank()
                        ? parseDifficulty(explicitDifficulty)
                        : inferDifficulty(text);
        return new Parsed(title, text, constraints, examples, difficulty, inferTopics(text));
    }

    public String slugify(String title) {
        String slug =
                title.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9\\s-]", "")
                        .strip()
                        .replaceAll("\\s+", "-");
        return slug.isBlank() ? "problem" : slug.substring(0, Math.min(120, slug.length()));
    }

    private String inferTitle(String text) {
        for (String line : text.split("\n")) {
            String candidate = line.strip().replaceFirst("^#+\\s*", "").replaceFirst("^\\d+\\.\\s*", "");
            if (candidate.isBlank()) {
                continue;
            }
            if (candidate.length() <= 90 && !candidate.endsWith(".")) {
                return candidate;
            }
            return candidate.length() > 80 ? candidate.substring(0, 80).strip() + "…" : candidate;
        }
        return "Untitled problem";
    }

    private Difficulty parseDifficulty(String value) {
        try {
            return Difficulty.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Difficulty.UNKNOWN;
        }
    }

    private Difficulty inferDifficulty(String text) {
        String head = text.length() > 400 ? text.substring(0, 400) : text;
        Matcher matcher = DIFFICULTY.matcher(head);
        if (matcher.find()) {
            return parseDifficulty(matcher.group(1));
        }
        return Difficulty.UNKNOWN;
    }

    private String extractConstraints(String text) {
        int index = text.toLowerCase(Locale.ROOT).indexOf("constraints");
        if (index < 0) {
            return "";
        }
        String tail = text.substring(index);
        int stop = tail.toLowerCase(Locale.ROOT).indexOf("\nfollow up");
        if (stop > 0) {
            tail = tail.substring(0, stop);
        }
        return tail.strip();
    }

    /** Each block starting at "Example n:" becomes {input, output, explanation}. */
    public List<Map<String, String>> extractExamples(String text) {
        List<Map<String, String>> examples = new ArrayList<>();
        Matcher headers = EXAMPLE_HEADER.matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (headers.find()) {
            starts.add(headers.start());
        }
        if (starts.isEmpty()) {
            return examples;
        }
        starts.add(text.length());
        for (int i = 0; i < starts.size() - 1; i++) {
            String block = text.substring(starts.get(i), starts.get(i + 1));
            Map<String, String> example = new LinkedHashMap<>();
            Matcher input = INPUT_LINE.matcher(block);
            Matcher output = OUTPUT_LINE.matcher(block);
            Matcher explanation = EXPLANATION_LINE.matcher(block);
            if (input.find()) {
                example.put("input", input.group(1).strip());
            }
            if (output.find()) {
                example.put("output", output.group(1).strip());
            }
            if (explanation.find()) {
                example.put("explanation", explanation.group(1).strip());
            }
            if (!example.isEmpty()) {
                examples.add(example);
            }
            if (examples.size() >= 5) {
                break;
            }
        }
        return examples;
    }

    public List<String> inferTopics(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> topics = new ArrayList<>();
        TOPIC_KEYWORDS.forEach(
                (topic, keywords) -> {
                    if (keywords.stream().anyMatch(lower::contains)) {
                        topics.add(topic);
                    }
                });
        return topics.stream().limit(5).toList();
    }
}
