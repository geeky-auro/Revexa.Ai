package ai.revexa.practice.service;

import ai.revexa.catalog.api.ProblemService;
import ai.revexa.catalog.domain.Problem;
import ai.revexa.core.config.CacheConfig;
import ai.revexa.core.error.ApiException;
import ai.revexa.intelligence.dto.SolutionComparison;
import ai.revexa.intelligence.pipeline.ReviewPipeline;
import ai.revexa.intelligence.pipeline.StageContext;
import ai.revexa.practice.domain.Submission;
import ai.revexa.practice.dto.PracticeDtos;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the side-by-side approach comparison.
 *
 * <p>Results are cached by (problem, code, reveal flag): the same submission compared twice is the
 * same answer, and the AI call is the expensive part of the request.
 */
@Service
public class ComparisonService {

    private final ProblemService problems;
    private final SubmissionService submissions;
    private final ReviewPipeline pipeline;
    private final StageContextFactory contexts;

    public ComparisonService(
            ProblemService problems,
            SubmissionService submissions,
            ReviewPipeline pipeline,
            StageContextFactory contexts) {
        this.problems = problems;
        this.submissions = submissions;
        this.pipeline = pipeline;
        this.contexts = contexts;
    }

    @Transactional(readOnly = true)
    public PracticeDtos.ComparisonView compare(PracticeDtos.ComparisonRequest request, UUID userId) {
        Problem problem = problems.require(request.problemId(), userId);

        String code = request.code();
        String language = request.language();
        if ((code == null || code.isBlank()) && request.submissionId() != null) {
            Submission submission = submissions.require(request.submissionId(), userId);
            code = submission.getCode();
            language = submission.getLanguage();
        }
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Send the code to compare, or a submissionId to load it from.");
        }

        SolutionComparison comparison =
                cachedComparison(problem.getId(), code, language, request.revealPseudocode(), problem);
        return new PracticeDtos.ComparisonView(problem.getId(), problem.getTitle(), comparison);
    }

    @Cacheable(
            cacheNames = CacheConfig.COMPARISON_CACHE,
            key = "#problemId + ':' + #code.hashCode() + ':' + #reveal")
    public SolutionComparison cachedComparison(
            UUID problemId, String code, String language, boolean reveal, Problem problem) {
        StageContext context =
                contexts.from(problem).code(code).language(language).allowSpoilers(reveal).build();
        return pipeline.compare(context);
    }
}
