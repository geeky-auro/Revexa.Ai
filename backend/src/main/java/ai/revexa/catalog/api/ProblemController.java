package ai.revexa.catalog.api;

import ai.revexa.catalog.dto.ProblemDtos;
import ai.revexa.core.security.CurrentUser;
import ai.revexa.core.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/problems")
@Tag(name = "Problems")
public class ProblemController {

    private final ProblemService problems;

    public ProblemController(ProblemService problems) {
        this.problems = problems;
    }

    @GetMapping
    @Operation(summary = "List the curated library plus your own imports")
    public PageResponse<ProblemDtos.ProblemSummary> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(
                problems.list(
                        CurrentUser.requireId(),
                        search,
                        difficulty,
                        PageRequest.of(Math.max(0, page), Math.min(100, size), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one problem")
    public ProblemDtos.ProblemDetail get(@PathVariable UUID id) {
        return problems.detail(id, CurrentUser.requireId());
    }

    @PostMapping
    @Operation(summary = "Create a problem by hand")
    public ResponseEntity<ProblemDtos.ProblemDetail> create(
            @Valid @RequestBody ProblemDtos.CreateProblemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(problems.create(request, CurrentUser.requireId()));
    }

    @PostMapping("/import")
    @Operation(summary = "Import through a PracticePlatformProvider (paste, URL or extension payload)")
    public ResponseEntity<ProblemDtos.ImportResult> importProblem(
            @Valid @RequestBody ProblemDtos.ImportProblemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(problems.importProblem(request, CurrentUser.requireId()));
    }

    @GetMapping("/platforms")
    @Operation(summary = "The registered practice platform providers and their real capabilities")
    public List<ProblemDtos.PlatformProviderInfo> platforms() {
        return problems.providers();
    }
}
