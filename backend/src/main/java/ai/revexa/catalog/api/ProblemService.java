package ai.revexa.catalog.api;

import ai.revexa.catalog.domain.Difficulty;
import ai.revexa.catalog.domain.PlatformSource;
import ai.revexa.catalog.domain.Problem;
import ai.revexa.catalog.domain.ProblemRepository;
import ai.revexa.catalog.dto.ProblemDtos;
import ai.revexa.catalog.provider.PracticePlatformProvider;
import ai.revexa.catalog.provider.PracticePlatformRegistry;
import ai.revexa.catalog.provider.ProblemTextParser;
import ai.revexa.core.error.ApiException;
import ai.revexa.core.json.Json;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemService {

    private final ProblemRepository repository;
    private final PracticePlatformRegistry registry;
    private final ProblemTextParser parser;
    private final Json json;

    public ProblemService(
            ProblemRepository repository,
            PracticePlatformRegistry registry,
            ProblemTextParser parser,
            Json json) {
        this.repository = repository;
        this.registry = registry;
        this.parser = parser;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Page<ProblemDtos.ProblemSummary> list(UUID userId, String search, String difficulty, Pageable pageable) {
        Difficulty parsed = parseDifficulty(difficulty);
        String term = search == null || search.isBlank() ? null : search.strip();
        return repository.findVisible(userId, term, parsed, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Problem require(UUID id, UUID userId) {
        Problem problem = repository.findById(id).orElseThrow(() -> ApiException.notFound("Problem"));
        if (!problem.isCurated() && problem.getOwnerId() != null && !problem.getOwnerId().equals(userId)) {
            throw ApiException.forbidden("This problem belongs to another account");
        }
        return problem;
    }

    @Transactional(readOnly = true)
    public String titleOf(UUID problemId) {
        return repository.findById(problemId).map(Problem::getTitle).orElse("Deleted problem");
    }

    @Transactional(readOnly = true)
    public ProblemDtos.ProblemDetail detail(UUID id, UUID userId) {
        return toDetail(require(id, userId));
    }

    @Transactional
    public ProblemDtos.ProblemDetail create(ProblemDtos.CreateProblemRequest request, UUID userId) {
        ProblemTextParser.Parsed parsed =
                parser.parse(request.statement(), request.title(), request.difficulty());

        Problem problem = new Problem();
        problem.setTitle(request.title().strip());
        problem.setSlug(uniqueSlug(parser.slugify(request.title()), userId));
        problem.setStatement(request.statement());
        problem.setConstraintsText(
                request.constraintsText() == null || request.constraintsText().isBlank()
                        ? parsed.constraints()
                        : request.constraintsText());
        problem.setExamples(parsed.examplesJson());
        problem.setDifficulty(parsed.difficulty());
        problem.setTopics(
                request.topics() == null || request.topics().isEmpty()
                        ? new ArrayList<>(parsed.topics())
                        : new ArrayList<>(request.topics()));
        problem.setSource(PlatformSource.MANUAL);
        problem.setUrl(request.url());
        problem.setOwnerId(userId);
        return toDetail(repository.save(problem));
    }

    @Transactional
    public ProblemDtos.ImportResult importProblem(ProblemDtos.ImportProblemRequest request, UUID userId) {
        PracticePlatformProvider.ImportRequest importRequest =
                new PracticePlatformProvider.ImportRequest(
                        request.rawText(),
                        request.url(),
                        request.title(),
                        request.difficulty(),
                        request.structuredPayload());

        if (importRequest.safeText().isBlank()
                && importRequest.safeUrl().isBlank()
                && (request.structuredPayload() == null || request.structuredPayload().isBlank())) {
            throw ApiException.badRequest("Provide the problem text, a problem URL, or an extension payload to import.");
        }

        PracticePlatformProvider provider = registry.resolve(request.providerId(), importRequest);
        PracticePlatformProvider.ImportedProblem imported = provider.importProblem(importRequest);

        Problem problem = new Problem();
        problem.setTitle(imported.title());
        problem.setSlug(uniqueSlug(imported.slug(), userId));
        problem.setStatement(imported.statement());
        problem.setConstraintsText(imported.constraintsText());
        problem.setExamples(imported.examples());
        problem.setDifficulty(imported.difficulty());
        problem.setTopics(new ArrayList<>(imported.topics()));
        problem.setSource(imported.source());
        problem.setExternalId(imported.externalId());
        problem.setUrl(imported.url());
        problem.setOwnerId(userId);

        Problem saved = repository.save(problem);
        return new ProblemDtos.ImportResult(
                toDetail(saved), provider.id(), provider.displayName(), imported.warnings());
    }

    public List<ProblemDtos.PlatformProviderInfo> providers() {
        return registry.all().stream()
                .map(
                        p -> {
                            PracticePlatformProvider.Capabilities capabilities = p.capabilities();
                            return new ProblemDtos.PlatformProviderInfo(
                                    p.id(),
                                    p.displayName(),
                                    p.source(),
                                    capabilities.automaticImport(),
                                    capabilities.requiresUserAuthorization(),
                                    capabilities.submissionSync(),
                                    capabilities.acceptedInputs(),
                                    capabilities.notes());
                        })
                .toList();
    }

    // ------------------------------------------------------------- mapping

    public ProblemDtos.ProblemSummary toSummary(Problem problem) {
        return new ProblemDtos.ProblemSummary(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                problem.getSource(),
                problem.getTopics(),
                problem.getUrl(),
                problem.isCurated(),
                problem.getCreatedAt());
    }

    public ProblemDtos.ProblemDetail toDetail(Problem problem) {
        return new ProblemDtos.ProblemDetail(
                problem.getId(),
                problem.getTitle(),
                problem.getSlug(),
                problem.getDifficulty(),
                problem.getSource(),
                problem.getStatement(),
                problem.getConstraintsText(),
                examples(problem.getExamples()),
                problem.getTopics(),
                problem.getUrl(),
                problem.isCurated(),
                problem.getCreatedAt());
    }

    private List<ProblemDtos.Example> examples(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, String>> parsed = json.read(raw, new TypeReference<List<Map<String, String>>>() {});
            return parsed.stream()
                    .map(m -> new ProblemDtos.Example(m.get("input"), m.get("output"), m.get("explanation")))
                    .toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Difficulty.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown difficulty: " + value);
        }
    }

    private String uniqueSlug(String base, UUID userId) {
        String slug = base;
        int suffix = 2;
        while (repository.findBySlugAndOwnerId(slug, userId).isPresent()
                || repository.findBySlugAndCuratedTrue(slug).isPresent()) {
            slug = base + "-" + suffix++;
            if (suffix > 50) {
                slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        return slug;
    }
}
