package ai.revexa.intelligence.heuristic;

import ai.revexa.intelligence.dto.ApproachOption;
import ai.revexa.intelligence.dto.ChatReply;
import ai.revexa.intelligence.dto.ComplexityAnalysis;
import ai.revexa.intelligence.dto.GeneratedHint;
import ai.revexa.intelligence.dto.HintLevel;
import ai.revexa.intelligence.dto.OptimizationInsight;
import ai.revexa.intelligence.dto.ProblemUnderstanding;
import ai.revexa.intelligence.dto.SolutionAnalysis;
import ai.revexa.intelligence.dto.SolutionComparison;
import ai.revexa.intelligence.llm.LlmMessage;
import ai.revexa.intelligence.pipeline.StageContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The offline mentor.
 *
 * <p>Every stage the hosted models serve has a deterministic counterpart here, built from static
 * analysis plus the pattern knowledge base. That is what makes the product demonstrable with no API
 * key, gives the hosted providers a fallback when they fail, and gives the test suite something
 * stable to assert against.
 */
@Component
public class HeuristicEngine {

    private final StaticCodeAnalyzer analyzer;
    private final SolutionInspector inspector;
    private final ConstraintReader constraintReader;
    private final PatternCatalog catalog;

    public HeuristicEngine(
            StaticCodeAnalyzer analyzer,
            SolutionInspector inspector,
            ConstraintReader constraintReader,
            PatternCatalog catalog) {
        this.analyzer = analyzer;
        this.inspector = inspector;
        this.constraintReader = constraintReader;
        this.catalog = catalog;
    }

    // ------------------------------------------------------------- context

    /** The derived bundle every stage works from, computed once per request. */
    public record Insight(
            CodeFacts facts, ConstraintReader.Constraints constraints, PatternMatch match, List<PatternMatch> ranked) {}

    public Insight inspect(StageContext context) {
        CodeFacts facts = analyzer.analyze(context.code(), context.language());
        ConstraintReader.Constraints constraints =
                constraintReader.read(context.safeStatement(), context.constraintsText());
        List<PatternMatch> ranked = catalog.rank(searchText(context), context.topics(), facts);
        PatternMatch match = ranked.isEmpty() ? new PatternMatch(catalog.fallback(), 0, false) : ranked.get(0);
        return new Insight(facts, constraints, match, ranked);
    }

    private String searchText(StageContext context) {
        return (context.problemTitle() == null ? "" : context.problemTitle())
                + "\n"
                + context.safeStatement()
                + "\n"
                + (context.constraintsText() == null ? "" : context.constraintsText());
    }

    // ------------------------------------------------- stage: understanding

    public ProblemUnderstanding understand(StageContext context) {
        Insight insight = inspect(context);
        AlgorithmPattern pattern = insight.match().pattern();
        String statement = context.safeStatement();

        return new ProblemUnderstanding(
                restate(context, insight),
                inputs(statement),
                output(statement),
                insight.constraints().lines().isEmpty()
                        ? List.of("No explicit constraints were given in the statement.")
                        : insight.constraints().lines(),
                misreadings(insight, statement),
                topics(context, insight),
                difficulty(context, insight),
                pattern.questionsFor("clarify"));
    }

    private String restate(StageContext context, Insight insight) {
        String statement = context.safeStatement().strip();
        if (statement.isEmpty()) {
            return "No problem statement was provided yet.";
        }
        String firstSentences = firstSentences(statement, 2);
        StringBuilder sb = new StringBuilder();
        sb.append("In plain terms: ").append(firstSentences);
        if (!firstSentences.endsWith(".")) {
            sb.append('.');
        }
        if (insight.match().confident()) {
            sb.append(" Structurally this is a ")
                    .append(insight.match().pattern().name().toLowerCase(Locale.ROOT))
                    .append(" question, which tells you which tools are worth reaching for.");
        }
        if (insight.constraints().maxN() > 0) {
            sb.append(" ").append(insight.constraints().note());
        }
        return sb.toString();
    }

    private List<String> inputs(String statement) {
        List<String> inputs = new ArrayList<>();
        for (String line : statement.split("\n")) {
            String l = line.strip();
            String lower = l.toLowerCase(Locale.ROOT);
            if (lower.startsWith("input") || lower.startsWith("given ") || lower.startsWith("you are given")) {
                inputs.add(l.length() > 200 ? l.substring(0, 200) + "…" : l);
            }
            if (inputs.size() >= 4) {
                break;
            }
        }
        if (inputs.isEmpty()) {
            String first = firstSentences(statement, 1);
            inputs.add(first.isBlank() ? "Not stated explicitly — confirm before you code." : first);
        }
        return inputs;
    }

    private String output(String statement) {
        for (String line : statement.split("\n")) {
            String lower = line.strip().toLowerCase(Locale.ROOT);
            if (lower.startsWith("output") || lower.startsWith("return ") || lower.contains("return the")) {
                String l = line.strip();
                return l.length() > 240 ? l.substring(0, 240) + "…" : l;
            }
        }
        return "Not stated explicitly — decide exactly what you return before you write the loop.";
    }

    private List<String> misreadings(Insight insight, String statement) {
        String lower = statement.toLowerCase(Locale.ROOT);
        List<String> notes = new ArrayList<>();
        if (lower.contains("-10") || lower.contains("negative")) {
            notes.add("Values can be negative, which quietly breaks sliding-window and greedy arguments that assume growth.");
        }
        if (lower.contains("duplicate") || lower.contains("may repeat") || lower.contains("distinct")) {
            notes.add(lower.contains("distinct")
                    ? "Elements are stated to be distinct — that assumption is doing real work, so lean on it."
                    : "Duplicates are allowed, so any set-based shortcut needs a second look.");
        }
        if (lower.contains("in-place") || lower.contains("in place")) {
            notes.add("The statement asks for an in-place transform, so an auxiliary copy would not be accepted.");
        }
        if (lower.contains("sorted")) {
            notes.add("The input is already sorted — that is free information most brute-force solutions ignore.");
        }
        if (lower.contains("contiguous") || lower.contains("subarray")) {
            notes.add("Subarrays are contiguous and subsequences are not; the two words lead to completely different algorithms.");
        }
        if (lower.contains("0-indexed") || lower.contains("1-indexed")) {
            notes.add("The indexing base is stated explicitly — off-by-one errors here are the most common wrong answer.");
        }
        if (lower.contains("modulo") || lower.contains("10^9 + 7") || lower.contains("1000000007")) {
            notes.add("Results must be taken modulo a large prime; apply it during accumulation, not only at the end.");
        }
        if (insight.constraints().maxN() > 100_000) {
            notes.add("The input can be large enough that even an O(n log n) solution needs fast I/O in some languages.");
        }
        if (notes.isEmpty()) {
            notes.add("Re-read the exact wording of what is returned — index versus value is the classic trap.");
        }
        return notes;
    }

    private List<String> topics(StageContext context, Insight insight) {
        Set<String> topics = new LinkedHashSet<>(context.topics());
        topics.addAll(insight.match().pattern().topics());
        insight.ranked().stream()
                .skip(1)
                .limit(1)
                .filter(PatternMatch::confident)
                .forEach(m -> topics.addAll(m.pattern().topics()));
        if (topics.isEmpty()) {
            topics.addAll(insight.match().pattern().topics());
        }
        return new ArrayList<>(topics);
    }

    private String difficulty(StageContext context, Insight insight) {
        if (context.difficulty() != null && !context.difficulty().isBlank()) {
            return context.difficulty();
        }
        String optimal = insight.match().pattern().optimalTime();
        if (insight.match().pattern().id().equals("dynamic-programming")
                || insight.match().pattern().id().equals("backtracking")) {
            return "MEDIUM_HARD";
        }
        return Complexity.rank(optimal) <= Complexity.rank(Complexity.LINEAR) ? "EASY_MEDIUM" : "MEDIUM";
    }

    // ------------------------------------------------------ stage: analysis

    public SolutionAnalysis analyzeSolution(StageContext context) {
        Insight insight = inspect(context);
        return inspector.analyze(insight.facts(), insight.constraints(), insight.match());
    }

    public ComplexityAnalysis complexity(StageContext context) {
        Insight insight = inspect(context);
        return inspector.complexity(insight.facts(), insight.constraints(), insight.match());
    }

    // -------------------------------------------------- stage: optimisation

    public OptimizationInsight optimize(StageContext context) {
        Insight insight = inspect(context);
        AlgorithmPattern pattern = insight.match().pattern();
        ComplexityAnalysis complexity = inspector.complexity(insight.facts(), insight.constraints(), insight.match());

        boolean better =
                Complexity.isBetter(complexity.optimalTime(), complexity.time())
                        || Complexity.isBetter(complexity.optimalSpace(), complexity.space());

        List<String> nudges = new ArrayList<>();
        if (better) {
            nudges.add(pattern.rung("observe"));
            nudges.add(pattern.rung("concept"));
            if (!insight.constraints().note().isBlank()) {
                nudges.add(insight.constraints().note());
            }
        } else {
            nudges.add(
                    "Your complexity already matches the best known bound for this shape of problem — the remaining wins are constant factors and clarity.");
            nudges.add(
                    "Try re-deriving why the bound is tight: being able to argue that no faster algorithm exists is exactly what an interviewer probes for.");
        }

        String keyInsight =
                better
                        ? pattern.keyInsight()
                        : "You already found the observation this problem is built around: "
                                + lowerFirst(pattern.keyInsight());

        return new OptimizationInsight(
                better,
                keyInsight,
                better ? pattern.direction() : "Tighten the implementation rather than the asymptotics.",
                nudges.stream().filter(n -> n != null && !n.isBlank()).toList(),
                complexity.optimalTime(),
                complexity.optimalSpace(),
                pattern.name());
    }

    // --------------------------------------------------------- stage: hints

    public GeneratedHint hint(StageContext context) {
        Insight insight = inspect(context);
        AlgorithmPattern pattern = insight.match().pattern();
        HintLevel level = HintLevel.ofOrder(Math.max(1, context.hintLevel()));

        String key =
                switch (level) {
                    case CLARIFY -> "clarify";
                    case OBSERVE -> "observe";
                    case CONCEPT -> "concept";
                    case DIRECTION -> "direction";
                    case PSEUDOCODE -> "pseudocode";
                    case SOLUTION -> "solution";
                };

        String body = pattern.rung(key);
        if (body == null || body.isBlank()) {
            body = catalog.fallback().rung(key);
        }

        StringBuilder content = new StringBuilder(body);
        if (level == HintLevel.CLARIFY && insight.constraints().maxN() > 0) {
            content.append("\n\n").append(insight.constraints().note());
        }
        if (level == HintLevel.OBSERVE && context.hasCode() && insight.facts().maxLoopDepth() >= 2) {
            content
                    .append("\n\nIn your own code the inner loop runs ")
                    .append(insight.facts().maxLoopDepth())
                    .append(" levels deep — that nesting is where the repeated work lives.");
        }
        if (level == HintLevel.DIRECTION) {
            content.append("\n\nTarget: ").append(pattern.optimalTime()).append(" time, ")
                    .append(pattern.optimalSpace()).append(" space.");
        }

        return new GeneratedHint(
                level.order(),
                level.name(),
                level.intent(),
                level.title(),
                content.toString(),
                pattern.questionsFor(key),
                level.isSpoiler(),
                level == HintLevel.SOLUTION);
    }

    // ---------------------------------------------------------- stage: chat

    public ChatReply chat(StageContext context) {
        Insight insight = inspect(context);
        AlgorithmPattern pattern = insight.match().pattern();
        String question = context.question() == null ? "" : context.question().strip();
        String lower = question.toLowerCase(Locale.ROOT);
        int turn = (int) context.history().stream().filter(m -> "user".equals(m.role())).count();

        if (question.isBlank()) {
            return reply(
                    "Ask me anything about this problem — what it is really asking, whether your idea holds up, or why "
                            + "an approach is slower than it looks. I will nudge before I answer.",
                    pattern.questionsFor("clarify"),
                    1,
                    false);
        }

        boolean wantsSolution = asksForSolution(lower);
        if (wantsSolution && !context.allowSpoilers()) {
            return reply(
                    "I can absolutely give you the full solution — but you are close enough that it would cost you the "
                            + "insight. Here is the observation it turns on:\n\n"
                            + pattern.keyInsight()
                            + "\n\nIf you still want the complete implementation, ask again with \"reveal the solution\" "
                            + "and I will show it in full.",
                    pattern.questionsFor("observe"),
                    HintLevel.CONCEPT.order(),
                    false);
        }
        if (wantsSolution) {
            return reply(
                    "Here is the complete approach, as you asked.\n\n**" + pattern.name() + "** — "
                            + pattern.rung("solution")
                            + "\n\nPseudocode:\n\n```\n" + pattern.rung("pseudocode") + "\n```\n\n"
                            + "Now the useful part: cover it up and re-derive why "
                            + lowerFirst(pattern.keyInsight()),
                    List.of("Can you re-derive the key step without looking?", "What breaks if the input is empty?"),
                    HintLevel.SOLUTION.order(),
                    true);
        }

        if (mentions(lower, "complexity", "time complex", "space complex", "big o", "how fast", "tle", "time limit")) {
            ComplexityAnalysis complexity = inspector.complexity(insight.facts(), insight.constraints(), insight.match());
            String message =
                    context.hasCode()
                            ? "Your submission looks like **" + complexity.time() + " time** and **"
                                    + complexity.space() + " space**.\n\n" + complexity.timeExplanation()
                                    + "\n\n" + complexity.spaceExplanation()
                                    + (complexity.timeOptimal()
                                            ? "\n\nThat matches the best known bound for this shape."
                                            : "\n\nThe achievable bound is " + complexity.optimalTime()
                                                    + " — the gap is one observation wide.")
                            : "Paste your code and I will cost it line by line. For this shape of problem the target is "
                                    + pattern.optimalTime() + " time and " + pattern.optimalSpace() + " space.";
            return reply(message, pattern.questionsFor("concept"), HintLevel.CONCEPT.order(), false);
        }

        if (mentions(lower, "explain the problem", "don't understand", "dont understand", "do not understand",
                "confused", "what does it mean", "what is it asking", "what is this asking", "asking for",
                "rephrase", "simplify", "in simple terms", "explain this problem", "restate")) {
            ProblemUnderstanding understanding = understand(context);
            return reply(
                    understanding.restatement()
                            + "\n\n**Watch out for:** "
                            + String.join(" ", understanding.easyToMisread())
                            + "\n\nBefore any code, answer these for yourself:",
                    understanding.clarifyingQuestions(),
                    HintLevel.CLARIFY.order(),
                    false);
        }

        if (mentions(lower, "right track", "is my approach", "does this work", "correct approach", "am i close", "review my idea")) {
            ComplexityAnalysis complexity = inspector.complexity(insight.facts(), insight.constraints(), insight.match());
            String verdict =
                    complexity.timeOptimal()
                            ? "Yes — your approach is at the right complexity. Now make sure the edges hold: empty input, single element, and duplicates."
                            : "Partly. What you have is correct-shaped but costs " + complexity.time()
                                    + ", and this problem is solvable in " + complexity.optimalTime()
                                    + ". You are one observation away rather than one rewrite away.";
            return reply(
                    verdict + "\n\n" + pattern.rung("observe"),
                    pattern.questionsFor("observe"),
                    HintLevel.OBSERVE.order(),
                    false);
        }

        if (mentions(lower, "why", "how come", "reason")) {
            return reply(
                    pattern.rung("concept")
                            + "\n\nApplied here: "
                            + lowerFirst(pattern.keyInsight()),
                    pattern.questionsFor("concept"),
                    HintLevel.CONCEPT.order(),
                    false);
        }

        if (mentions(lower, "hint", "stuck", "nudge", "help me start", "where do i start")) {
            int level = Math.min(HintLevel.DIRECTION.order(), Math.max(HintLevel.OBSERVE.order(), turn + 1));
            GeneratedHint hint =
                    hint(StageContext.builder()
                            .problemTitle(context.problemTitle())
                            .problemStatement(context.problemStatement())
                            .constraintsText(context.constraintsText())
                            .topics(context.topics())
                            .code(context.code())
                            .language(context.language())
                            .hintLevel(level)
                            .build());
            return reply(
                    "**" + hint.title() + "** — " + hint.content(),
                    hint.socraticQuestions(),
                    level,
                    false);
        }

        if (mentions(lower, "edge case", "test case", "what should i test", "failing")) {
            List<String> cases = edgeCases(insight);
            return reply(
                    "Run these before you submit:\n\n" + cases.stream().map(c -> "- " + c).reduce((a, b) -> a + "\n" + b).orElse(""),
                    List.of("Which of these does your current code get wrong?"),
                    HintLevel.OBSERVE.order(),
                    false);
        }

        // Default: acknowledge, then hand back a sharper question.
        return reply(
                "Good question. Here is the thread I would pull on: "
                        + lowerFirst(pattern.rung("observe"))
                        + "\n\nTell me what you notice and I will take you one step further.",
                pattern.questionsFor("observe"),
                HintLevel.OBSERVE.order(),
                false);
    }

    private List<String> edgeCases(Insight insight) {
        List<String> cases = new ArrayList<>();
        cases.add("Empty input — does it return the identity answer rather than crashing?");
        cases.add("Single element — do both pointers or both loop bounds still make sense?");
        cases.add("All elements identical — do duplicates break your set or map logic?");
        if (insight.facts().sorts()) {
            cases.add("Already sorted and reverse sorted input — does the sort assumption still hold?");
        }
        if (insight.facts().binarySearch()) {
            cases.add("Target smaller than everything and larger than everything — do the bounds terminate?");
        }
        if (insight.facts().recursive()) {
            cases.add("Maximum-size input — does the recursion depth stay inside the stack limit?");
        }
        if (insight.constraints().maxN() > 100_000) {
            cases.add("The largest allowed input — measure it, do not assume it fits the time limit.");
        }
        cases.add("Negative values or zero, if the constraints allow them.");
        return cases;
    }

    private ChatReply reply(String message, List<String> questions, int spoilerLevel, boolean revealed) {
        return new ChatReply(
                message,
                questions == null || questions.isEmpty()
                        ? List.of("What have you tried so far?")
                        : questions,
                spoilerLevel,
                revealed,
                "heuristic",
                "revexa-mentor-rules-v1");
    }

    /**
     * Whether the turn is asking for the finished answer at all. Whether that ask is <em>granted</em>
     * is a separate decision, carried on {@link StageContext#allowSpoilers()} — this method only
     * routes, it never unlocks.
     */
    private boolean asksForSolution(String lower) {
        return mentions(
                lower,
                "reveal the solution",
                "show me the solution",
                "give me the solution",
                "full solution",
                "just tell me",
                "show the code",
                "write the code",
                "give me the code",
                "complete solution",
                "final answer");
    }

    private boolean mentions(String haystack, String... needles) {
        return Arrays.stream(needles).anyMatch(haystack::contains);
    }

    // ---------------------------------------------------- stage: comparison

    public SolutionComparison compare(StageContext context) {
        Insight insight = inspect(context);
        AlgorithmPattern pattern = insight.match().pattern();
        ComplexityAnalysis complexity = inspector.complexity(insight.facts(), insight.constraints(), insight.match());
        boolean reveal = context.allowSpoilers();

        ApproachOption user =
                new ApproachOption(
                        "user",
                        inspector.name(insight.facts()),
                        context.hasCode()
                                ? "What you submitted, as the analyser reads it."
                                : "No code submitted yet — compare the alternatives below.",
                        complexity.time(),
                        complexity.space(),
                        userPros(insight, complexity),
                        userCons(insight, complexity),
                        complexity.timeOptimal()
                                ? "This is the approach to reach for in an interview: it is optimal and it reads clearly."
                                : "Reasonable as a first draft, and worth writing to establish correctness before optimising.",
                        complexity.timeOptimal()
                                ? pattern.keyInsight()
                                : "The observation you have not used yet: " + lowerFirst(pattern.keyInsight()),
                        true,
                        complexity.timeOptimal(),
                        null);

        List<ApproachOption> alternatives = new ArrayList<>();
        alternatives.add(
                new ApproachOption(
                        "optimal",
                        pattern.name(),
                        pattern.direction(),
                        Complexity.normalize(pattern.optimalTime()),
                        Complexity.normalize(pattern.optimalSpace()),
                        List.of(
                                "Best known asymptotic cost for this problem shape",
                                "Single pass over the data in most formulations",
                                "The version an interviewer is listening for"),
                        List.of(
                                "Requires spotting the key observation first",
                                "Usually trades extra memory for the speed"),
                        "Whenever the constraints rule out the brute-force version — which is most of the time.",
                        pattern.keyInsight(),
                        false,
                        true,
                        reveal ? pattern.rung("pseudocode") : null));

        for (AlgorithmPattern.Alternative alternative : pattern.alternatives()) {
            alternatives.add(
                    new ApproachOption(
                            alternative.id(),
                            alternative.name(),
                            alternative.summary(),
                            Complexity.normalize(alternative.time()),
                            Complexity.normalize(alternative.space()),
                            alternative.pros(),
                            alternative.cons(),
                            alternative.whenToPrefer(),
                            alternative.keyInsight(),
                            false,
                            !Complexity.isWorse(alternative.time(), pattern.optimalTime()),
                            null));
        }

        String missing =
                complexity.timeOptimal()
                        ? "Nothing major is missing — you already used the central observation. The remaining differences are trade-offs, not mistakes."
                        : pattern.keyInsight();

        String tradeoffs =
                "The choice here is the usual one: memory against time, and clarity against constant factors. "
                        + pattern.name()
                        + " buys "
                        + Complexity.normalize(pattern.optimalTime())
                        + " time by keeping "
                        + Complexity.normalize(pattern.optimalSpace())
                        + " of state, while the simpler versions keep less state and pay for it on every element.";

        String recommendation =
                complexity.timeOptimal()
                        ? "Keep your approach. Spend the remaining time on edge cases and on being able to justify why the bound is tight."
                        : "Work toward " + pattern.name().toLowerCase(Locale.ROOT) + ". Write the brute force first if it helps you "
                                + "check correctness, then apply the observation above and re-measure.";

        return new SolutionComparison(user, alternatives, missing, tradeoffs, recommendation, reveal);
    }

    private List<String> userPros(Insight insight, ComplexityAnalysis complexity) {
        List<String> pros = new ArrayList<>();
        if (complexity.timeOptimal()) {
            pros.add("Already at the optimal time complexity");
        }
        if (complexity.spaceOptimal()) {
            pros.add("Memory footprint is as small as this problem allows");
        }
        if (insight.facts().guardsEmptyInput()) {
            pros.add("Handles the empty or degenerate input explicitly");
        }
        if (insight.facts().maxLoopDepth() <= 1 && !insight.facts().recursive()) {
            pros.add("Flat control flow — easy to read and to debug under pressure");
        }
        if (insight.facts().memoized()) {
            pros.add("Caches repeated states instead of recomputing them");
        }
        if (pros.isEmpty()) {
            pros.add("Direct and easy to reason about, which makes it a good correctness baseline");
        }
        return pros;
    }

    private List<String> userCons(Insight insight, ComplexityAnalysis complexity) {
        List<String> cons = new ArrayList<>();
        if (!complexity.timeOptimal()) {
            cons.add("Slower than achievable: " + complexity.time() + " versus " + complexity.optimalTime());
        }
        if (!complexity.spaceOptimal()) {
            cons.add("Uses more memory than necessary: " + complexity.space() + " versus " + complexity.optimalSpace());
        }
        if (!insight.facts().guardsEmptyInput()) {
            cons.add("No explicit guard for empty input");
        }
        if (insight.facts().selfCallSites() >= 2 && !insight.facts().memoized()) {
            cons.add("Recomputes identical subproblems along different branches");
        }
        if (cons.isEmpty()) {
            cons.add("Nothing structural — the remaining risk is in the edge cases, not the algorithm");
        }
        return cons;
    }

    // -------------------------------------------------------------- helpers

    private String firstSentences(String text, int count) {
        String flattened = withoutHeading(text).replaceAll("\\s+", " ").strip();
        String[] sentences = flattened.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(count, sentences.length); i++) {
            sb.append(sentences[i]).append(' ');
        }
        String result = sb.toString().strip();
        return result.length() > 400 ? result.substring(0, 400) + "…" : result;
    }

    /** Drops a leading heading line — a short line with no terminal punctuation — before summarising. */
    private String withoutHeading(String text) {
        String[] lines = text.split("\n", 2);
        if (lines.length < 2) {
            return text;
        }
        String head = lines[0].strip();
        boolean looksLikeHeading = !head.isEmpty() && head.length() <= 90 && !head.matches(".*[.!?:]$");
        return looksLikeHeading ? lines[1].stripLeading() : text;
    }

    private String lowerFirst(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    /** Exposed for the recommendation engine, which reuses the same catalogue. */
    public PatternCatalog catalog() {
        return catalog;
    }

    public List<LlmMessage> noHistory() {
        return List.of();
    }
}
