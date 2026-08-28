package ai.revexa.sandbox;

import java.util.List;

/** Used when execution is switched off entirely — it refuses clearly rather than pretending. */
public class DisabledSandbox implements CodeExecutionSandbox {

    @Override
    public String id() {
        return "disabled";
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public List<String> supportedLanguages() {
        return List.of();
    }

    @Override
    public ExecutionResult run(ExecutionRequest request) {
        return new ExecutionResult(
                ExecutionResult.STATUS_DISABLED,
                "",
                "",
                0,
                null,
                List.of(),
                "Code execution is disabled in this deployment.");
    }
}
