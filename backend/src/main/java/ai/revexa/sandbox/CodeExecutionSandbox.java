package ai.revexa.sandbox;

import java.util.List;

/**
 * The seam for running user code.
 *
 * <p>Executing untrusted code safely means process isolation, resource caps and a hardened image —
 * infrastructure, not application logic. So the prototype ships the interface, a clearly-labelled
 * simulated implementation and a disabled one; a Judge0, Piston or Firecracker runner drops in behind
 * the same contract without any caller changing.
 */
public interface CodeExecutionSandbox {

    String id();

    boolean enabled();

    List<String> supportedLanguages();

    ExecutionResult run(ExecutionRequest request);

    record ExecutionRequest(String language, String code, String stdin, List<TestCase> testCases) {}

    record TestCase(String name, String input, String expectedOutput) {}

    record ExecutionResult(
            String status,
            String stdout,
            String stderr,
            long durationMs,
            Integer exitCode,
            List<TestOutcome> testOutcomes,
            String note) {

        public static final String STATUS_OK = "OK";
        public static final String STATUS_DISABLED = "DISABLED";
        public static final String STATUS_SIMULATED = "SIMULATED";
        public static final String STATUS_ERROR = "ERROR";
    }

    record TestOutcome(String name, boolean passed, String expected, String actual, String note) {}
}
