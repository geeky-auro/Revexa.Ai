package ai.revexa.sandbox;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/execution")
@Tag(name = "Execution sandbox")
public class SandboxController {

    private final CodeExecutionSandbox sandbox;

    public SandboxController(CodeExecutionSandbox sandbox) {
        this.sandbox = sandbox;
    }

    public record RunRequest(
            @NotBlank @Size(max = 32) String language,
            @NotBlank @Size(max = 60_000) String code,
            @Size(max = 10_000) String stdin,
            List<CodeExecutionSandbox.TestCase> testCases) {}

    public record SandboxInfo(String provider, boolean enabled, List<String> supportedLanguages) {}

    @GetMapping
    @Operation(summary = "Which sandbox is active and what it can run")
    public SandboxInfo info() {
        return new SandboxInfo(sandbox.id(), sandbox.enabled(), sandbox.supportedLanguages());
    }

    @PostMapping("/run")
    @Operation(summary = "Run (or, in this deployment, dry-run) a submission")
    public CodeExecutionSandbox.ExecutionResult run(@Valid @RequestBody RunRequest request) {
        return sandbox.run(
                new CodeExecutionSandbox.ExecutionRequest(
                        request.language(), request.code(), request.stdin(), request.testCases()));
    }
}
