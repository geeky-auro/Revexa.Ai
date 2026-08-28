package ai.revexa.sandbox;

import ai.revexa.intelligence.heuristic.CodeFacts;
import ai.revexa.intelligence.heuristic.StaticCodeAnalyzer;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * A dry run rather than a real one.
 *
 * <p>It never executes anything. It parses the submission, reports what a run <em>would</em> involve
 * and flags the structural risks a real judge would surface first — and it says so explicitly in
 * every response, because a simulated pass that reads like a real pass would be worse than useless.
 */
@Component
public class SimulatedSandbox implements CodeExecutionSandbox {

    private static final List<String> LANGUAGES =
            List.of("python", "java", "cpp", "c", "javascript", "typescript", "go", "rust", "kotlin", "csharp");

    private final StaticCodeAnalyzer analyzer;

    public SimulatedSandbox(StaticCodeAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public String id() {
        return "simulated";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public List<String> supportedLanguages() {
        return LANGUAGES;
    }

    @Override
    public ExecutionResult run(ExecutionRequest request) {
        long started = System.nanoTime();
        CodeFacts facts = analyzer.analyze(request.code(), request.language());

        StringBuilder out = new StringBuilder();
        out.append("Static dry run — no code was executed.\n\n")
                .append("Language detected: ").append(facts.language()).append('\n')
                .append("Non-blank lines: ").append(facts.lines()).append('\n')
                .append("Maximum loop nesting: ").append(facts.maxLoopDepth()).append('\n')
                .append("Recursive: ").append(facts.recursive() ? "yes" : "no").append('\n')
                .append("Structures in play: ").append(String.join(", ", facts.structures())).append('\n');

        List<String> risks = new ArrayList<>();
        if (!facts.guardsEmptyInput()) {
            risks.add("No empty-input guard — an empty test case would likely throw.");
        }
        if (facts.recursive() && !facts.hasBaseCase()) {
            risks.add("Recursion with no clear base case — expect a stack overflow on a large input.");
        }
        if (facts.hasMidpointOverflowRisk()) {
            risks.add("Midpoint arithmetic can overflow in this language.");
        }
        if (facts.maxLoopDepth() >= 3) {
            risks.add("Triple-nested loops — a large input will exceed a typical time limit.");
        }
        if (!risks.isEmpty()) {
            out.append("\nWhat a real judge would probably hit first:\n");
            risks.forEach(risk -> out.append("  - ").append(risk).append('\n'));
        }

        List<TestOutcome> outcomes = new ArrayList<>();
        if (request.testCases() != null) {
            for (TestCase testCase : request.testCases()) {
                outcomes.add(
                        new TestOutcome(
                                testCase.name(),
                                false,
                                testCase.expectedOutput(),
                                null,
                                "Not executed — connect a real runner to get a verdict."));
            }
        }

        return new ExecutionResult(
                ExecutionResult.STATUS_SIMULATED,
                out.toString(),
                risks.isEmpty() ? "" : String.join("\n", risks),
                (System.nanoTime() - started) / 1_000_000,
                null,
                outcomes,
                "Execution is simulated in this deployment. Set revexa.sandbox.provider to a real runner "
                        + "(Judge0, Piston or an isolated container) to run code for real.");
    }
}
