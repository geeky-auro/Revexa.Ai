package ai.revexa.intelligence.heuristic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * A deliberately small, language-tolerant source analyser.
 *
 * <p>It is not a compiler front end: it strips comments and string literals, then reads structure
 * (loop nesting, recursion, the data structures in play) well enough to reason about cost. Every
 * downstream claim built on it is reported as an estimate with a confidence score.
 */
@Component
public class StaticCodeAnalyzer {

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final Map<String, String[]> STRUCTURE_SIGNALS =
            Map.ofEntries(
                    Map.entry(
                            "hashMap",
                            new String[] {
                                "hashmap", "unordered_map", "map<", "dict(", "defaultdict", "counter(",
                                "new map(", "{}", "map[string]", "hashtable", ".get(", "map<int"
                            }),
                    Map.entry(
                            "hashSet",
                            new String[] {"hashset", "unordered_set", "set(", "set<", "new set(", "seen"}),
                    Map.entry(
                            "sorting",
                            new String[] {
                                "sort(", "sorted(", ".sort()", "arrays.sort", "collections.sort", "sort_by",
                                ".sort(", "sort.slice"
                            }),
                    Map.entry("heap", new String[] {"heapq", "priorityqueue", "priority_queue", "heappush", "binaryheap"}),
                    Map.entry("deque", new String[] {"deque", "arraydeque", "linkedlist", "collections.deque"}),
                    Map.entry("stack", new String[] {"stack<", "stack()", "new stack", ".pop()", ".push("}),
                    Map.entry("queue", new String[] {"queue<", "queue()", "new queue", ".shift()", "poll()"}),
                    Map.entry(
                            "binarySearch",
                            new String[] {
                                "bisect", "binarysearch", "lower_bound", "upper_bound", "binary_search",
                                "lo + (hi", "left + (right", "(lo+hi)", "(left+right)", "mid ="
                            }),
                    Map.entry("dp", new String[] {"dp[", "dp =", "dp=", "memo", "lru_cache", "@cache", "tabulation"}),
                    Map.entry(
                            "graph",
                            new String[] {
                                "adj", "graph[", "visited", "neighbou", "neighbor", "dfs(", "bfs(", "edges"
                            }),
                    Map.entry("unionFind", new String[] {"union(", "find(", "parent[", "rank[", "dsu", "disjoint"}),
                    Map.entry("prefixSum", new String[] {"prefix", "cumulative", "presum", "running_sum", "runningsum"}),
                    Map.entry(
                            "slidingWindow",
                            new String[] {"window", "windowstart", "left = 0", "left=0", "start = 0", "start=0"}),
                    Map.entry("twoPointers", new String[] {"left", "right", "lo", "hi", "slow", "fast"}),
                    Map.entry("trie", new String[] {"trie", "children[26]", "trienode"}),
                    Map.entry("bitmask", new String[] {"<<", ">>", "&1", "xor", "^ ", "bitcount", "popcount"}),
                    Map.entry("matrix", new String[] {"[i][j]", "grid[", "matrix[", "board["}),
                    Map.entry("stringBuilder", new String[] {"stringbuilder", "''.join", "\"\".join", "stringbuffer"}),
                    Map.entry("backtracking", new String[] {"backtrack", "permut", "combinat", "subset", ".remove(path"}));

    public CodeFacts analyze(String rawCode, String declaredLanguage) {
        String code = rawCode == null ? "" : rawCode;
        String language = declaredLanguage == null || declaredLanguage.isBlank()
                ? detectLanguage(code)
                : declaredLanguage.toLowerCase(Locale.ROOT);
        String cleaned = stripCommentsAndStrings(code, language);
        String lower = cleaned.toLowerCase(Locale.ROOT);

        boolean indentScoped = isIndentScoped(language);
        LoopProfile loops = indentScoped ? analyzeIndentLoops(cleaned) : analyzeBraceLoops(cleaned);
        loops = loops.mergeComprehensions(countComprehensionDepth(cleaned));

        List<String> functions = functionNames(cleaned, language);
        int selfCalls = countSelfCalls(cleaned, functions);
        boolean recursive = selfCalls > 0;

        Set<String> structures = new LinkedHashSet<>();
        Map<String, Integer> signalLines = new HashMap<>();
        String[] lines = cleaned.split("\n", -1);
        STRUCTURE_SIGNALS.forEach(
                (name, needles) -> {
                    for (String needle : needles) {
                        int idx = lower.indexOf(needle);
                        if (idx >= 0) {
                            structures.add(name);
                            signalLines.putIfAbsent(name, lineOfOffset(lower, idx));
                            return;
                        }
                    }
                });

        // "twoPointers" is a weak signal on its own: require a while loop that moves both ends.
        if (structures.contains("twoPointers") && !movesTwoPointers(lower)) {
            structures.remove("twoPointers");
        }
        if (structures.contains("slidingWindow") && !(loops.maxDepth() >= 1 && (lower.contains("window") || movesTwoPointers(lower)))) {
            structures.remove("slidingWindow");
        }
        if (structures.contains("binarySearch") && !looksLikeBinarySearch(lower)) {
            structures.remove("binarySearch");
        }

        boolean memoized =
                lower.contains("memo")
                        || lower.contains("lru_cache")
                        || lower.contains("@cache")
                        || lower.contains("functools.cache")
                        || (recursive && structures.contains("dp"));

        // A visited set only rescues the cost when the recursion actually branches — that is the
        // shape of a traversal. One self-call plus a variable named `seen` is not a graph.
        boolean guardedByVisitedSet = selfCalls >= 2 && marksVisitedNodes(lower);

        Set<String> signals = new LinkedHashSet<>();
        if (guardedByVisitedSet) {
            signals.add("visitedGuard");
        }
        if (recursive) {
            signals.add("recursion");
        }
        if (memoized) {
            signals.add("memoization");
        }
        if (selfCalls >= 2) {
            signals.add("branchingRecursion");
        }
        if (loops.maxDepth() >= 2) {
            signals.add("nestedLoops");
        }

        return new CodeFacts(
                language,
                countLines(code),
                loops.maxDepth(),
                loops.count(),
                recursive,
                selfCalls,
                memoized,
                guardedByVisitedSet,
                structures.contains("sorting"),
                structures.contains("binarySearch"),
                loops.maxDepth() >= 2 && nestedScanOverSameCollection(cleaned),
                structures,
                signals,
                functions,
                signalLines,
                guardsEmptyInput(lower),
                hasMidpointOverflowRisk(lower, language),
                hasBaseCase(lower, recursive),
                allocatesAuxiliaryArray(lower),
                allocatesMatrix(lower),
                buildsStringByConcatInLoop(lines, language),
                cleaned);
    }

    // ---------------------------------------------------------------- language

    public String detectLanguage(String code) {
        String lower = code.toLowerCase(Locale.ROOT);
        if (lower.contains("#include") || lower.contains("using namespace std") || lower.contains("vector<")) {
            return "cpp";
        }
        if (lower.contains("public class") || lower.contains("system.out.print") || lower.contains("public static void main")) {
            return "java";
        }
        if (lower.contains("def ") && (lower.contains("self") || lower.contains(":\n") || lower.contains("elif"))) {
            return "python";
        }
        if (lower.contains("func ") && lower.contains("package ")) {
            return "go";
        }
        if (lower.contains("fn ") && lower.contains("let mut")) {
            return "rust";
        }
        if (lower.contains("fun ") && lower.contains("val ")) {
            return "kotlin";
        }
        if (lower.contains("console.log") || lower.contains("=>") || lower.contains("const ") || lower.contains("function ")) {
            return lower.contains(": number") || lower.contains("interface ") ? "typescript" : "javascript";
        }
        if (lower.contains("def ")) {
            return "python";
        }
        return "unknown";
    }

    private boolean isIndentScoped(String language) {
        return "python".equals(language);
    }

    // ------------------------------------------------------------- sanitising

    /** Removes comments and string bodies so keyword scanning cannot be fooled by prose or literals. */
    public String stripCommentsAndStrings(String code, String language) {
        boolean hash = "python".equals(language) || "ruby".equals(language) || "unknown".equals(language);
        StringBuilder out = new StringBuilder(code.length());
        int i = 0;
        int n = code.length();
        while (i < n) {
            char c = code.charAt(i);
            char next = i + 1 < n ? code.charAt(i + 1) : '\0';

            if (c == '/' && next == '/') {
                while (i < n && code.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && next == '*') {
                i += 2;
                while (i + 1 < n && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) {
                    if (code.charAt(i) == '\n') {
                        out.append('\n');
                    }
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }
            if (hash && c == '#') {
                while (i < n && code.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if ((c == '"' || c == '\'') && i + 2 < n && code.charAt(i + 1) == c && code.charAt(i + 2) == c) {
                String fence = String.valueOf(c).repeat(3);
                int end = code.indexOf(fence, i + 3);
                String body = end < 0 ? code.substring(i + 3) : code.substring(i + 3, end);
                out.append(" ").append("\n".repeat((int) body.chars().filter(ch -> ch == '\n').count()));
                i = end < 0 ? n : end + 3;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                char quote = c;
                i++;
                out.append("\"\"");
                while (i < n && code.charAt(i) != quote) {
                    if (code.charAt(i) == '\\') {
                        i++;
                    }
                    if (i < n && code.charAt(i) == '\n') {
                        out.append('\n');
                    }
                    i++;
                }
                i++;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    // ----------------------------------------------------------------- loops

    private record LoopProfile(int maxDepth, int count) {
        LoopProfile mergeComprehensions(int comprehensionDepth) {
            return new LoopProfile(
                    Math.max(maxDepth, comprehensionDepth), count + (comprehensionDepth > 0 ? comprehensionDepth : 0));
        }
    }

    private LoopProfile analyzeBraceLoops(String code) {
        Deque<Boolean> blocks = new ArrayDeque<>();
        int max = 0;
        int loops = 0;
        int current = 0;
        boolean pendingLoop = false;
        int parenDepth = 0;
        boolean headerClosed = false;

        Matcher matcher = WORD.matcher(code);
        List<int[]> words = new ArrayList<>();
        while (matcher.find()) {
            String w = matcher.group();
            if (w.equals("for") || w.equals("while") || w.equals("foreach")) {
                words.add(new int[] {matcher.start(), 1});
            } else if (w.equals("do")) {
                words.add(new int[] {matcher.start(), 1});
            }
        }
        Set<Integer> loopStarts = new LinkedHashSet<>();
        for (int[] w : words) {
            loopStarts.add(w[0]);
        }

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (loopStarts.contains(i)) {
                pendingLoop = true;
                headerClosed = false;
                loops++;
            }
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
                if (pendingLoop && parenDepth <= 0) {
                    headerClosed = true;
                }
            } else if (c == '{') {
                boolean isLoop = pendingLoop;
                blocks.push(isLoop);
                if (isLoop) {
                    current++;
                    max = Math.max(max, current);
                }
                pendingLoop = false;
            } else if (c == '}') {
                if (!blocks.isEmpty() && Boolean.TRUE.equals(blocks.pop())) {
                    current--;
                }
            } else if (c == ';' && pendingLoop && headerClosed) {
                // Brace-less single statement loop body.
                max = Math.max(max, current + 1);
                pendingLoop = false;
            }
        }
        return new LoopProfile(max, loops);
    }

    private LoopProfile analyzeIndentLoops(String code) {
        Deque<int[]> stack = new ArrayDeque<>(); // {indent, isLoop}
        int max = 0;
        int loops = 0;
        for (String rawLine : code.split("\n", -1)) {
            String line = rawLine.stripTrailing();
            if (line.isBlank()) {
                continue;
            }
            int indent = line.length() - line.stripLeading().length();
            String body = line.strip();
            while (!stack.isEmpty() && stack.peek()[0] >= indent) {
                stack.pop();
            }
            boolean isLoop = body.startsWith("for ") || body.startsWith("while ") || body.startsWith("for(") || body.startsWith("while(");
            if (isLoop) {
                loops++;
                int depth = (int) stack.stream().filter(e -> e[1] == 1).count() + 1;
                max = Math.max(max, depth);
            }
            if (body.endsWith(":")) {
                stack.push(new int[] {indent, isLoop ? 1 : 0});
            }
        }
        return new LoopProfile(max, loops);
    }

    /** Comprehensions such as {@code [x for a in rows for x in a]} nest without indenting. */
    private int countComprehensionDepth(String code) {
        int max = 0;
        for (String line : code.split("\n", -1)) {
            if (!line.contains("for ")) {
                continue;
            }
            if (!(line.contains("[") || line.contains("{") || line.contains("("))) {
                continue;
            }
            int count = line.split("\\bfor\\b", -1).length - 1;
            if (count > 1) {
                max = Math.max(max, count);
            }
        }
        return max;
    }

    // ------------------------------------------------------------- recursion

    private List<String> functionNames(String code, String language) {
        List<String> names = new ArrayList<>();
        List<Pattern> patterns =
                List.of(
                        Pattern.compile("\\bdef\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\("),
                        Pattern.compile("\\bfunction\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\("),
                        Pattern.compile("\\bfunc\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\("),
                        Pattern.compile("\\bfn\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\("),
                        Pattern.compile("\\bfun\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\("),
                        Pattern.compile(
                                "(?:public|private|protected|static|final|\\s)+[\\w<>\\[\\],\\s]+\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\([^;]*\\)\\s*\\{"),
                        Pattern.compile("\\bconst\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(?:async\\s*)?\\("));
        for (Pattern p : patterns) {
            Matcher m = p.matcher(code);
            while (m.find()) {
                String name = m.group(1);
                if (!names.contains(name) && !isKeyword(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private boolean isKeyword(String name) {
        return Set.of("if", "for", "while", "switch", "return", "catch", "new", "class", "main", "print")
                .contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Counts calls a function makes to itself.
     *
     * <p>The subtlety is locating the declaration rather than the first mention: in Java and C++ a
     * helper is routinely called above the line that defines it, and treating that first mention as
     * the declaration makes every helper look recursive.
     */
    private int countSelfCalls(String code, List<String> functions) {
        int total = 0;
        for (String name : functions) {
            int declarationIndex = declarationIndex(code, name);
            if (declarationIndex < 0) {
                continue;
            }
            String body = code.substring(declarationIndex + name.length());
            Matcher call = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(").matcher(body);
            int calls = 0;
            while (call.find()) {
                calls++;
            }
            total += calls;
        }
        return total;
    }

    /** Where the function is <em>defined</em>: a {@code def}/{@code function} keyword, or a signature with a body. */
    private int declarationIndex(String code, String name) {
        Matcher keyword =
                Pattern.compile("\\b(?:def|function|func|fn|fun)\\s+" + Pattern.quote(name) + "\\s*\\(").matcher(code);
        if (keyword.find()) {
            return keyword.start();
        }
        // Brace languages: a signature immediately followed by an opening body brace. A call site such
        // as `if (allUnique(s, i, j)) {` cannot match, because a `)` sits between the arguments and the brace.
        Matcher signature =
                Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\([^;{)]*\\)\\s*(?:const\\s*)?\\{").matcher(code);
        return signature.find() ? signature.start() : -1;
    }

    // ---------------------------------------------------------------- probes

    /**
     * A traversal that records where it has been cannot revisit a node, so its recursion is bounded
     * by the size of the graph rather than by branching factor — the difference between O(V + E) and
     * O(2^n). In-place marking (overwriting a grid cell) counts just as much as an explicit set.
     */
    private boolean marksVisitedNodes(String lower) {
        return lower.contains("visited")
                || lower.contains("seen")
                || lower.contains("= \"\"")  && lower.contains("grid[")
                || lower.contains("grid[") && (lower.contains("= 0") || lower.contains("='0'") || lower.contains("= '0'"))
                || lower.contains("board[") && lower.contains("#");
    }

    private boolean movesTwoPointers(String lower) {
        boolean incrementsLeft = lower.contains("left++") || lower.contains("left += 1") || lower.contains("left+=1") || lower.contains("lo++") || lower.contains("slow =");
        boolean decrementsRight = lower.contains("right--") || lower.contains("right -= 1") || lower.contains("right-=1") || lower.contains("hi--") || lower.contains("fast =");
        return incrementsLeft && decrementsRight;
    }

    private boolean looksLikeBinarySearch(String lower) {
        boolean hasMid = lower.contains("mid");
        boolean halves = lower.contains("/ 2") || lower.contains("/2") || lower.contains(">> 1") || lower.contains(">>1");
        return (hasMid && halves) || lower.contains("bisect") || lower.contains("binarysearch") || lower.contains("lower_bound");
    }

    /**
     * True when two different loops iterate the same collection — the signature of a brute-force pair
     * scan, and a stronger signal than nesting depth alone (nested loops over <em>different</em>
     * collections are often unavoidable).
     */
    private boolean nestedScanOverSameCollection(String code) {
        // Brace languages: the whole for-header, including the condition after the first semicolon.
        Matcher braces = Pattern.compile("for\\s*\\([^)]*?\\b(\\w+)\\.(?:length|size)\\b").matcher(code);
        if (repeatsACollection(braces)) {
            return true;
        }
        // Python: range(...) may carry an offset before the len() call, as in range(i + 1, len(nums)).
        Matcher python = Pattern.compile("for\\s+\\w+\\s+in\\s+range\\([^)]*len\\(\\s*(\\w+)").matcher(code);
        if (repeatsACollection(python)) {
            return true;
        }
        // Python: direct iteration such as "for a in nums:" twice over.
        Matcher direct = Pattern.compile("for\\s+\\w+\\s+in\\s+(\\w+)\\s*:").matcher(code);
        return repeatsACollection(direct);
    }

    private boolean repeatsACollection(Matcher matcher) {
        Map<String, Integer> counts = new HashMap<>();
        while (matcher.find()) {
            counts.merge(matcher.group(1), 1, Integer::sum);
        }
        return counts.values().stream().anyMatch(v -> v >= 2);
    }

    private boolean guardsEmptyInput(String lower) {
        return lower.contains("isempty")
                || lower.contains("== null")
                || lower.contains("is none")
                || lower.contains("not nums")
                || lower.contains("len(") && lower.contains("== 0")
                || lower.contains(".length == 0")
                || lower.contains(".size() == 0")
                || lower.contains("!nums")
                || lower.contains(".empty()");
    }

    private boolean hasMidpointOverflowRisk(String lower, String language) {
        boolean fixedWidth = List.of("java", "cpp", "c", "csharp", "go", "rust", "kotlin").contains(language);
        boolean naiveMid =
                lower.contains("(low + high) / 2")
                        || lower.contains("(lo + hi) / 2")
                        || lower.contains("(left + right) / 2")
                        || lower.contains("(l + r) / 2")
                        || lower.contains("(low+high)/2")
                        || lower.contains("(left+right)/2");
        return fixedWidth && naiveMid;
    }

    private boolean hasBaseCase(String lower, boolean recursive) {
        if (!recursive) {
            return true;
        }
        return lower.contains("return") && (lower.contains("if") || lower.contains("elif"));
    }

    /**
     * Only counts storage the algorithm keeps. {@code return new int[]{i, j}} is the answer being
     * handed back, not auxiliary space, and counting it would wrongly mark an O(1) solution as O(n).
     */
    private boolean allocatesAuxiliaryArray(String lower) {
        return containsOutsideReturn(lower, "new int[")
                || containsOutsideReturn(lower, "new arraylist")
                || containsOutsideReturn(lower, "vector<")
                || containsOutsideReturn(lower, "= []")
                || containsOutsideReturn(lower, "[0] *")
                || containsOutsideReturn(lower, "[0]*")
                || containsOutsideReturn(lower, "make([]");
    }

    private boolean containsOutsideReturn(String lower, String needle) {
        for (String line : lower.split("\n")) {
            if (line.contains(needle) && !line.strip().startsWith("return")) {
                return true;
            }
        }
        return false;
    }

    private boolean allocatesMatrix(String lower) {
        return lower.contains("[][]")
                || lower.contains("new int[") && lower.contains("][")
                || lower.contains("vector<vector")
                || lower.contains("for _ in range") && lower.contains("[0] *")
                || lower.contains("dp[i][j]");
    }

    private boolean buildsStringByConcatInLoop(String[] lines, String language) {
        boolean inLoop = false;
        for (String line : lines) {
            String l = line.strip();
            if (l.startsWith("for") || l.startsWith("while")) {
                inLoop = true;
            }
            if (inLoop && (l.contains("+= \"") || l.matches(".*\\w+\\s*\\+=\\s*\\w+\\s*;?") && l.contains("str"))) {
                return true;
            }
        }
        return false;
    }

    private int countLines(String code) {
        return (int) Arrays.stream(code.split("\n", -1)).filter(l -> !l.isBlank()).count();
    }

    private int lineOfOffset(String text, int offset) {
        int line = 1;
        for (int i = 0; i < Math.min(offset, text.length()); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
