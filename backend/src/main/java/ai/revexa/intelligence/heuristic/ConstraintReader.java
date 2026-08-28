package ai.revexa.intelligence.heuristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Reads the constraint block of a problem statement.
 *
 * <p>Constraint magnitudes are the most reliable signal there is about the intended complexity: an
 * upper bound of 10^5 rules out quadratic work, while a bound of 20 quietly invites it.
 */
@Component
public class ConstraintReader {

    private static final Pattern POWER = Pattern.compile("10\\s*\\^\\s*(\\d+)|1e(\\d+)|10\\*\\*(\\d+)");
    private static final Pattern PLAIN_NUMBER = Pattern.compile("(\\d[\\d,_]{2,})");
    /** The right-hand side of an inequality is the bound, however few digits it has. */
    private static final Pattern UPPER_BOUND = Pattern.compile("(?:<=|≤|<)\\s*([\\d,_]+)");
    private static final Pattern SIZE_TERM =
            Pattern.compile("(?i)\\b(?:\\w+\\.(?:length|size)|length|size|\\bn\\b|\\bm\\b|\\bk\\b|\\bh\\b|amount|nodes|piles\\.length)\\b");
    private static final Pattern VALUE_TERM = Pattern.compile("\\w+\\[[a-z]\\]|\\[i\\]|\\[j\\]");
    private static final Pattern MULTIPLIER = Pattern.compile("(\\d+)\\s*\\*\\s*10\\s*\\^");
    private static final Pattern CONSTRAINT_LINE =
            Pattern.compile("(?m)^\\s*[-*•]?\\s*(?:\\d\\s*<=|.*\\blength\\b|.*\\bn\\b\\s*<=|.*<=.*).*$");

    /** Everything the reader could infer, with {@code maxN} = 0 meaning "not stated". */
    public record Constraints(List<String> lines, long maxN, String budget, String note) {}

    public Constraints read(String statement, String explicitConstraints) {
        String text = (statement == null ? "" : statement) + "\n" + (explicitConstraints == null ? "" : explicitConstraints);
        List<String> lines = extractLines(text);
        long maxN = largestBound(text);
        String budget = budgetFor(maxN);
        String note = noteFor(maxN, budget);
        return new Constraints(lines, maxN, budget, note);
    }

    private List<String> extractLines(String text) {
        List<String> lines = new ArrayList<>();
        boolean inConstraintBlock = false;
        for (String raw : text.split("\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("constraint")) {
                inConstraintBlock = true;
                continue;
            }
            if (inConstraintBlock && (lower.startsWith("example") || lower.startsWith("follow up") || lower.startsWith("note:"))) {
                inConstraintBlock = false;
            }
            boolean looksLikeConstraint =
                    line.contains("<=") || line.contains("≤") || line.contains(">=") || line.contains("≥");
            if ((inConstraintBlock || looksLikeConstraint) && CONSTRAINT_LINE.matcher(line).matches()) {
                lines.add(line.replaceFirst("^[-*•]\\s*", ""));
            }
            if (lines.size() >= 8) {
                break;
            }
        }
        return lines;
    }

    /**
     * The bound that matters is the one on <em>input size</em>, not on the values.
     *
     * <p>"-10^9 &lt;= nums[i] &lt;= 10^9" says nothing about how long the loop runs, while
     * "nums.length &lt;= 10^4" says everything. So size-bearing lines are read first; only if none
     * exist does the reader fall back to the largest number anywhere in the text.
     */
    private long largestBound(String text) {
        long sizeBound = 0;
        for (String line : text.split("\n")) {
            if (SIZE_TERM.matcher(line).find() && !VALUE_TERM.matcher(line).find()) {
                sizeBound = Math.max(sizeBound, largestNumberIn(line));
            }
        }
        return sizeBound > 0 ? sizeBound : largestNumberIn(text);
    }

    private long largestNumberIn(String text) {
        long max = 0;
        Matcher power = POWER.matcher(text);
        while (power.find()) {
            String digits =
                    power.group(1) != null ? power.group(1) : power.group(2) != null ? power.group(2) : power.group(3);
            int exponent = Integer.parseInt(digits);
            if (exponent <= 18) {
                long value = (long) Math.pow(10, exponent);
                // Pick up a leading multiplier, e.g. "5 * 10^4".
                Matcher multiplier = MULTIPLIER.matcher(text);
                while (multiplier.find()) {
                    value = Math.max(value, Long.parseLong(multiplier.group(1)) * (long) Math.pow(10, exponent));
                }
                max = Math.max(max, value);
            }
        }
        max = Math.max(max, largestMatch(PLAIN_NUMBER.matcher(text)));
        max = Math.max(max, largestMatch(UPPER_BOUND.matcher(text)));
        return max;
    }

    private long largestMatch(Matcher matcher) {
        long max = 0;
        while (matcher.find()) {
            try {
                long value = Long.parseLong(matcher.group(1).replace(",", "").replace("_", ""));
                if (value <= 1_000_000_000_000L) {
                    max = Math.max(max, value);
                }
            } catch (NumberFormatException ignored) {
                // Not a usable bound.
            }
        }
        return max;
    }

    /** The slowest complexity that still fits in a typical one-second judge limit. */
    public String budgetFor(long maxN) {
        if (maxN <= 0) {
            return "";
        }
        if (maxN <= 25) {
            return Complexity.EXPONENTIAL;
        }
        if (maxN <= 500) {
            return Complexity.CUBIC;
        }
        if (maxN <= 5_000) {
            return Complexity.QUADRATIC;
        }
        if (maxN <= 1_000_000) {
            return Complexity.LINEARITHMIC;
        }
        return Complexity.LINEAR;
    }

    private String noteFor(long maxN, String budget) {
        if (maxN <= 0) {
            return "No explicit bound was stated, so the target complexity is inferred from the problem shape rather than the constraints.";
        }
        String pretty = pretty(maxN);
        return "The largest bound in the statement is about " + pretty
                + ", so anything slower than " + budget + " is likely to time out.";
    }

    public String pretty(long value) {
        if (value >= 1_000_000_000L) {
            return (value / 1_000_000_000L) + "e9";
        }
        if (value >= 1_000_000L) {
            return (value / 1_000_000L) + "e6";
        }
        if (value >= 1_000L) {
            return (value / 1_000L) + "e3";
        }
        return String.valueOf(value);
    }
}
