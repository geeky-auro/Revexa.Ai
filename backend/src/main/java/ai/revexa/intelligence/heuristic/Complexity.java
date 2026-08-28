package ai.revexa.intelligence.heuristic;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical complexity strings plus a total order over them, so "is this better?" is a comparison
 * rather than a guess.
 */
public final class Complexity {

    public static final String CONSTANT = "O(1)";
    public static final String LOG = "O(log n)";
    public static final String SQRT = "O(sqrt n)";
    public static final String LINEAR = "O(n)";
    public static final String LINEARITHMIC = "O(n log n)";
    public static final String QUADRATIC = "O(n^2)";
    public static final String CUBIC = "O(n^3)";
    public static final String QUARTIC = "O(n^4)";
    public static final String EXPONENTIAL = "O(2^n)";
    public static final String FACTORIAL = "O(n!)";

    private static final Map<String, Integer> RANKS = new LinkedHashMap<>();

    static {
        RANKS.put(CONSTANT, 0);
        RANKS.put(LOG, 10);
        RANKS.put(SQRT, 20);
        RANKS.put(LINEAR, 30);
        RANKS.put("O(n log k)", 35);
        RANKS.put(LINEARITHMIC, 40);
        RANKS.put("O(n log^2 n)", 45);
        RANKS.put(QUADRATIC, 50);
        RANKS.put("O(n^2 log n)", 55);
        RANKS.put(CUBIC, 60);
        RANKS.put(QUARTIC, 70);
        RANKS.put("O(n * 2^n)", 85);
        RANKS.put(EXPONENTIAL, 90);
        RANKS.put(FACTORIAL, 100);
    }

    private Complexity() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return LINEAR;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("*", "").replace("×", "");
        s = s.replace("o(", "").replace(")", "");
        s = s.replace("log(n)", "logn").replace("lg", "log");
        return switch (s) {
            case "1", "c", "constant" -> CONSTANT;
            case "logn", "log" -> LOG;
            case "sqrtn", "sqrt(n)", "n^0.5" -> SQRT;
            case "n", "m+n", "n+m", "v+e", "linear" -> LINEAR;
            case "nlogn", "nlog n" -> LINEARITHMIC;
            case "nlogk" -> "O(n log k)";
            case "nlog^2n", "nlog2n" -> "O(n log^2 n)";
            case "n^2", "n2", "nn", "n²" -> QUADRATIC;
            case "n^2logn" -> "O(n^2 log n)";
            case "n^3", "n3", "n³" -> CUBIC;
            case "n^4", "n4" -> QUARTIC;
            case "2^n", "2n" -> EXPONENTIAL;
            case "n2^n", "n*2^n" -> "O(n * 2^n)";
            case "n!" -> FACTORIAL;
            default -> "O(" + raw.trim().replaceAll("^[Oo]\\((.*)\\)$", "$1") + ")";
        };
    }

    public static int rank(String complexity) {
        return RANKS.getOrDefault(normalize(complexity), 30);
    }

    /** True when {@code candidate} is strictly cheaper than {@code baseline}. */
    public static boolean isBetter(String candidate, String baseline) {
        return rank(candidate) < rank(baseline);
    }

    public static boolean isWorse(String candidate, String baseline) {
        return rank(candidate) > rank(baseline);
    }

    public static String forLoopDepth(int depth) {
        return switch (Math.max(0, depth)) {
            case 0 -> CONSTANT;
            case 1 -> LINEAR;
            case 2 -> QUADRATIC;
            case 3 -> CUBIC;
            default -> QUARTIC;
        };
    }

    /** Human-friendly reading, e.g. "quadratic — doubles the input, quadruples the work". */
    public static String describe(String complexity) {
        return switch (normalize(complexity)) {
            case CONSTANT -> "constant — the input size does not change the work";
            case LOG -> "logarithmic — each step throws away half the search space";
            case SQRT -> "square-root — you touch only about sqrt(n) of the input";
            case LINEAR -> "linear — one pass over the input";
            case LINEARITHMIC -> "linearithmic — a sort or a divide-and-conquer split dominates";
            case QUADRATIC -> "quadratic — double the input and the work goes up 4x";
            case CUBIC -> "cubic — double the input and the work goes up 8x";
            case EXPONENTIAL -> "exponential — every extra element doubles the work";
            case FACTORIAL -> "factorial — this only survives on tiny inputs";
            default -> "estimated from the structure of the code";
        };
    }
}
